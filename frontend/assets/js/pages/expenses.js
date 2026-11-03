import { api } from '../api.js';
import { nav, setStatus, clearNode } from '../ui.js';
import { connectGroupStream } from '../sse.js';

nav('expenses');

const statusEl = document.getElementById('status');
const liveStatusEl = document.getElementById('live-status');
const groupsBody = document.getElementById('groups-body');
const expensesBody = document.getElementById('expenses-body');

let selectedGroupId = '';
let allExpenses = [];
let groupStream = null;
let refreshTimer = null;

function setLiveStatus(state, detail = '') {
  if (!liveStatusEl) return;

  const textByState = {
    connecting: 'Connecting...',
    connected: 'Connected',
    reconnecting: `Reconnecting (${detail || 'temporary issue'})`,
    closed: 'Disconnected'
  };

  liveStatusEl.textContent = textByState[state] || 'Not connected';
}

function disconnectGroupStream() {
  if (groupStream) {
    groupStream.close();
    groupStream = null;
  }
  setLiveStatus('closed');
}

function scheduleExpensesReload() {
  if (!selectedGroupId) return;
  if (refreshTimer) {
    clearTimeout(refreshTimer);
  }
  refreshTimer = setTimeout(async () => {
    refreshTimer = null;
    try {
      await loadGroupExpenses(selectedGroupId, { preserveStream: true });
      setStatus(statusEl, 'ok', 'Live update received. Expense list refreshed.');
    } catch (err) {
      setStatus(statusEl, 'warn', `Live update received, but refresh failed: ${err.message}`);
    }
  }, 300);
}

function connectStreamForGroup(groupId) {
  disconnectGroupStream();
  if (!groupId) return;

  groupStream = connectGroupStream(groupId, {
    onStatus: ({ state, detail }) => setLiveStatus(state, detail),
    onEvent: (message) => {
      const payload = message?.data;
      if (!payload || payload.groupId !== selectedGroupId) {
        return;
      }
      if (
        message.type === 'EXPENSE_CREATED' ||
        message.type === 'EXPENSE_UPDATED' ||
        message.type === 'EXPENSE_DELETED' ||
        message.type === 'SETTLEMENT_CREATED' ||
        message.type === 'SETTLEMENT_DELETED'
      ) {
        scheduleExpensesReload();
      }
    }
  });
}

function rememberContext(groupId, expenseId = null) {
  if (groupId) {
    localStorage.setItem('spendsmart.lastGroupId', groupId);
    selectedGroupId = groupId;
  }
  if (expenseId) {
    localStorage.setItem('spendsmart.lastExpenseId', expenseId);
  }
}

async function getMemberCount(groupId) {
  const details = await api.get(`/groups/${groupId}`);
  return Array.isArray(details?.members) ? details.members.length : 0;
}

