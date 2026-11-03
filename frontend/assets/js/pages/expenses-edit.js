import { api } from '../api.js';
import { nav, setStatus, parseJsonInput, clearNode } from '../ui.js';

nav('expenses');

const statusEl = document.getElementById('status');
const updateRowsEl = document.getElementById('update-split-rows');
const updateSplitJsonEl = document.getElementById('update-split-json');

function createSplitRow(container, values = {}, onChange) {
  const row = document.createElement('div');
  row.className = 'form-row';
  row.innerHTML = `
    <div>
      <label>User ID</label>
      <input type="text" data-split-user placeholder="User UUID" value="${values.userId || ''}" />
    </div>
    <div>
      <label>Amount</label>
      <input type="number" step="0.01" data-split-amount placeholder="0.00" value="${values.amount ?? ''}" />
    </div>
    <div style="grid-column:1 / -1;">
      <button class="btn ghost" type="button" data-remove-split>Remove</button>
    </div>
  `;
  container.appendChild(row);

  row.querySelector('[data-remove-split]').addEventListener('click', () => {
    row.remove();
    if (!container.children.length) {
      createSplitRow(container, {}, onChange);
    }
    onChange();
  });

  for (const input of row.querySelectorAll('input')) {
    input.addEventListener('input', onChange);
  }
}

function readSplitRows(container) {
  const split = [];
  for (const row of container.children) {
    const userId = row.querySelector('[data-split-user]')?.value?.trim();
    const amountRaw = row.querySelector('[data-split-amount]')?.value;
    if (!userId && !amountRaw) continue;
    split.push({ userId, amount: Number(amountRaw) });
  }
  return split;
}

function syncRowsToJson() {
  const split = readSplitRows(updateRowsEl);
  updateSplitJsonEl.value = split.length ? JSON.stringify(split, null, 2) : '';
}

function applySplitToRows(split) {
  clearNode(updateRowsEl);
  if (!Array.isArray(split) || !split.length) {
    createSplitRow(updateRowsEl, {}, syncRowsToJson);
    return;
  }
  for (const item of split) {
    createSplitRow(updateRowsEl, item, syncRowsToJson);
  }
}

function getSplitFromForm() {
  const raw = updateSplitJsonEl.value.trim();
  if (raw) {
    return parseJsonInput('update-split-json', []);
  }
  return readSplitRows(updateRowsEl);
}

function applyExpenseToForms(e) {
  document.getElementById('load-expense-id').value = e.id || '';
  document.getElementById('update-expense-id').value = e.id || '';
  document.getElementById('delete-expense-id').value = e.id || '';
  document.getElementById('update-group-id').value = e.groupId || '';
  document.getElementById('update-amount').value = e.amount ?? '';
  document.getElementById('update-version').value = e.version ?? '';
  document.getElementById('delete-version').value = e.version ?? '';
  applySplitToRows(e.split || []);
  syncRowsToJson();
  localStorage.setItem('spendsmart.lastExpenseId', e.id);
  localStorage.setItem('spendsmart.lastGroupId', e.groupId);
}

async function loadExpense(expenseId) {
  const e = await api.get(`/expense/item/${expenseId}`);
  applyExpenseToForms(e);
}

function getInitialExpenseId() {
  const query = new URLSearchParams(window.location.search);
  return query.get('expenseId') || localStorage.getItem('spendsmart.lastExpenseId') || '';
}

document.getElementById('load-expense-btn').addEventListener('click', async () => {
  try {
    const expenseId = document.getElementById('load-expense-id').value.trim();
    if (!expenseId) throw new Error('Expense ID is required.');
    await loadExpense(expenseId);
    setStatus(statusEl, 'ok', 'Expense loaded.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('update-add-split-btn').addEventListener('click', () => {
  createSplitRow(updateRowsEl, {}, syncRowsToJson);
  syncRowsToJson();
});

updateSplitJsonEl.addEventListener('blur', () => {
  if (!updateSplitJsonEl.value.trim()) return;
  try {
    const split = parseJsonInput('update-split-json', []);
    applySplitToRows(split);
    syncRowsToJson();
  } catch {
    // Keep invalid text as-is for correction.
  }
});

document.getElementById('update-expense-btn').addEventListener('click', async () => {
  try {
    const expenseId = document.getElementById('update-expense-id').value.trim();
    const groupId = document.getElementById('update-group-id').value.trim();
    const amount = Number(document.getElementById('update-amount').value);
    const version = Number(document.getElementById('update-version').value);
    const split = getSplitFromForm();

    if (!expenseId) throw new Error('Expense ID is required.');
    if (!groupId) throw new Error('Group ID is required.');
    if (!Number.isFinite(amount) || amount <= 0) throw new Error('Amount must be greater than zero.');
    if (!Number.isFinite(version) || version < 0) throw new Error('Version is required.');

    await api.put(`/expense/${expenseId}`, { groupId, amount, version, split });
    await loadExpense(expenseId);
    setStatus(statusEl, 'ok', 'Expense updated. Fresh version loaded.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('delete-expense-btn').addEventListener('click', async () => {
  try {
    const expenseId = document.getElementById('delete-expense-id').value.trim();
    const version = Number(document.getElementById('delete-version').value);
    if (!expenseId) throw new Error('Expense ID is required.');
    if (!Number.isFinite(version) || version < 0) throw new Error('Version is required.');

    await api.del(`/expense/${expenseId}`, { version });
    localStorage.setItem('spendsmart.lastExpenseId', '');
    setStatus(statusEl, 'ok', 'Expense deleted. Return to workspace to pick another expense.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

const initialExpenseId = getInitialExpenseId();
createSplitRow(updateRowsEl, {}, syncRowsToJson);
syncRowsToJson();
if (initialExpenseId) {
  document.getElementById('load-expense-id').value = initialExpenseId;
  loadExpense(initialExpenseId)
    .then(() => setStatus(statusEl, 'ok', 'Loaded last selected expense.'))
    .catch((err) => setStatus(statusEl, 'warn', `Could not auto-load last expense: ${err.message}`));
}
