import { api } from '../api.js';
import { nav, setStatus, clearNode } from '../ui.js';
import { connectGroupStream } from '../sse.js';

nav('expenses');

const statusEl = document.getElementById('status');
const liveStatusEl = document.getElementById('live-status');
const splitList = document.getElementById('split-list');
const settlementsBody = document.getElementById('settlements-body');
let groupStream = null;
let currentExpenseId = '';
let currentGroupId = '';
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

function scheduleDetailReload() {
  if (!currentExpenseId) return;
  if (refreshTimer) {
    clearTimeout(refreshTimer);
  }
  refreshTimer = setTimeout(async () => {
    refreshTimer = null;
    try {
      await load(currentExpenseId, { preserveStream: true });
      setStatus(statusEl, 'ok', 'Live update received. Expense detail refreshed.');
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
      if (!payload || payload.groupId !== currentGroupId) {
        return;
      }

      const payloadExpenseId = payload.expenseId || '';
      if (!payloadExpenseId || payloadExpenseId !== currentExpenseId) {
        return;
      }

      if (
        message.type === 'EXPENSE_UPDATED' ||
        message.type === 'EXPENSE_DELETED' ||
        message.type === 'SETTLEMENT_CREATED' ||
        message.type === 'SETTLEMENT_DELETED'
      ) {
        scheduleDetailReload();
      }
    }
  });
}

function resolveExpenseId() {
  const query = new URLSearchParams(window.location.search);
  return query.get('expenseId') || localStorage.getItem('spendsmart.lastExpenseId') || '';
}

function renderSplit(split) {
  clearNode(splitList);
  if (!split?.length) {
    const li = document.createElement('li');
    li.textContent = 'No split entries found.';
    splitList.appendChild(li);
    return;
  }

  for (const row of split) {
    const li = document.createElement('li');
    li.textContent = `${row.userId}: ${row.amount}`;
    splitList.appendChild(li);
  }
}

function renderSettlements(rows, expenseId) {
  clearNode(settlementsBody);
  if (!rows?.length) {
    settlementsBody.innerHTML = '<tr><td colspan="7" class="empty">No settlements found.</td></tr>';
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
      <td><a class="btn danger" href="./settlements-delete.html?expenseId=${encodeURIComponent(expenseId)}&linkId=${encodeURIComponent(s.linkId)}">Delete</a></td>
    `;
    settlementsBody.appendChild(tr);
  }
}

async function load(expenseId, options = {}) {
  const expense = await api.get(`/expense/item/${expenseId}`);
  const settlements = await api.get(`/expense/${expenseId}/settlements`);
  const previousGroupId = currentGroupId;
  currentExpenseId = expenseId;
  currentGroupId = expense.groupId || '';

  document.getElementById('expense-id').textContent = expense.id || '-';
  document.getElementById('group-id').textContent = expense.groupId || '-';
  document.getElementById('issuer-id').textContent = expense.userId || '-';
  document.getElementById('amount').textContent = expense.amount ?? '-';
  document.getElementById('version').textContent = expense.version ?? '-';

  renderSplit(expense.split || []);
  renderSettlements(settlements || [], expenseId);

  document.getElementById('edit-expense-link').href = `./expenses-edit.html?expenseId=${encodeURIComponent(expenseId)}`;
  document.getElementById('create-settlement-link').href = `./settlements-create.html?expenseId=${encodeURIComponent(expenseId)}`;

  localStorage.setItem('spendsmart.lastGroupId', expense.groupId || '');
  localStorage.setItem('spendsmart.lastExpenseId', expenseId);

  if (!options.preserveStream && (previousGroupId !== currentGroupId || !groupStream)) {
    connectStreamForGroup(currentGroupId);
  }
}

const expenseId = resolveExpenseId();
if (!expenseId) {
  setStatus(statusEl, 'warn', 'No expense selected. Go back and pick one from the expenses list.');
} else {
  load(expenseId)
    .then(() => setStatus(statusEl, 'ok', 'Expense detail loaded.'))
    .catch((err) => setStatus(statusEl, 'err', err.message));
}

window.addEventListener('beforeunload', () => {
  disconnectGroupStream();
});
