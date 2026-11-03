import { api } from '../api.js';
import { nav, setStatus } from '../ui.js';

nav('groups');

const statusEl = document.getElementById('status');

document.getElementById('create-group-btn').addEventListener('click', async () => {
  try {
    const name = document.getElementById('group-name').value.trim();
    if (!name) throw new Error('Group name is required.');

    const result = await api.post('/groups', { name });
    const groupId = result?.groupId || result?.id;
    if (groupId) {
      localStorage.setItem('spendsmart.lastGroupId', groupId);
    }

    setStatus(statusEl, 'ok', 'Group created. You can now manage members.');
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});
