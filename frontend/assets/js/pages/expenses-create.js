import { api } from '../api.js';
import { nav, setStatus, parseJsonInput, clearNode } from '../ui.js';

nav('expenses');

const statusEl = document.getElementById('status');
const createRowsEl = document.getElementById('create-split-rows');
const createSplitJsonEl = document.getElementById('create-split-json');
const groupInput = document.getElementById('create-group-id');

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
  const split = readSplitRows(createRowsEl);
  createSplitJsonEl.value = split.length ? JSON.stringify(split, null, 2) : '';
}

function applySplitToRows(split) {
  clearNode(createRowsEl);
  if (!Array.isArray(split) || !split.length) {
    createSplitRow(createRowsEl, {}, syncRowsToJson);
    return;
  }
  for (const item of split) {
    createSplitRow(createRowsEl, item, syncRowsToJson);
  }
}

function getSplitFromForm() {
  const raw = createSplitJsonEl.value.trim();
  if (raw) {
    return parseJsonInput('create-split-json', []);
  }
  return readSplitRows(createRowsEl);
}

function prefillGroup() {
  const query = new URLSearchParams(window.location.search);
  const groupId = query.get('groupId') || localStorage.getItem('spendsmart.lastGroupId') || '';
  if (groupId) {
    groupInput.value = groupId;
  }
}

document.getElementById('create-add-split-btn').addEventListener('click', () => {
  createSplitRow(createRowsEl, {}, syncRowsToJson);
  syncRowsToJson();
});

createSplitJsonEl.addEventListener('blur', () => {
  if (!createSplitJsonEl.value.trim()) return;
  try {
    const split = parseJsonInput('create-split-json', []);
    applySplitToRows(split);
    syncRowsToJson();
  } catch {
    // Keep invalid text as-is for correction.
  }
});

document.getElementById('create-expense-btn').addEventListener('click', async () => {
  try {
    const groupId = groupInput.value.trim();
    const amount = Number(document.getElementById('create-amount').value);
    const split = getSplitFromForm();

    if (!groupId) throw new Error('Group ID is required.');
    if (!Number.isFinite(amount) || amount <= 0) throw new Error('Amount must be greater than zero.');

    await api.post('/expense', { groupId, amount, split });
    localStorage.setItem('spendsmart.lastGroupId', groupId);
    setStatus(statusEl, 'ok', 'Expense created. You can create another one or go back to workspace.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

prefillGroup();
createSplitRow(createRowsEl, {}, syncRowsToJson);
syncRowsToJson();
