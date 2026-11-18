// Menú hamburguesa (☰): abre y cierra el panel de secciones.
document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('menu-toggle');
    const nav = document.getElementById('main-nav');
    const backdrop = document.getElementById('nav-backdrop');
    if (!toggle || !nav) return;

    const setOpen = open => {
        nav.classList.toggle('open', open);
        backdrop.hidden = !open;
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
    };
    toggle.addEventListener('click', () => setOpen(!nav.classList.contains('open')));
    backdrop.addEventListener('click', () => setOpen(false));
    document.addEventListener('keydown', e => { if (e.key === 'Escape') setOpen(false); });
});
