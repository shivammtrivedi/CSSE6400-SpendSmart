import { api } from '../api.js';
import { nav, setStatus } from '../ui.js';

nav('bank');

const provider = new URLSearchParams(window.location.search).get('provider') ?? 'bankapi';
const statusEl = document.getElementById('status');

document.getElementById('provider-label').textContent =
  provider === 'bankapi' ? 'BankApi' : provider.toUpperCase();

const fields = document.getElementById('fields');
if (provider === 'bankapi') {
  fields.innerHTML = `
    <label>Email</label>
    <input id="f-email" type="email" placeholder="you@example.com" />
    <label class="spaced">Password</label>
    <input id="f-password" type="password" placeholder="password" />`;
} else {
  fields.innerHTML = `
    <label>Username</label>
    <input id="f-username" type="text" placeholder="username" />
    <label class="spaced">Password</label>
    <input id="f-password" type="password" placeholder="password" />
    <label class="spaced">Consumer Key</label>
    <input id="f-consumer-key" type="text" placeholder="consumer key" />`;
}

document.getElementById('connect-btn').addEventListener('click', async () => {
  const btn = document.getElementById('connect-btn');
  btn.disabled = true;
  statusEl.hidden = true;

  const credentials = provider === 'bankapi'
    ? {
        email: fields.querySelector('#f-email').value.trim(),
        password: fields.querySelector('#f-password').value
      }
    : {
        username: fields.querySelector('#f-username').value.trim(),
        password: fields.querySelector('#f-password').value,
        consumer_key: fields.querySelector('#f-consumer-key').value.trim()
      };

  try {
    await api.post(`/login/${provider}`, credentials);
    statusEl.hidden = false;
    setStatus(statusEl, 'ok', 'Connected! Redirecting…');
    setTimeout(() => { window.location.href = './home.html'; }, 1500);
  } catch (err) {
    statusEl.hidden = false;
    setStatus(statusEl, 'err', err.message ?? 'Login failed. Check your credentials.');
    btn.disabled = false;
  }
});
