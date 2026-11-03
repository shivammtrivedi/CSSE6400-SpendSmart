import { nav, setStatus } from '../ui.js';
import { currentUserIdFromJwt, isAuthenticated, logout } from '../auth.js';

nav('home');

document.getElementById('uid').textContent = currentUserIdFromJwt();
if (!isAuthenticated()) {
  setStatus(document.getElementById('status'), 'warn', 'No active session. Please sign in from Auth page.');
}

const apiBaseInput = document.getElementById('api-base-input');
apiBaseInput.value = localStorage.getItem('spendsmart.apiBaseUrl') || 'http://localhost:8082/api/v1';
document.getElementById('cognito-url-input').value = localStorage.getItem('spendsmart.cognitoDomain') || '';

document.getElementById('save-api-base-btn').addEventListener('click', () => {
  localStorage.setItem('spendsmart.apiBaseUrl', apiBaseInput.value.trim());
  setStatus(document.getElementById('status'), 'ok', 'API base URL saved.');
});

document.getElementById('save-cognito-url-btn').addEventListener('click', () => {
  const value = document.getElementById('cognito-url-input').value.trim();
  localStorage.setItem('spendsmart.cognitoDomain', value);
  setStatus(document.getElementById('status'), 'ok', 'Cognito domain override saved.');
});

document.getElementById('logout-btn').addEventListener('click', () => {
  logout();
});
