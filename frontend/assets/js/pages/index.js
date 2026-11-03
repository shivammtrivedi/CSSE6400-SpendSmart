import { isAuthenticated, startLogin } from '../auth.js';
import { setStatus } from '../ui.js';

const statusEl = document.getElementById('status');
const cognitoBtn = document.getElementById('cognito-login-btn');

if (isAuthenticated()) {
  setStatus(statusEl, 'ok', 'You are already signed in. Continue to Home.');
}

cognitoBtn.addEventListener('click', async (e) => {
  e.preventDefault();
  try {
    await startLogin();
  } catch (err) {
    setStatus(statusEl, 'err', err.message);
  }
});
