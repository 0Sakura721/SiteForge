/**
 * SiteForge 管理面板 - 前端交互逻辑
 */

const API = {
  async get(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  async post(url, data) {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  async put(url, data) {
    const res = await fetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  async del(url) {
    const res = await fetch(url, { method: 'DELETE' });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
};

// ---- 导航 ----
function navigate(page) {
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  document.querySelector(`[data-page="${page}"]`)?.classList.add('active');
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById(`page-${page}`)?.classList.add('active');

  if (page === 'dashboard') fetchDashboard();
  if (page === 'sites') fetchSites();
  if (page === 'create') fetchTemplates();
  if (page === 'files') initFileManager();
  if (page === 'databases') initDatabases();
  if (page === 'settings') fetchSettings();
}

document.querySelectorAll('.nav-item').forEach(item => {
  item.addEventListener('click', (e) => {
    e.preventDefault();
    navigate(item.dataset.page);
  });
});

// ---- Toast ----
function toast(msg, type = 'success') {
  const container = document.getElementById('toastContainer');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = msg;
  container.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

// ---- 确认弹窗 ----
let confirmCallback = null;
function showConfirm(title, msg, cb) {
  document.getElementById('confirmModalTitle').textContent = title;
  document.getElementById('confirmModalMsg').textContent = msg;
  document.getElementById('confirmModalBtn').onclick = () => {
    closeModal('confirmModal');
    if (cb) cb();
  };
  document.getElementById('confirmModal').classList.add('show');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('show');
}

// ====== 控制台 ======
async function fetchDashboard() {
  await fetchSystemStatus();
  fetchSites(); // 也会更新统计
}

async function fetchSystemStatus() {
  try {
    const s = await API.get('/api/status');
    document.getElementById('statTotal').textContent = s.total_sites;
    document.getElementById('statRunning').textContent = s.running_sites;
    document.getElementById('statStopped').textContent = s.total_sites - s.running_sites;

    const info = document.getElementById('sysInfo');
    info.innerHTML = `
      <div class="info-item"><span class="info-key">平台版本</span><span class="info-val">${s.version}</span></div>
      <div class="info-item"><span class="info-key">Python</span><span class="info-val">${s.python.split('\\n')[0]}</span></div>
      <div class="info-item"><span class="info-key">端口范围</span><span class="info-val">${s.port_range}</span></div>
    `;
  } catch (e) {
    document.getElementById('statusIndicator').className = 'status-dot error';
    document.getElementById('statusText').textContent = '错误';
  }
}

// ====== 站点管理 ======
async function fetchSites() {
  try {
    const sites = await API.get('/api/sites');
    renderSitesTable(sites);
    // 更新控制台统计
    const running = sites.filter(s => s.status === 'running').length;
    document.getElementById('statTotal').textContent = sites.length;
    document.getElementById('statRunning').textContent = running;
    document.getElementById('statStopped').textContent = sites.length - running;
  } catch (e) {
    toast('加载站点列表失败: ' + e.message, 'error');
  }
}

function renderSitesTable(sites) {
  const tbody = document.getElementById('sitesTableBody');
  const empty = document.getElementById('sitesEmpty');

  if (sites.length === 0) {
    tbody.innerHTML = '';
    empty.classList.add('show');
    return;
  }
  empty.classList.remove('show');

  const typeNames = { static: '静态', php: 'PHP', 'single-page': 'SPA' };
  tbody.innerHTML = sites.map(s => `
    <tr>
      <td><strong>${esc(s.name)}</strong>${s.description ? `<br><small style="color:#999">${esc(s.description)}</small>` : ''}</td>
      <td>${typeNames[s.type] || s.type}</td>
      <td><code>${s.port}</code></td>
      <td><span class="badge badge-${s.status}">${s.status === 'running' ? '运行中' : '已停止'}</span></td>
      <td>${s.created_at?.slice(0, 10) || '-'}</td>
      <td>
        ${s.status === 'stopped'
          ? `<button class="btn btn-sm btn-success" onclick="startSite(${s.id})">启动</button>`
          : `<button class="btn btn-sm btn-outline" onclick="stopSite(${s.id})">停止</button>`
        }
        ${s.status === 'running'
          ? `<button class="btn btn-sm btn-outline" onclick="restartSite(${s.id})">重启</button>`
          : ''
        }
        <button class="btn btn-sm btn-outline" onclick="window.open('/s/${s.name}/', '_blank')" ${s.status !== 'running' ? 'disabled' : ''}>访问</button>
        <button class="btn btn-sm btn-danger" onclick="deleteSite(${s.id}, '${esc(s.name)}')">删除</button>
      </td>
    </tr>
  `).join('');
}

async function startSite(id) {
  try {
    await API.post(`/api/sites/${id}/start`);
    toast('站点已启动');
    fetchSites();
  } catch (e) { toast('启动失败: ' + e.message, 'error'); }
}

async function stopSite(id) {
  try {
    await API.post(`/api/sites/${id}/stop`);
    toast('站点已停止');
    fetchSites();
  } catch (e) { toast('停止失败: ' + e.message, 'error'); }
}

async function restartSite(id) {
  try {
    await API.post(`/api/sites/${id}/restart`);
    toast('站点已重启');
    fetchSites();
  } catch (e) { toast('重启失败: ' + e.message, 'error'); }
}

function deleteSite(id, name) {
  showConfirm('删除站点', `确定要删除站点 "${name}" 吗？此操作不可恢复。`, async () => {
    try {
      await API.del(`/api/sites/${id}`);
      toast('站点已删除');
      fetchSites();
    } catch (e) { toast('删除失败: ' + e.message, 'error'); }
  });
}

// ====== 创建站点 ======
async function fetchTemplates() {
  try {
    const templates = await API.get('/api/templates');
    const sel = document.getElementById('siteTemplate');
    sel.innerHTML = '';
    for (const [key, tpl] of Object.entries(templates)) {
      const opt = document.createElement('option');
      opt.value = key;
      opt.textContent = `${tpl.name} - ${tpl.description}`;
      sel.appendChild(opt);
    }
    updateTemplatePreview(templates);
    sel.onchange = () => updateTemplatePreview(templates);
  } catch (e) { toast('加载模板失败', 'error'); }
}

function updateTemplatePreview(templates) {
  const key = document.getElementById('siteTemplate')?.value;
  if (templates && templates[key]) {
    document.getElementById('templatePreviewText').textContent = templates[key].preview || templates[key].description;
  }
}

function onSiteTypeChange() {
  const type = document.getElementById('siteType').value;
  const tplSel = document.getElementById('siteTemplate');
  // 根据类型过滤模板显示
  Array.from(tplSel.options).forEach(opt => {
    // 简单逻辑：PHP 类型只能选 php-* 模板
    if (type === 'php') {
      opt.style.display = opt.value.startsWith('php') || opt.value === 'blank' ? '' : 'none';
    } else {
      opt.style.display = opt.value.startsWith('php') ? 'none' : '';
    }
  });
}

async function handleCreateSite(e) {
  e.preventDefault();
  const btn = document.getElementById('createBtn');
  btn.disabled = true;
  btn.innerHTML = '创建中...';

  try {
    const data = {
      name: document.getElementById('siteName').value.trim(),
      type: document.getElementById('siteType').value,
      template: document.getElementById('siteTemplate').value,
      description: document.getElementById('siteDesc').value.trim(),
    };
    const site = await API.post('/api/sites', data);
    toast(`站点 "${site.name}" 创建成功！`);
    document.getElementById('createForm').reset();
    navigate('sites');
  } catch (e) {
    toast('创建失败: ' + e.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> 立即创建站点';
  }
}

// ====== 文件管理 ======
let currentFileSiteId = null;
let currentFilePath = '';
let currentEditorPath = '';

async function initFileManager() {
  // 填充站点下拉
  try {
    const sites = await API.get('/api/sites');
    const sel = document.getElementById('fileSiteSelect');
    sel.innerHTML = '<option value="">-- 选择站点 --</option>' +
      sites.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
  } catch (e) { /* ignore */ }
}

async function loadFileManager() {
  const sel = document.getElementById('fileSiteSelect');
  currentFileSiteId = sel.value ? parseInt(sel.value) : null;
  if (!currentFileSiteId) {
    document.getElementById('fileManagerContent').style.display = 'none';
    document.getElementById('fileEditor').style.display = 'none';
    return;
  }
  document.getElementById('fileManagerContent').style.display = 'block';
  document.getElementById('fileEditor').style.display = 'none';
  currentFilePath = '';
  await loadFiles('');
}

async function loadFiles(path) {
  if (!currentFileSiteId) return;
  currentFilePath = path;
  try {
    const files = await API.get(`/api/sites/${currentFileSiteId}/files?path=${encodeURIComponent(path)}`);
    renderFiles(files);
    updateBreadcrumb(path);
  } catch (e) { toast('加载文件失败: ' + e.message, 'error'); }
}

function renderFiles(files) {
  const container = document.getElementById('fileTable');
  if (files.length === 0) {
    container.innerHTML = '<div class="empty-state show"><p>目录为空</p></div>';
    return;
  }
  const iconMap = { directory: '📁', '.html': '🌐', '.css': '🎨', '.js': '📜', '.php': '🐘', '.json': '📋', '.md': '📝', '.txt': '📄', '.py': '🐍', '.sql': '🗄️', '.zip': '📦', '.jpg': '🖼️', '.png': '🖼️', '.svg': '🖼️', '.pdf': '📕' };

  container.innerHTML = files.map(f => {
    const icon = f.type === 'directory' ? iconMap.directory : (iconMap[f.ext] || '📄');
    const size = f.type === 'directory' ? '-' : formatSize(f.size);
    const clickAction = f.type === 'directory'
      ? `loadFiles('${currentFilePath ? currentFilePath + '/' + f.name : f.name}')`
      : `openFile('${currentFilePath ? currentFilePath + '/' + f.name : f.name}')`;
    return `
      <div class="file-row" ondblclick="${clickAction}">
        <span class="icon">${icon}</span>
        <span class="name" style="cursor:pointer" onclick="${clickAction}">${esc(f.name)}</span>
        <span class="size">${size}</span>
        <span class="date">${f.modified?.slice(0, 16) || '-'}</span>
        <span class="actions">
          <button class="btn btn-sm btn-outline" onclick="event.stopPropagation();renameItem('${esc_attr(f.name)}')">重命名</button>
          <button class="btn btn-sm btn-danger" onclick="event.stopPropagation();deleteFileItem('${esc_attr(f.name)}')">删除</button>
        </span>
      </div>`;
  }).join('');
}

function updateBreadcrumb(path) {
  const bc = document.getElementById('fileBreadcrumb');
  if (!path) {
    bc.innerHTML = '<strong>/ (根目录)</strong>';
    return;
  }
  const parts = path.split('/');
  let cum = '';
  let html = '<a onclick="loadFiles(\'\')">/</a> ';
  parts.forEach((p, i) => {
    cum = cum ? cum + '/' + p : p;
    html += i === parts.length - 1
      ? `<strong>${esc(p)}</strong>`
      : `<a onclick="loadFiles('${cum}')">${esc(p)}</a> / `;
  });
  bc.innerHTML = html;
}

async function openFile(path) {
  try {
    const file = await API.get(`/api/sites/${currentFileSiteId}/files/read?path=${encodeURIComponent(path)}`);
    currentEditorPath = path;
    document.getElementById('fileManagerContent').style.display = 'none';
    document.getElementById('fileEditor').style.display = 'block';
    document.getElementById('editorFilename').textContent = file.name;
    document.getElementById('editorTextarea').value = file.content;
  } catch (e) { toast('打开文件失败: ' + e.message, 'error'); }
}

function closeEditor() {
  document.getElementById('fileEditor').style.display = 'none';
  document.getElementById('fileManagerContent').style.display = 'block';
  currentEditorPath = '';
  loadFiles(currentFilePath);
}

async function saveFile() {
  if (!currentEditorPath) return;
  const content = document.getElementById('editorTextarea').value;
  try {
    await API.post(`/api/sites/${currentFileSiteId}/files/write`, { path: currentEditorPath, content });
    toast('文件已保存');
  } catch (e) { toast('保存失败: ' + e.message, 'error'); }
}

let newItemType = 'file';
function showNewItemModal(type) {
  newItemType = type;
  document.getElementById('newItemModalTitle').textContent = type === 'file' ? '新建文件' : '新建目录';
  document.getElementById('newItemName').value = '';
  document.getElementById('newItemModal').classList.add('show');
}

async function createNewItem() {
  const name = document.getElementById('newItemName').value.trim();
  if (!name) return;
  closeModal('newItemModal');
  try {
    if (newItemType === 'file') {
      await API.post(`/api/sites/${currentFileSiteId}/files/create`, { path: currentFilePath, name });
    } else {
      await API.post(`/api/sites/${currentFileSiteId}/files/mkdir`, { path: currentFilePath, name });
    }
    toast('创建成功');
    loadFiles(currentFilePath);
  } catch (e) { toast('创建失败: ' + e.message, 'error'); }
}

async function deleteFileItem(name) {
  showConfirm('删除', `确定删除 "${name}" 吗？`, async () => {
    const fullPath = currentFilePath ? currentFilePath + '/' + name : name;
    try {
      await API.del(`/api/sites/${currentFileSiteId}/files?path=${encodeURIComponent(fullPath)}`);
      toast('已删除');
      loadFiles(currentFilePath);
    } catch (e) { toast('删除失败: ' + e.message, 'error'); }
  });
}

async function renameItem(oldName) {
  const newName = prompt('输入新名称:', oldName);
  if (!newName || newName === oldName) return;
  const oldPath = currentFilePath ? currentFilePath + '/' + oldName : oldName;
  try {
    await API.put(`/api/sites/${currentFileSiteId}/files/rename`, { path: oldPath, new_name: newName });
    toast('已重命名');
    loadFiles(currentFilePath);
  } catch (e) { toast('重命名失败: ' + e.message, 'error'); }
}

async function handleFileUpload() {
  const input = document.getElementById('fileUploadInput');
  const files = input.files;
  if (!files.length) return;
  for (const file of files) {
    const formData = new FormData();
    formData.append('file', file);
    try {
      await fetch(`/api/sites/${currentFileSiteId}/files/upload?path=${encodeURIComponent(currentFilePath)}`, {
        method: 'POST',
        body: formData,
      });
      toast(`"${file.name}" 上传成功`);
    } catch (e) { toast(`上传 "${file.name}" 失败`, 'error'); }
  }
  input.value = '';
  loadFiles(currentFilePath);
}

// ====== 数据库管理 ======
async function initDatabases() {
  try {
    const sites = await API.get('/api/sites');
    const sel = document.getElementById('dbSiteSelect');
    sel.innerHTML = '<option value="">-- 选择站点 --</option>' +
      sites.map(s => `<option value="${s.id}">${esc(s.name)}</option>`).join('');
  } catch (e) { /* ignore */ }
}

async function loadDatabases() {
  const siteId = document.getElementById('dbSiteSelect').value;
  if (!siteId) {
    document.getElementById('dbTableBody').innerHTML = '';
    return;
  }
  try {
    const dbs = await API.get(`/api/sites/${siteId}/databases`);
    document.getElementById('dbTableBody').innerHTML = dbs.length === 0
      ? '<tr><td colspan="4" style="text-align:center;color:#999;">暂无数据库</td></tr>'
      : dbs.map(d => `
        <tr>
          <td><strong>${esc(d.name)}.db</strong></td>
          <td><code style="font-size:0.78rem;">${esc(d.path)}</code></td>
          <td>${d.created_at?.slice(0, 10) || '-'}</td>
          <td><button class="btn btn-sm btn-danger" onclick="deleteDB(${d.id})">删除</button></td>
        </tr>
      `).join('');
  } catch (e) { toast('加载数据库失败', 'error'); }
}

function showCreateDBModal() {
  const siteId = document.getElementById('dbSiteSelect').value;
  if (!siteId) { toast('请先选择站点', 'warning'); return; }
  document.getElementById('newDBName').value = '';
  document.getElementById('createDBModal').classList.add('show');
}

async function createDatabase() {
  const siteId = document.getElementById('dbSiteSelect').value;
  const name = document.getElementById('newDBName').value.trim();
  if (!name) return;
  closeModal('createDBModal');
  try {
    await API.post(`/api/sites/${siteId}/databases`, { name });
    toast('数据库创建成功');
    loadDatabases();
  } catch (e) { toast('创建失败: ' + e.message, 'error'); }
}

function deleteDB(dbId) {
  const siteId = document.getElementById('dbSiteSelect').value;
  showConfirm('删除数据库', '确定删除此数据库吗？所有数据将丢失。', async () => {
    try {
      await API.del(`/api/sites/${siteId}/databases/${dbId}`);
      toast('数据库已删除');
      loadDatabases();
    } catch (e) { toast('删除失败', 'error'); }
  });
}

// ====== 系统设置 ======
async function fetchSettings() {
  try {
    const settings = await API.get('/api/settings');
    document.getElementById('settingTitle').value = settings.site_title || 'SiteForge';
    document.getElementById('settingTheme').value = settings.theme || 'light';
    document.getElementById('settingAutoStart').checked = settings.auto_start === 'true';
  } catch (e) { toast('加载设置失败', 'error'); }
}

async function handleSaveSettings(e) {
  e.preventDefault();
  try {
    await API.put('/api/settings', {
      site_title: document.getElementById('settingTitle').value,
      theme: document.getElementById('settingTheme').value,
      auto_start: document.getElementById('settingAutoStart').checked ? 'true' : 'false',
    });
    toast('设置已保存');
  } catch (e) { toast('保存失败: ' + e.message, 'error'); }
}

// ====== 工具函数 ======
function esc(s) {
  if (!s) return '';
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
function esc_attr(s) {
  return s.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"');
}
function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  while (bytes >= 1024 && i < units.length - 1) { bytes /= 1024; i++; }
  return bytes.toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
}

// ---- 键盘快捷键 ----
document.addEventListener('keydown', (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    if (currentEditorPath) { e.preventDefault(); saveFile(); }
  }
  if (e.key === 'Escape') {
    if (currentEditorPath) closeEditor();
    document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'));
  }
});

// ---- 初始化 ----
navigate('dashboard');
