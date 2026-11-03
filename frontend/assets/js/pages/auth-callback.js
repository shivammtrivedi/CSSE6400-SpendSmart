import { handleAuthCallback } from '../auth.js';
import { setStatus } from '../ui.js';

const statusEl = document.getElementById('status');

handleAuthCallback()
  .then(() => {
    setStatus(statusEl, 'ok', 'Sign in complete. Redirecting...');
    window.location.replace('./home.html');
  })
  .catch((err) => {
    setStatus(statusEl, 'err', `Login failed: ${err.message}`);
  });