async function loadGroupsForPicker() {
  const payload = await api.get('/groups');
  const groups = payload?.groups || [];
  clearNode(groupsBody);

  if (!groups.length) {
    groupsBody.innerHTML = '<tr><td colspan="4" class="empty">No groups found.</td></tr>';
    return [];
  }

  const counts = await Promise.all(groups.map((g) => getMemberCount(g.groupId).catch(() => 0)));
  const summaries = groups.map((g, i) => ({
    groupId: g.groupId,
    name: g.name || '-',
    memberCount: counts[i]
  }));

  for (const g of summaries) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${g.groupId}</td>
      <td>${g.name}</td>
      <td>${g.memberCount}</td>
      <td>
        <button class="btn secondary" data-view-group="${g.groupId}">View Expenses</button>
        <button class="btn ghost" data-create-group="${g.groupId}">Create Expense</button>
      </td>
    `;
    groupsBody.appendChild(tr);
  }

  for (const btn of groupsBody.querySelectorAll('button[data-view-group]')) {
    btn.addEventListener('click', async () => {
      const groupId = btn.dataset.viewGroup;
      try {
        await loadGroupExpenses(groupId);
        setStatus(statusEl, 'ok', 'Expenses loaded.');
      } catch (err) {
        setStatus(statusEl, 'err', err.message);
      }
    });
  }

  for (const btn of groupsBody.querySelectorAll('button[data-create-group]')) {
    btn.addEventListener('click', () => {
      const groupId = btn.dataset.createGroup;
      rememberContext(groupId);
      window.location.href = `./expenses-create.html?groupId=${encodeURIComponent(groupId)}`;
    });
  }

  return summaries;
}

function renderExpenses(expenses) {
  clearNode(expensesBody);
  if (!expenses.length) {
    expensesBody.innerHTML = '<tr><td colspan="5" class="empty">No expenses found for this filter.</td></tr>';
    return;
  }

  for (const e of expenses) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${e.id}</td>
      <td>${e.userId}</td>
      <td>${e.amount}</td>
      <td>${e.version}</td>
      <td>
        <button class="btn secondary" data-expense-id="${e.id}">Expense Detail</button>
        <button class="btn ghost" data-edit-expense="${e.id}">Edit</button>
      </td>
    `;
    expensesBody.appendChild(tr);
  }

  for (const btn of expensesBody.querySelectorAll('button[data-expense-id]')) {
    btn.addEventListener('click', () => {
      const expenseId = btn.dataset.expenseId;
      rememberContext(selectedGroupId, expenseId);
      window.location.href = `./expense-detail.html?expenseId=${encodeURIComponent(expenseId)}`;
    });
  }

  for (const btn of expensesBody.querySelectorAll('button[data-edit-expense]')) {
    btn.addEventListener('click', () => {
      const expenseId = btn.dataset.editExpense;
      rememberContext(selectedGroupId, expenseId);
      window.location.href = `./expenses-edit.html?expenseId=${encodeURIComponent(expenseId)}`;
    });
  }
}

function applyFilters() {
  const minRaw = document.getElementById('amount-min').value;
  const maxRaw = document.getElementById('amount-max').value;
  const sortBy = document.getElementById('sort-by').value;

  const min = minRaw === '' ? null : Number(minRaw);
  const max = maxRaw === '' ? null : Number(maxRaw);

  let filtered = allExpenses.filter((e) => {
    const amount = Number(e.amount);
    if (min !== null && amount < min) return false;
    if (max !== null && amount > max) return false;
    return true;
  });

  if (sortBy === 'amount-desc') {
    filtered.sort((a, b) => Number(b.amount) - Number(a.amount));
  } else if (sortBy === 'amount-asc') {
    filtered.sort((a, b) => Number(a.amount) - Number(b.amount));
  } else {
    filtered.sort((a, b) => Number(b.version) - Number(a.version));
  }

  renderExpenses(filtered);
}

async function loadGroupExpenses(groupId, options = {}) {
  const previousGroupId = selectedGroupId;
  const expenses = await api.get(`/expense/${groupId}`);
  rememberContext(groupId);
  allExpenses = expenses || [];
  applyFilters();

  if (!options.preserveStream && (previousGroupId !== groupId || !groupStream)) {
    connectStreamForGroup(groupId);
  }
}

document.getElementById('load-groups-btn').addEventListener('click', async () => {
  try {
    await loadGroupsForPicker();
    setStatus(statusEl, 'ok', 'Groups loaded.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('apply-filters-btn').addEventListener('click', () => {
  applyFilters();
});

loadGroupsForPicker()
  .then(async (groups) => {
    const lastGroupId = localStorage.getItem('spendsmart.lastGroupId') || '';
    if (lastGroupId && groups.some((g) => g.groupId === lastGroupId)) {
      await loadGroupExpenses(lastGroupId);
      setStatus(statusEl, 'ok', 'Loaded groups and last selected group expenses.');
      return;
    }
    setStatus(statusEl, 'ok', 'Groups loaded.');
  })
  .catch((err) => setStatus(statusEl, 'warn', `Could not auto-load groups: ${err.message}`));

window.addEventListener('beforeunload', () => {
  disconnectGroupStream();
});
