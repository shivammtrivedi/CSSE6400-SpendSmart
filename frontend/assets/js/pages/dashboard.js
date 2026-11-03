import { nav, setStatus } from '../ui.js';
import { api } from '../api.js';

nav('dashboard');

const statusEl = document.getElementById('status');
const periodSelect = document.getElementById('period');
const totalSpendEl = document.getElementById('total-spend');
const budgetUsedEl = document.getElementById('budget-used');
const topCategoryEl = document.getElementById('top-category');

let chartInstance = null;

function getDateRange(period) {
  const now = new Date();
  let from, to;

  switch (period) {
    case 'This month':
      from = new Date(now.getFullYear(), now.getMonth(), 1);
      to = now;
      break;
    case 'Last month':
      from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      to = new Date(now.getFullYear(), now.getMonth(), 0);
      break;
    case 'Last 3 months':
      from = new Date(now.getFullYear(), now.getMonth() - 3, 1);
      to = now;
      break;
    default:
      from = new Date(now.getFullYear(), now.getMonth(), 1);
      to = now;
  }

  return { from: from.toISOString().split('T')[0], to: to.toISOString().split('T')[0] };
}

function aggregateByDay(transactions) {
  const map = {};
  for (const t of transactions) {
    if (!t.occurredAt) continue;
    const day = t.occurredAt.split('T')[0];
    map[day] = (map[day] || 0) + (t.amount || 0);
  }

  const keys = Object.keys(map).sort();
  return {
    labels: keys.map(k => {
      const d = new Date(k + 'T00:00:00');
      return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
    }),
    values: keys.map(k => parseFloat(map[k].toFixed(2)))
  };
}

function buildChart(labels, values) {
  const ctx = document.getElementById('spend-chart').getContext('2d');

  if (chartInstance) chartInstance.destroy();

  chartInstance = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Daily Spend',
        data: values,
        backgroundColor: values.map(v => v >= 0 ? 'rgba(75, 192, 192, 0.6)' : 'rgba(255, 99, 132, 0.6)'),
        borderColor: values.map(v => v >= 0 ? 'rgba(75, 192, 192, 1)' : 'rgba(255, 99, 132, 1)'),
        borderWidth: 1
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: { display: false }
      },
      scales: {
        y: {
          beginAtZero: true,
          title: { display: true, text: 'Amount' }
        },
        x: {
          title: { display: true, text: 'Date' }
        }
      }
    }
  });
}

function updateSummaryCards(transactions) {
  let totalSpend = 0;
  let totalBudget = 0;
  const categoryMap = {};

  for (const t of transactions) {
    const amt = t.amount || 0;
    totalSpend += amt;
    // Budget tracking placeholder — no budget API yet, use credits as proxy
    if (amt > 0) totalBudget += amt;

    const cat = t.description || t.externalTxnId || 'Unknown';
    categoryMap[cat] = (categoryMap[cat] || 0) + Math.abs(amt);
  }

  totalSpendEl.textContent = `$${totalSpend.toFixed(2)}`;

  // Budget used: placeholder (no budget API yet)
  budgetUsedEl.textContent = totalBudget > 0 ? `$${totalBudget.toFixed(2)}` : '--';

  // Top category
  let topCat = '-';
  let topVal = 0;
  for (const [cat, val] of Object.entries(categoryMap)) {
    if (val > topVal) { topVal = val; topCat = cat; }
  }
  topCategoryEl.textContent = topCat;
}

async function refreshDashboard() {
  const period = periodSelect.value;
  const { from, to } = getDateRange(period);

  try {
    setStatus(statusEl, 'info', 'Loading...');

    const transactions = await api.get(`/query/transactions?fromDate=${from}&toDate=${to}`);

    if (!transactions || !transactions.length) {
      if (chartInstance) chartInstance.destroy();
      chartInstance = null;
      setStatus(statusEl, 'warn', 'No transactions found for this period.');
      totalSpendEl.textContent = '--';
      budgetUsedEl.textContent = '--';
      topCategoryEl.textContent = '--';
      return;
    }

    const { labels, values } = aggregateByDay(transactions);
    buildChart(labels, values);
    updateSummaryCards(transactions);

    setStatus(statusEl, 'ok', `Loaded ${transactions.length} transaction(s).`);
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
}

periodSelect.addEventListener('change', refreshDashboard);
refreshDashboard();
