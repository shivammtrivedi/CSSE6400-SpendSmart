import { config } from './config.js';
import { getAccessToken } from './auth.js';

class ApiError extends Error {
  constructor(message, details = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = details.status ?? 0;
    this.path = details.path ?? '';
    this.code = details.code ?? '';
    this.requestId = details.requestId ?? '';
    this.responseBody = details.responseBody ?? null;
  }
}

function tryParseJson(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

function buildErrorMessage(path, status, body, text, contentType) {
  const isJson = contentType.includes('application/json');
  const htmlLike = !isJson && text && text.trim().startsWith('<');

  if (body?.message) {
    let message = body.message;
    if (body.code) {
      message = `${body.code}: ${message}`;
    }
    if (body.requestId) {
      message = `${message} (requestId: ${body.requestId})`;
    }
    return message;
  }

  if (htmlLike) {
    return `Request failed (${status}) for ${path}. Received HTML instead of API JSON (likely CloudFront/ALB route mismatch or upstream 404/403).`;
  }

  if (text && !isJson) {
    return `Request failed (${status}) for ${path}: ${text.slice(0, 200)}`;
  }

  return `Request failed (${status}) for ${path}`;
}

async function request(path, options = {}) {
  const token = await getAccessToken();
  const headers = {
    ...(options.headers || {})
  };
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let res;
  try {
    res = await fetch(`${config.apiBaseUrl}${path}`, {
      ...options,
      headers
    });
  } catch (err) {
    throw new ApiError(`Network failure calling ${path}: ${err.message}`, { path });
  }

  if (res.status === 204) return null;
  const text = await res.text();
  const contentType = res.headers.get('content-type') || '';
  const body = tryParseJson(text) || (text ? { raw: text } : null);

  if (!res.ok) {
    const message = buildErrorMessage(path, res.status, body, text, contentType);
    throw new ApiError(message, {
      status: res.status,
      path,
      code: body?.code,
      requestId: body?.requestId,
      responseBody: body
    });
  }
  return body;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  del: (path, body) => request(path, { method: 'DELETE', body: JSON.stringify(body) })
};
