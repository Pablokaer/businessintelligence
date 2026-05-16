// ── Theme (apply before paint to avoid flash) ─────────
(function(){
  if (localStorage.getItem('nf-theme') === 'light')
    document.documentElement.classList.add('light');
})();

// ── Sidebar mobile toggle ─────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  const sidebar  = document.getElementById('sidebar');
  const overlay  = document.getElementById('sidebarOverlay');
  const toggle   = document.getElementById('menuToggle');

  function openSidebar()  { sidebar?.classList.add('open'); overlay?.classList.add('open'); }
  function closeSidebar() { sidebar?.classList.remove('open'); overlay?.classList.remove('open'); }

  toggle?.addEventListener('click', openSidebar);
  overlay?.addEventListener('click', closeSidebar);

  // ── Auto-dismiss alerts ─────────────────────────────
  document.querySelectorAll('.alert[data-auto-dismiss]').forEach(el => {
    setTimeout(() => el.style.transition = 'opacity .4s', 100);
    setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 400); }, 4000);
  });

  // ── Bar chart renderer ──────────────────────────────
  document.querySelectorAll('[data-bar-chart]').forEach(container => {
    const data  = JSON.parse(container.dataset.barChart);
    const max   = Math.max(...data.map(d => d.v), 1);
    const colors = ['#6c63ff','#7b73ff','#8a83ff','#5955e0','#9990ff','#4f4bce','#b3aeff'];

    container.innerHTML = data.map((d, i) => `
      <div class="bar-col">
        <div class="bar" style="height:${Math.round(d.v / max * 100)}px;background:${colors[i % colors.length]};opacity:.85" title="${d.label}: ${d.formatted}"></div>
        <span class="bar-label">${d.label}</span>
      </div>
    `).join('');
  });

  // ── Confirmation dialogs ────────────────────────────
  document.querySelectorAll('[data-confirm]').forEach(el => {
    el.addEventListener('click', e => {
      if (!confirm(el.dataset.confirm)) e.preventDefault();
    });
  });

  // ── Theme toggle ─────────────────────────────────────
  const themeBtn = document.getElementById('themeToggle');
  if (themeBtn) {
    const update = () => {
      const light = document.documentElement.classList.contains('light');
      themeBtn.textContent = light ? '☀️' : '🌙';
      themeBtn.title = light ? 'Switch to dark mode' : 'Switch to light mode';
    };
    update();
    themeBtn.addEventListener('click', () => {
      const isLight = document.documentElement.classList.toggle('light');
      localStorage.setItem('nf-theme', isLight ? 'light' : 'dark');
      update();
    });
  }
});

// ── Format EUR ───────────────────────────────────────
function fmtBRL(v) {
  return new Intl.NumberFormat('nl-NL', { style: 'currency', currency: 'EUR' }).format(v);
}
