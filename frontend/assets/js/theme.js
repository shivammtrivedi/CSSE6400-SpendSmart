(function () {
  var KEY = 'spendsmart.theme';
  var root = document.documentElement;

  function apply(theme) {
    root.dataset.theme = theme === 'dark' ? 'dark' : 'light';
  }

  try {
    apply(localStorage.getItem(KEY) || 'light');
  } catch (e) {
    apply('light');
  }

  function wire() {
    var btn = document.getElementById('theme-toggle');
    if (!btn) return;
    btn.addEventListener('click', function () {
      var next = root.dataset.theme === 'dark' ? 'light' : 'dark';
      apply(next);
      try { localStorage.setItem(KEY, next); } catch (e) {}
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', wire);
  } else {
    wire();
  }
})();
