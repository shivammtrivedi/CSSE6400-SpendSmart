import { config } from './config.js';
import { getAccessToken } from './auth.js';

const RETRY_DELAYS_MS = [1000, 2000, 5000, 10000];
const SSE_ENABLED = true;

function resolveApiUrl(path) {
  if (/^https?:\/\//.test(path)) {
    return path;
  }
  if (path.startsWith('/')) {
    return `${window.location.origin}${path}`;
  }
  return `${window.location.origin}/${path}`;
}

async function consumeEventStream(stream, onMessage) {
  const reader = stream.getReader();
  const decoder = new TextDecoder('utf-8');

  let buffer = '';
  let eventName = 'message';
  let dataLines = [];

  const dispatch = () => {
    if (dataLines.length === 0) {
      eventName = 'message';
      return;
    }

    const dataText = dataLines.join('\n');
    let data = dataText;
    try {
      data = JSON.parse(dataText);
    } catch {
      // Keep original string payload when the event data is not JSON.
    }
    onMessage({ type: eventName, data });

    eventName = 'message';
    dataLines = [];
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      dispatch();
      return;
    }

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split(/\r?\n/);
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (!line) {
        dispatch();
        continue;
      }
      if (line.startsWith(':')) {
        continue;
      }
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim() || 'message';
        continue;
      }
      if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart());
      }
    }
  }
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function connectGroupStream(groupId, { onEvent, onStatus } = {}) {
  let active = true;
  let retryAttempt = 0;
  let abortController = null;

  const streamPath = `${config.apiBaseUrl}/expense/events/${encodeURIComponent(groupId)}`;
  const streamUrl = resolveApiUrl(streamPath);

  const emitStatus = (state, detail = '') => {
    if (typeof onStatus === 'function') {
      onStatus({ state, detail });
    }
  };

  if (!SSE_ENABLED) {
    emitStatus('closed', 'Live updates temporarily disabled');
    return {
      close() {
        emitStatus('closed');
      }
    };
  }

  const run = async () => {
    while (active) {
      try {
        const token = await getAccessToken();
        if (!token) {
          throw new Error('Missing access token for SSE stream');
        }

        abortController = new AbortController();
        emitStatus('connecting');

        const res = await fetch(streamUrl, {
          method: 'GET',
          headers: {
            Accept: 'text/event-stream, application/json',
            Authorization: `Bearer ${token}`,
            'Cache-Control': 'no-cache'
          },
          signal: abortController.signal
        });

        if (!res.ok) {
          const errorText = await res.text();
          const detail = errorText?.trim()
            ? `: ${errorText.trim().slice(0, 200)}`
            : '';
          throw new Error(`SSE stream failed (${res.status})${detail}`);
        }
        if (!res.body) {
          throw new Error('SSE stream did not provide a response body');
        }

        retryAttempt = 0;
        emitStatus('connected');
        await consumeEventStream(res.body, (message) => {
          if (typeof onEvent === 'function') {
            onEvent(message);
          }
        });

        if (active) {
          throw new Error('SSE stream ended unexpectedly');
        }
      } catch (err) {
        if (!active) {
          return;
        }

        const detail = err?.message || 'Unknown stream error';
        const delay = RETRY_DELAYS_MS[Math.min(retryAttempt, RETRY_DELAYS_MS.length - 1)];
        retryAttempt += 1;
        emitStatus('reconnecting', `${detail}. Retrying in ${Math.round(delay / 1000)}s.`);
        await wait(delay);
      }
    }
  };

  run();

  return {
    close() {
      active = false;
      if (abortController) {
        abortController.abort();
      }
      emitStatus('closed');
    }
  };
}
