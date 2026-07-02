const Log = (() => {
  let currentPage = 1;
  let total = 0;
  const pageSize = 20;

  function init() {
    document.getElementById('btn-log-search').addEventListener('click', () => {
      currentPage = 1;
      load();
    });
    document.getElementById('btn-log-prev').addEventListener('click', () => {
      if (currentPage > 1) { currentPage--; load(); }
    });
    document.getElementById('btn-log-next').addEventListener('click', () => {
      if (currentPage * pageSize < total) { currentPage++; load(); }
    });
    document.getElementById('btn-dashboard').addEventListener('click', loadDashboard);
  }

  async function load() {
    const model = document.getElementById('log-filter-model').value;
    const status = document.getElementById('log-filter-status').value;
    const trace = document.getElementById('log-filter-trace').value;

    let url = '/api/v1/logs?page=' + currentPage + '&page_size=' + pageSize;
    if (model) url += '&model=' + encodeURIComponent(model);
    if (status) url += '&status=' + encodeURIComponent(status);
    if (trace) url += '&trace_id=' + encodeURIComponent(trace);

    try {
      const data = await API.get(url);
      total = data.total || 0;
      renderLogs(data.list || []);
      renderPagination();
    } catch (e) {
      showToast('加载日志失败: ' + e.message, 'error');
    }
  }

  function renderLogs(list) {
    const container = document.getElementById('log-table-body');
    if (list.length === 0) {
      container.innerHTML = '<tr><td colspan="8" class="empty-state">暂无日志</td></tr>';
      return;
    }
    container.innerHTML = list.map(log => `
      <tr class="cursor-pointer" onclick="Log.showDetail(${log.id})">
        <td class="text-mono text-sm">${escapeHtml(log.trace_id)}</td>
        <td>${escapeHtml(log.model)}</td>
        <td>${escapeHtml(log.mapped_provider)}</td>
        <td>${log.prompt_tokens || 0} / ${log.completion_tokens || 0} / ${log.total_tokens || 0}</td>
        <td>${log.latency_ms || 0}ms</td>
        <td><span class="badge ${log.status === 'success' ? 'badge-green' : log.status === 'failed' ? 'badge-red' : 'badge-orange'}">${escapeHtml(log.status)}</span></td>
        <td>${log.fallback_count || 0}</td>
        <td class="text-sm text-dim">${escapeHtml(log.created_at)}</td>
      </tr>
    `).join('');
  }

  function renderPagination() {
    document.getElementById('log-page-info').textContent =
      `第 ${currentPage} 页 / 共 ${Math.ceil(total / pageSize) || 1} 页 (${total} 条)`;
  }

  async function showDetail(id) {
    const overlay = document.getElementById('modal-overlay');
    overlay.querySelector('.modal').innerHTML = `
      <div class="modal-header">
        <span class="modal-title">日志详情 #${id}</span>
        <button class="btn btn-sm btn-icon" onclick="Config.closeModal()">✕</button>
      </div>
      <div class="modal-body"><div class="empty-state"><span class="spinner"></span> 加载中...</div></div>
    `;
    overlay.classList.add('active');

    try {
      const data = await API.get('/api/v1/logs/' + id);
      let html = '<div class="form-group"><label>基本信息</label>';
      html += `<div class="text-sm" style="background:var(--bg-input);padding:12px;border-radius:4px;">
        <strong>Trace ID:</strong> ${escapeHtml(data.trace_id)}<br>
        <strong>Request ID:</strong> ${escapeHtml(data.request_id)}<br>
        <strong>协议:</strong> ${escapeHtml(data.protocol)} · <strong>模型:</strong> ${escapeHtml(data.model)} · <strong>供应商:</strong> ${escapeHtml(data.mapped_provider)}<br>
        <strong>Key ID:</strong> ${data.api_key_id || '-'} · <strong>规则 ID:</strong> ${data.route_rule_id || '-'}${data.intent ? '<br><strong>意图:</strong> ' + escapeHtml(data.intent) : ''}
        <br><strong>Tokens:</strong> ${data.prompt_tokens||0} (prompt) + ${data.completion_tokens||0} (completion) = ${data.total_tokens||0} (total)
        <br><strong>延迟:</strong> ${data.latency_ms||0}ms · <strong>TTFT:</strong> ${data.ttft_ms||0}ms
        <br><strong>状态:</strong> <span class="badge ${data.status==='success'?'badge-green':'badge-red'}">${escapeHtml(data.status)}</span> · <strong>Fallback:</strong> ${data.fallback_count||0}
        <br><strong>时间:</strong> ${escapeHtml(data.created_at)}
      </div></div>`;

      if (data.messages) {
        html += '<div class="form-group mt-16"><label>请求消息 (Prompt)</label>';
        html += `<pre style="max-height:200px;overflow-y:auto;">${escapeHtml(data.messages)}</pre></div>`;
      }
      if (data.response_content) {
        html += '<div class="form-group mt-16"><label>响应内容 (Response)</label>';
        html += `<pre style="max-height:200px;overflow-y:auto;">${escapeHtml(data.response_content)}</pre></div>`;
      }
      if (data.error_code || data.error_message) {
        html += '<div class="form-group mt-16"><label>错误信息</label>';
        html += `<div style="background:rgba(248,113,113,0.1);padding:12px;border-radius:4px;color:var(--red);">`;
        html += `<strong>${escapeHtml(data.error_code||'')}</strong>: ${escapeHtml(data.error_message||'')}</div></div>`;
      }

      overlay.querySelector('.modal-body').innerHTML = html;
    } catch (e) {
      overlay.querySelector('.modal-body').innerHTML =
        '<div class="empty-state" style="color:var(--red);">加载失败: ' + escapeHtml(e.message) + '</div>';
    }
  }

  async function loadDashboard() {
    const overlay = document.getElementById('modal-overlay');
    overlay.querySelector('.modal').innerHTML = `
      <div class="modal-header">
        <span class="modal-title">数据看板</span>
        <button class="btn btn-sm btn-icon" onclick="Config.closeModal()">✕</button>
      </div>
      <div class="modal-body"><div class="empty-state"><span class="spinner"></span> 加载中...</div></div>
    `;
    overlay.classList.add('active');

    try {
      const data = await API.get('/api/v1/logs/dashboard');
      let html = '<div class="stats-grid">';
      html += statCard('总请求', data.total_requests || 0);
      html += statCard('总 Token', data.total_tokens || 0);
      html += statCard('平均延迟', (data.avg_latency_ms || 0) + 'ms');
      html += statCard('成功率', ((data.success_rate || 0) * 100).toFixed(1) + '%');
      html += statCard('Fallback 率', ((data.fallback_rate || 0) * 100).toFixed(1) + '%');
      html += '</div>';

      if (data.model_distribution && data.model_distribution.length) {
        html += '<div class="form-group mt-16"><label>模型分布</label>';
        html += '<table><thead><tr><th>模型</th><th>请求数</th></tr></thead><tbody>';
        data.model_distribution.forEach(m => {
          html += `<tr><td>${escapeHtml(m.model)}</td><td>${m.cnt}</td></tr>`;
        });
        html += '</tbody></table></div>';
      }

      if (data.provider_distribution && data.provider_distribution.length) {
        html += '<div class="form-group mt-16"><label>供应商分布</label>';
        html += '<table><thead><tr><th>供应商</th><th>请求数</th><th>占比</th></tr></thead><tbody>';
        data.provider_distribution.forEach(p => {
          html += `<tr><td>${escapeHtml(p.provider)}</td><td>${p.cnt}</td><td>${((p.percentage||0)*100).toFixed(1)}%</td></tr>`;
        });
        html += '</tbody></table></div>';
      }

      overlay.querySelector('.modal-body').innerHTML = html;
    } catch (e) {
      overlay.querySelector('.modal-body').innerHTML =
        '<div class="empty-state" style="color:var(--red);">加载失败: ' + escapeHtml(e.message) + '</div>';
    }
  }

  function statCard(label, value) {
    return `<div class="stat-card"><div class="stat-label">${label}</div><div class="stat-value">${value}</div></div>`;
  }

  return { init, load, showDetail };
})();
