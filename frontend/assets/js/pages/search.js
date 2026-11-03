import { nav, setStatus, clearNode } from '../ui.js';
import { api } from '../api.js';

nav('search');

const statusEl = document.getElementById('status');
const resultsBody = document.getElementById('results-body');
const searchBtn = document.getElementById('search-btn');

function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  return d.toLocaleDateString();
}

function formatAmount(amount, currency) {
  const abs = Math.abs(amount ?? 0).toFixed(2);
  const cur = currency || '';
  return `${abs} ${cur}`.trim();
}

function determineDirection(amount) {
  if (amount == null) return '-';
  return amount < 0 ? 'DEBIT' : 'CREDIT';
}

function renderTransactions(transactions) {
  clearNode(resultsBody);

  if (!transactions || !transactions.length) {
    const tr = document.createElement('tr');
    tr.innerHTML = '<td colspan="5" class="empty">No results found.</td>';
    resultsBody.appendChild(tr);
    return;
  }

  for (const t of transactions) {
    const tr = document.createElement('tr');
    const txnJson = JSON.stringify(t).replace(/'/g, "&#39;");
    tr.innerHTML = `
      <td>${formatDate(t.occurredAt)}</td>
      <td>-</td>
      <td>${formatAmount(t.amount, t.currency)}</td>
      <td>${determineDirection(t.amount)}</td>
      <td><button class="btn secondary persist-btn" data-txn='${txnJson}'>Persist</button></td>
    `;
    resultsBody.appendChild(tr);
  }

  for (const btn of resultsBody.querySelectorAll('.persist-btn')) {
    btn.addEventListener('click', async () => {
      try {
        const txn = JSON.parse(btn.dataset.txn);
        await api.post('/query/transactions/persist', txn);
        btn.disabled = true;
        btn.textContent = 'Saved';
        setStatus(statusEl, 'ok', 'Transaction saved.');
      } catch (err) {
        setStatus(statusEl, 'err', `Failed to persist: ${err.message}`);
      }
    });
  }
}

async function performSearch() {
  const params = new URLSearchParams();

  const fromDate = document.getElementById('date-from').value;
  const toDate = document.getElementById('date-to').value;
  const currency = document.getElementById('currency').value.trim();
  const minAmount = document.getElementById('amount-min').value;
  const maxAmount = document.getElementById('amount-max').value;
  const provider = document.getElementById('provider').value;

  if (fromDate) params.set('fromDate', fromDate);
  if (toDate) params.set('toDate', toDate);
  if (currency) params.set('currency', currency);
  if (minAmount) params.set('minAmount', minAmount);
  if (maxAmount) params.set('maxAmount', maxAmount);
  if (provider) params.set('provider', provider);

  const qs = params.toString();
  const path = qs ? `/query/transactions?${qs}` : '/query/transactions';

  try {
    setStatus(statusEl, 'info', 'Searching...');
    const results = await api.get(path);
    renderTransactions(results);
    setStatus(statusEl, 'ok', `Found ${results?.length || 0} transaction(s).`);
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
    renderTransactions([]);
  }
}

searchBtn.addEventListener('click', performSearch);
