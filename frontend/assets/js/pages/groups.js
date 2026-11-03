import { api } from '../api.js';
import { nav, setStatus, clearNode } from '../ui.js';

nav('groups');

const statusEl = document.getElementById('status');
const groupsBody = document.getElementById('groups-body');

async function getMemberCount(groupId) {
  const details = await api.get(`/groups/${groupId}`);
  return Array.isArray(details?.members) ? details.members.length : 0;
}

async function loadGroups() {
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
        <button class="btn secondary" data-members-group="${g.groupId}">Members</button>
        <button class="btn ghost" data-expenses-group="${g.groupId}">Expenses</button>
      </td>
    `;
    groupsBody.appendChild(tr);
  }

  for (const btn of groupsBody.querySelectorAll('button[data-members-group]')) {
    btn.addEventListener('click', () => {
      const groupId = btn.dataset.membersGroup;
      localStorage.setItem('spendsmart.lastGroupId', groupId);
      window.location.href = `./groups-members.html?groupId=${encodeURIComponent(groupId)}`;
    });
  }

  for (const btn of groupsBody.querySelectorAll('button[data-expenses-group]')) {
    btn.addEventListener('click', () => {
      const groupId = btn.dataset.expensesGroup;
      localStorage.setItem('spendsmart.lastGroupId', groupId);
      window.location.href = './expenses.html';
    });
  }

  return summaries;
}

document.getElementById('refresh-groups-btn').addEventListener('click', async () => {
  try {
    await loadGroups();
    setStatus(statusEl, 'ok', 'Groups loaded.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('create-group-btn').addEventListener('click', async () => {
  try {
    const name = document.getElementById('group-name').value.trim();
    if (!name) throw new Error('Group name is required.');

    const result = await api.post('/groups', { name });
    const groupId = result?.groupId || result?.id;
    if (groupId) {
      localStorage.setItem('spendsmart.lastGroupId', groupId);
    }

    await loadGroups();
    setStatus(statusEl, 'ok', 'Group created.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

loadGroups()
  .then(() => setStatus(statusEl, 'ok', 'Groups loaded.'))
  .catch((err) => setStatus(statusEl, 'warn', `Could not auto-load groups: ${err.message}`));
