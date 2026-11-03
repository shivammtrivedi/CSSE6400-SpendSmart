import { api } from '../api.js';
import { nav, setStatus, clearNode } from '../ui.js';

nav('budgets');

const statusEl = document.getElementById('status');
const budgetsBody = document.getElementById('budgets-body');

let editingId = null;

function enterEditMode(budget) {
  editingId = budget.id;
  document.getElementById('edit-hint').hidden = true;
  document.getElementById('edit-category').value = budget.category;
  document.getElementById('edit-category').disabled = false;
  document.getElementById('edit-limit').value = budget.limitAmount;
  document.getElementById('edit-limit').disabled = false;
  document.getElementById('edit-period').value = budget.period;
  document.getElementById('edit-period').disabled = false;
  document.getElementById('save-budget-btn').disabled = false;
  document.getElementById('cancel-edit-btn').disabled = false;
}

function exitEditMode() {
  editingId = null;
  document.getElementById('edit-hint').hidden = false;
  document.getElementById('edit-category').value = '';
  document.getElementById('edit-category').disabled = true;
  document.getElementById('edit-limit').value = '';
  document.getElementById('edit-limit').disabled = true;
  document.getElementById('edit-period').value = 'MONTHLY';
  document.getElementById('edit-period').disabled = true;
  document.getElementById('save-budget-btn').disabled = true;
  document.getElementById('cancel-edit-btn').disabled = true;
}

async function loadBudgets() {
  const budgets = await api.get('/budgets');
  clearNode(budgetsBody);

  if (!budgets || !budgets.length) {
    budgetsBody.innerHTML = '<tr><td colspan="4" class="empty">No budgets yet.</td></tr>';
    return;
  }

  for (const b of budgets) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${b.category}</td>
      <td>${b.period}</td>
      <td>${Number(b.limitAmount).toFixed(2)}</td>
      <td>
        <button class="btn secondary" data-edit-id="${b.id}">Edit</button>
        <button class="btn ghost" data-delete-id="${b.id}">Delete</button>
      </td>
    `;
    tr.querySelector('[data-edit-id]')._budget = b;
    budgetsBody.appendChild(tr);
  }

  for (const btn of budgetsBody.querySelectorAll('button[data-edit-id]')) {
    btn.addEventListener('click', () => enterEditMode(btn._budget));
  }

  for (const btn of budgetsBody.querySelectorAll('button[data-delete-id]')) {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.deleteId;
      try {
        await api.del(`/budgets/${id}`);
        if (editingId === id) exitEditMode();
        await loadBudgets();
        setStatus(statusEl, 'ok', 'Budget deleted.');
      } catch (err) {
        setStatus(statusEl, 'err', err.message);
      }
    });
  }
}

document.getElementById('create-budget-btn').addEventListener('click', async () => {
  const category = document.getElementById('budget-category').value.trim();
  const limitAmount = parseFloat(document.getElementById('budget-limit').value);
  const period = document.getElementById('budget-period').value;

  if (!category) { setStatus(statusEl, 'err', 'Category is required.'); return; }
  if (!limitAmount || limitAmount <= 0) { setStatus(statusEl, 'err', 'Limit must be a positive number.'); return; }

  try {
    await api.post('/budgets', { category, limitAmount, period });
    document.getElementById('budget-category').value = '';
    document.getElementById('budget-limit').value = '';
    await loadBudgets();
    setStatus(statusEl, 'ok', 'Budget created.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('save-budget-btn').addEventListener('click', async () => {
  if (!editingId) return;
  const category = document.getElementById('edit-category').value.trim();
  const limitAmount = parseFloat(document.getElementById('edit-limit').value);
  const period = document.getElementById('edit-period').value;

  if (!category) { setStatus(statusEl, 'err', 'Category is required.'); return; }
  if (!limitAmount || limitAmount <= 0) { setStatus(statusEl, 'err', 'Limit must be a positive number.'); return; }

  try {
    await api.put(`/budgets/${editingId}`, { category, limitAmount, period });
    exitEditMode();
    await loadBudgets();
    setStatus(statusEl, 'ok', 'Budget updated.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('cancel-edit-btn').addEventListener('click', () => exitEditMode());

loadBudgets()
  .then(() => setStatus(statusEl, 'ok', 'Budgets loaded.'))
  .catch((err) => setStatus(statusEl, 'warn', `Could not load budgets: ${err.message}`));
