import { config } from './config.js';

const AUTH_STORAGE_KEY = 'spendsmart.auth';
const PKCE_VERIFIER_KEY = 'spendsmart.pkce_verifier';
const PKCE_STATE_KEY = 'spendsmart.pkce_state';
const REFRESH_SKEW_MS = 60 * 1000;

function parseJwt(token) {
  if (!token || token.split('.').length < 2) return null;
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}

function nowMs() {
  return Date.now();
}

function randomString(length = 64) {
  const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => charset[b % charset.length]).join('');
}

function base64UrlEncode(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function sha256(input) {
  const data = new TextEncoder().encode(input);
  return crypto.subtle.digest('SHA-256', data);
}

function getStoredAuth() {
  const raw = sessionStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function storeAuth(auth) {
  sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

export function clearAuth() {
  sessionStorage.removeItem(AUTH_STORAGE_KEY);
  sessionStorage.removeItem(PKCE_VERIFIER_KEY);
  sessionStorage.removeItem(PKCE_STATE_KEY);
}

function isConfigured() {
  return Boolean(
    config.cognitoDomain &&
    config.cognitoClientId &&
    config.cognitoRedirectUri &&
    config.cognitoLogoutUri
  );
}

function toAuthRecord(tokenResponse) {
  const expiresIn = Number(tokenResponse.expires_in || 3600);
  return {
    accessToken: tokenResponse.access_token || '',
    idToken: tokenResponse.id_token || '',
    refreshToken: tokenResponse.refresh_token || '',
    expiresAt: nowMs() + (expiresIn * 1000)
  };
}

async function tokenRequest(params) {
  const body = new URLSearchParams(params);
  const res = await fetch(`${config.cognitoDomain}/oauth2/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString()
  });
  const payload = await res.json().catch(() => ({}));
  if (!res.ok) {
    const reason = payload.error_description || payload.error || `Token request failed (${res.status})`;
    throw new Error(reason);
  }
  return payload;
}

export async function startLogin() {
  if (!isConfigured()) {
    throw new Error('Cognito frontend auth config is missing. Check deploy-config.js values.');
  }

  const codeVerifier = randomString(96);
  const codeChallenge = base64UrlEncode(await sha256(codeVerifier));
  const state = randomString(32);

  sessionStorage.setItem(PKCE_VERIFIER_KEY, codeVerifier);
  sessionStorage.setItem(PKCE_STATE_KEY, state);

  const params = new URLSearchParams({
    client_id: config.cognitoClientId,
    response_type: 'code',
    scope: config.cognitoScopes,
    redirect_uri: config.cognitoRedirectUri,
    code_challenge_method: 'S256',
    code_challenge: codeChallenge,
    state
  });

  window.location.assign(`${config.cognitoDomain}/oauth2/authorize?${params.toString()}`);
}

export async function handleAuthCallback() {
  const params = new URLSearchParams(window.location.search);
  const error = params.get('error');
  if (error) {
    throw new Error(params.get('error_description') || error);
  }

  const code = params.get('code');
  const state = params.get('state');
  if (!code) {
    throw new Error('Missing authorization code in callback.');
  }

  const storedState = sessionStorage.getItem(PKCE_STATE_KEY);
  const codeVerifier = sessionStorage.getItem(PKCE_VERIFIER_KEY);
  if (!storedState || state !== storedState) {
    throw new Error('Invalid callback state.');
  }
  if (!codeVerifier) {
    throw new Error('Missing PKCE code verifier.');
  }

  const tokenPayload = await tokenRequest({
    grant_type: 'authorization_code',
    client_id: config.cognitoClientId,
    code,
    redirect_uri: config.cognitoRedirectUri,
    code_verifier: codeVerifier
  });

  storeAuth(toAuthRecord(tokenPayload));
  sessionStorage.removeItem(PKCE_STATE_KEY);
  sessionStorage.removeItem(PKCE_VERIFIER_KEY);
}

async function refreshAuth(auth) {
  if (!auth?.refreshToken) {
    clearAuth();
    throw new Error('Session expired. Please sign in again.');
  }

  const tokenPayload = await tokenRequest({
    grant_type: 'refresh_token',
    client_id: config.cognitoClientId,
    refresh_token: auth.refreshToken
  });

  const refreshed = {
    ...auth,
    ...toAuthRecord(tokenPayload),
    refreshToken: auth.refreshToken
  };
  storeAuth(refreshed);
  return refreshed;
}

export async function getAccessToken() {
  const auth = getStoredAuth();
  if (!auth?.accessToken) return '';
  if ((auth.expiresAt || 0) - REFRESH_SKEW_MS > nowMs()) {
    return auth.accessToken;
  }
  const refreshed = await refreshAuth(auth);
  return refreshed.accessToken || '';
}

export function isAuthenticated() {
  const auth = getStoredAuth();
  return Boolean(auth?.accessToken);
}

export function currentUserIdFromJwt() {
  const auth = getStoredAuth();
  const idPayload = parseJwt(auth?.idToken || '');
  if (idPayload?.sub) return idPayload.sub;

  const accessPayload = parseJwt(auth?.accessToken || '');
  return accessPayload?.sub || 'unknown';
}

export function logout() {
  clearAuth();
  if (!isConfigured()) return;

  const params = new URLSearchParams({
    client_id: config.cognitoClientId,
    logout_uri: config.cognitoLogoutUri
  });
  window.location.assign(`${config.cognitoDomain}/logout?${params.toString()}`);
}
