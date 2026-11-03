export function setStatus(el, type, text) {
  if (!el) return;
  el.className = `status ${type}`;
  el.textContent = text;
}

export function clearNode(node) {
  while (node.firstChild) node.removeChild(node.firstChild);
}

export function nav(page) {
  for (const link of document.querySelectorAll('.nav a')) {
    link.classList.toggle('active', link.dataset.page === page);
  }
}

export function parseJsonInput(id, fallback = []) {
  const value = document.getElementById(id)?.value?.trim();
  if (!value) return fallback;
  return JSON.parse(value);
}
