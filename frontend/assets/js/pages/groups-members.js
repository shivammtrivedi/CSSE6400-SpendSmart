import { api } from '../api.js';
import { nav, setStatus, clearNode } from '../ui.js';

nav('groups');

const statusEl = document.getElementById('status');
const groupsBody = document.getElementById('groups-body');
const detailCard = document.getElementById('detail-card');
const detailTitle = document.getElementById('detail-title');
const membersList = document.getElementById('members-list');

let selectedGroupId = '';

async function openGroup(groupId) {
  selectedGroupId = groupId;
  localStorage.setItem('spendsmart.lastGroupId', groupId);

  const details = await api.get(`/groups/${groupId}`);
  detailTitle.textContent = `${details?.name || 'Group'} (${groupId})`;
  clearNode(membersList);

  for (const member of details?.members || []) {
    const li = document.createElement('li');
    li.textContent = member;
    membersList.appendChild(li);
  }

  detailCard.style.display = 'block';
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
      <td><button class="btn secondary" data-open-group="${g.groupId}">Manage</button></td>
    `;
    groupsBody.appendChild(tr);
  }

  for (const btn of groupsBody.querySelectorAll('button[data-open-group]')) {
    btn.addEventListener('click', async () => {
      const groupId = btn.dataset.openGroup;
      try {
        await openGroup(groupId);
        setStatus(statusEl, 'ok', 'Group loaded.');
      } catch (err) {
        setStatus(statusEl, 'err', err.message);
      }
    });
  }

  return summaries;
}

document.getElementById('load-groups-btn').addEventListener('click', async () => {
  try {
    await loadGroupsForPicker();
    setStatus(statusEl, 'ok', 'Groups loaded.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('add-member-btn').addEventListener('click', async () => {
  try {
    if (!selectedGroupId) throw new Error('Select a group first.');
    const user = document.getElementById('member-user-id').value.trim();
    if (!user) throw new Error('User ID is required.');

    await api.post(`/groups/${selectedGroupId}/members`, { user });
    await openGroup(selectedGroupId);
    setStatus(statusEl, 'ok', 'Member added.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('remove-member-btn').addEventListener('click', async () => {
  try {
    if (!selectedGroupId) throw new Error('Select a group first.');
    const user = document.getElementById('member-user-id').value.trim();
    if (!user) throw new Error('User ID is required.');

    await api.del(`/groups/${selectedGroupId}/members`, { user });
    await openGroup(selectedGroupId);
    setStatus(statusEl, 'ok', 'Member removed.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});

document.getElementById('leave-group-btn').addEventListener('click', () => {
  setStatus(statusEl, 'warn', 'Leave-group flow depends on user identity mapping and is not wired in this static MVP shell yet.');
});

loadGroupsForPicker()
  .then(async (groups) => {
    const query = new URLSearchParams(window.location.search);
    const preferredGroupId = query.get('groupId') || localStorage.getItem('spendsmart.lastGroupId') || '';
    if (preferredGroupId && groups.some((g) => g.groupId === preferredGroupId)) {
      await openGroup(preferredGroupId);
      setStatus(statusEl, 'ok', 'Loaded groups and selected group.');
      return;
    }
    setStatus(statusEl, 'ok', 'Groups loaded.');
  })
  .catch((err) => setStatus(statusEl, 'warn', `Could not auto-load groups: ${err.message}`));
