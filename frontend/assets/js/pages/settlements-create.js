import { api } from '../api.js';
import { nav, setStatus } from '../ui.js';

nav('settlements');

const statusEl = document.getElementById('status');
const providerEl = document.getElementById('provider');
const transactionSelectEl = document.getElementById('transaction-select');
const amountEl = document.getElementById('amount');

let transactions = [];

function prefillExpense() {
  const query = new URLSearchParams(window.location.search);
  const expenseId = query.get('expenseId') || localStorage.getItem('spendsmart.lastExpenseId') || '';
  if (expenseId) {
    document.getElementById('create-expense-id').value = expenseId;
  }
}

function formatTransactionOption(t) {
  const datePart = t?.occurredAt ? new Date(t.occurredAt).toLocaleDateString() : 'Unknown date';
  const amountPart = t?.amount != null ? `${Math.abs(Number(t.amount)).toFixed(2)} ${t.currency || ''}`.trim() : 'Unknown amount';
  const idPart = t?.externalTxnId || 'No external ID';
  return `${datePart} | ${amountPart} | ${idPart}`;
}

function renderTransactionOptions(rows) {
  transactionSelectEl.innerHTML = '<option value="">Select a transaction</option>';

  for (const t of rows) {
    if (!t.externalTxnId) continue;
    const option = document.createElement('option');
    option.value = t.externalTxnId;
    option.textContent = formatTransactionOption(t);
    transactionSelectEl.appendChild(option);
  }
}

async function loadTransactionsForProvider(provider) {
  try {
    setStatus(statusEl, 'info', 'Loading transactions...');
    const rows = await api.get(`/query/transactions?provider=${encodeURIComponent(provider)}`);
    transactions = Array.isArray(rows) ? rows : [];
    renderTransactionOptions(transactions);
    setStatus(statusEl, 'ok', `Loaded ${transactionSelectEl.options.length - 1} transaction option(s).`);
  } catch (err) {
    transactions = [];
    renderTransactionOptions([]);
    setStatus(statusEl, 'err', `Could not load transactions: ${err.message}`);
  }
}

transactionSelectEl.addEventListener('change', () => {
  const selectedId = transactionSelectEl.value;
  if (!selectedId) return;
  const selected = transactions.find((t) => t.externalTxnId === selectedId);
  if (selected?.amount != null && !amountEl.value) {
    amountEl.value = Math.abs(Number(selected.amount)).toFixed(2);
  }
});

providerEl.addEventListener('change', () => {
  loadTransactionsForProvider(providerEl.value);
});

document.getElementById('create-btn').addEventListener('click', async () => {
  try {
    const expenseId = document.getElementById('create-expense-id').value.trim();
    const provider = providerEl.value.trim();
    const transactionId = transactionSelectEl.value.trim();
    const amount = Number(document.getElementById('amount').value);
    const version = Number(document.getElementById('create-version').value);

    if (!expenseId) throw new Error('Expense ID is required.');
    if (!provider) throw new Error('Provider is required.');
    if (!transactionId) throw new Error('Transaction reference is required.');
    if (!Number.isFinite(amount) || amount <= 0) throw new Error('Amount must be greater than zero.');
    if (!Number.isFinite(version) || version < 0) throw new Error('Expense version is required.');

    const selected = transactions.find((t) => t.externalTxnId === transactionId);
    if (!selected) throw new Error('Selected transaction is no longer loaded. Reload the list and pick again.');

    // Step 1 — persist the chosen transaction into the canonical transactions table.
    // expense-service returns 404 when settling unless this row already exists for
    // (externalTxnId, owner, provider). Idempotent upsert, so re-submitting is safe.
    // Send the transaction's own amount/currency (not the user's settlement amount),
    // and the same provider value used below so the lookup triple matches exactly.
    try {
      setStatus(statusEl, 'info', 'Saving transaction...');
      await api.post('/query/transactions/persist', {
        externalTxnId: selected.externalTxnId,
        provider,
        amount: selected.amount,
        currency: selected.currency,
        occurredAt: selected.occurredAt
      });
    } catch (err) {
      throw new Error(`Could not save transaction before settling: ${err.message}`);
    }

    // Step 2 — create the settlement against the now-persisted transaction.
    setStatus(statusEl, 'info', 'Creating settlement...');
    await api.post(`/expense/${expenseId}/settlements`, {
      provider,
      transactionId,
      amount,
      version
    });

    localStorage.setItem('spendsmart.lastExpenseId', expenseId);
    setStatus(statusEl, 'ok', 'Settlement created.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

prefillExpense();
loadTransactionsForProvider(providerEl.value);
