import { api } from '../api.js';
import { nav, setStatus } from '../ui.js';

nav('settlements');

const statusEl = document.getElementById('status');

function prefillFields() {
  const query = new URLSearchParams(window.location.search);
  const expenseId = query.get('expenseId') || localStorage.getItem('spendsmart.lastExpenseId') || '';
  const linkId = query.get('linkId') || localStorage.getItem('spendsmart.lastSettlementId') || '';

  if (expenseId) {
    document.getElementById('delete-expense-id').value = expenseId;
  }
  if (linkId) {
    document.getElementById('delete-link-id').value = linkId;
  }
}

document.getElementById('delete-btn').addEventListener('click', async () => {
  try {
    const expenseId = document.getElementById('delete-expense-id').value.trim();
    const linkId = document.getElementById('delete-link-id').value.trim();
    const version = Number(document.getElementById('delete-version').value);

    if (!expenseId) throw new Error('Expense ID is required.');
    if (!linkId) throw new Error('Settlement link ID is required.');
    if (!Number.isFinite(version) || version < 0) throw new Error('Settlement version is required.');

    await api.del(`/expense/${expenseId}/settlements/${linkId}`, { version });
    localStorage.setItem('spendsmart.lastExpenseId', expenseId);
    localStorage.setItem('spendsmart.lastSettlementId', '');
    setStatus(statusEl, 'ok', 'Settlement deleted.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

prefillFields();
