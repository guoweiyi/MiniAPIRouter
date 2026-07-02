const API = (() => {
  let token = localStorage.getItem('miniapi_token') || '';

  async function init() {
    if (!token) {
      try {
        const res = await fetch('/api/v1/system/info');
        const json = await res.json();
        if (json.code === 0 && json.data.auth_token) {
          token = json.data.auth_token;
          localStorage.setItem('miniapi_token', token);
        }
      } catch (e) {
        console.error('Failed to get token:', e);
      }
    }
    return token;
  }

  async function request(method, url, body) {
    const opts = {
      method,
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json',
      },
    };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(url, opts);
    const json = await res.json();
    if (json.code !== 0 && json.code !== undefined) {
      throw new Error(json.message || 'Request failed');
    }
    return json.data !== undefined ? json.data : json;
  }

  return {
    init,
    get: (url) => request('GET', url),
    post: (url, body) => request('POST', url, body),
    put: (url, body) => request('PUT', url, body),
    del: (url) => request('DELETE', url),
    patch: (url, body) => request('PATCH', url, body),
    getToken: () => token,
  };
})();

function showToast(msg, type = 'success') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = 'toast toast-' + type;
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function renderMarkdown(text) {
  if (!text) return '';
  let html = escapeHtml(text);
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (m, lang, code) => {
    return '<pre><code>' + escapeHtml(code.trim()) + '</code></pre>';
  });
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/^\s*[-*]\s+(.+)$/gm, '<li>$1</li>');
  html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');
  html = html.replace(/\n\n/g, '</p><p>');
  html = html.replace(/\n/g, '<br>');
  return '<p>' + html + '</p>';
}

function switchTab(tabName) {
  document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
  document.querySelector('.nav-tab[data-tab="' + tabName + '"]').classList.add('active');
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.getElementById('view-' + tabName).classList.add('active');
  if (tabName === 'config') Config.load();
  if (tabName === 'log') Log.load();
}

document.addEventListener('DOMContentLoaded', async () => {
  await API.init();
  document.querySelectorAll('.nav-tab').forEach(tab => {
    tab.addEventListener('click', () => switchTab(tab.dataset.tab));
  });
  Chat.init();
  Config.init();
  Log.init();
});
