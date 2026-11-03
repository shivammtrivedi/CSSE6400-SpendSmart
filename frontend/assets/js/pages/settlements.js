import { api } from '../api.js';
import { nav, setStatus, clearNode } from '../ui.js';

nav('settlements');

const statusEl = document.getElementById('status');
const groupsBody = document.getElementById('groups-body');
const expensesBody = document.getElementById('expenses-body');
const bodyEl = document.getElementById('settlements-body');

function rememberContext(expenseId, settlementId = null) {
  if (expenseId) {
    localStorage.setItem('spendsmart.lastExpenseId', expenseId);
  }
  if (settlementId) {
    localStorage.setItem('spendsmart.lastSettlementId', settlementId);
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
      <td><button class="btn secondary" data-load-expenses="${g.groupId}">Show Expenses</button></td>
    `;
    groupsBody.appendChild(tr);
  }

  for (const btn of groupsBody.querySelectorAll('button[data-load-expenses]')) {
    btn.addEventListener('click', async () => {
      const groupId = btn.dataset.loadExpenses;
      try {
        await loadExpensesForGroup(groupId);
        setStatus(statusEl, 'ok', 'Expenses loaded for selected group.');
      } catch (err) {
        setStatus(statusEl, 'err', err.message);
      }
    });
  }

  return summaries;
}

function renderExpenses(expenses) {
  clearNode(expensesBody);
  if (!expenses.length) {
    expensesBody.innerHTML = '<tr><td colspan="5" class="empty">No expenses found for this group.</td></tr>';
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
        <button class="btn secondary" data-expense-id="${e.id}">Show Settlements</button>
        <button class="btn ghost" data-create-settlement="${e.id}">Create Settlement</button>
      </td>
    `;
    expensesBody.appendChild(tr);
  }

  for (const btn of expensesBody.querySelectorAll('button[data-expense-id]')) {
    btn.addEventListener('click', async () => {
      const expenseId = btn.dataset.expenseId;
      try {
        await loadSettlements(expenseId);
        setStatus(statusEl, 'ok', 'Settlements loaded.');
      } catch (err) {
        setStatus(statusEl, 'err', err.message);
      }
    });
  }

  for (const btn of expensesBody.querySelectorAll('button[data-create-settlement]')) {
    btn.addEventListener('click', () => {
      const expenseId = btn.dataset.createSettlement;
      rememberContext(expenseId);
      window.location.href = `./settlements-create.html?expenseId=${encodeURIComponent(expenseId)}`;
    });
  }
}

async function loadExpensesForGroup(groupId) {
  localStorage.setItem('spendsmart.lastGroupId', groupId);
  const expenses = await api.get(`/expense/${groupId}`);
  renderExpenses(expenses || []);
}

function render(rows, expenseId) {
  clearNode(bodyEl);
  if (!rows.length) {
    bodyEl.innerHTML = '<tr><td colspan="7" class="empty">No settlements found for this expense.</td></tr>';
    return;
  }

  for (const s of rows) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${s.linkId}</td>
      <td>${s.userId}</td>
      <td>${s.provider}</td>
      <td>${s.transactionId}</td>
      <td>${s.amount}</td>
      <td>${s.version}</td>
      <td><button class="btn secondary" data-link-id="${s.linkId}" data-expense-id="${expenseId}">Select</button></td>
    `;
    bodyEl.appendChild(tr);
  }

  for (const btn of bodyEl.querySelectorAll('button[data-link-id]')) {
    btn.addEventListener('click', () => {
      rememberContext(btn.dataset.expenseId, btn.dataset.linkId);
      setStatus(statusEl, 'ok', 'Settlement selected. Opening delete page...');
      window.location.href = `./settlements-delete.html?expenseId=${encodeURIComponent(btn.dataset.expenseId)}&linkId=${encodeURIComponent(btn.dataset.linkId)}`;
    });
  }
}

async function loadSettlements(expenseId) {
  const rows = await api.get(`/expense/${expenseId}/settlements`);
  rememberContext(expenseId);
  render(rows || [], expenseId);
}

document.getElementById('load-groups-btn').addEventListener('click', async () => {
  try {
    await loadGroupsForPicker();
    setStatus(statusEl, 'ok', 'Groups loaded.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

loadGroupsForPicker()
  .then(async () => {
    const lastGroupId = localStorage.getItem('spendsmart.lastGroupId') || '';
    const lastExpenseId = localStorage.getItem('spendsmart.lastExpenseId') || '';
    if (lastGroupId) {
      await loadExpensesForGroup(lastGroupId);
    }
    if (lastExpenseId) {
      await loadSettlements(lastExpenseId);
      setStatus(statusEl, 'ok', 'Loaded groups, expenses, and last selected settlements.');
      return;
    }
    setStatus(statusEl, 'ok', 'Groups loaded.');
  })
  .catch((err) => setStatus(statusEl, 'warn', `Could not auto-load groups: ${err.message}`));
