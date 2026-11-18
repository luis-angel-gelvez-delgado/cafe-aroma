// Utilidades compartidas: token CSRF, formato de pesos, ventana emergente (modal)
// y el detalle de un pedido. Lo usan Tomar pedido (resumen y cobro) e Historial (Ver detalle).
window.CafeAroma = (() => {
    // Token CSRF que pone Thymeleaf en <head>: Spring Security lo exige en POST/PATCH.
    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        return token && header ? { [header.content]: token.content } : {};
    }

    const money = value => '$' + Number(value).toLocaleString('es-CO', { maximumFractionDigits: 0 });

    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    // La API devuelve una unidad por ítem: aquí se agrupan las iguales → "3 × Latte · Grande".
    function groupItems(items) {
        const groups = [];
        items.forEach(item => {
            const key = item.productName + '|' + item.size + '|' + item.modifiers.join(',');
            const found = groups.find(g => g.key === key);
            if (found) found.quantity++;
            else groups.push({ key, item, quantity: 1 });
        });
        return groups;
    }

    const STATUS_CLASS = {
        RECIBIDO: 'status-recibido', EN_PREPARACION: 'status-en_preparacion',
        LISTO_PARA_RECOGER: 'status-listo_para_recoger', ENTREGADO: 'status-entregado', CANCELADO: 'status-cancelado'
    };

    /** Detalle de un pedido: estado, pago, fecha, ítems con cantidad, precio unitario y subtotal, y total. */
    function orderDetail(order) {
        const box = el('div', 'order-detail');

        const meta = el('p', 'detail-meta');
        meta.append(el('span', 'badge ' + (STATUS_CLASS[order.status] || ''), order.statusLabel), ' ',
            el('span', 'badge ' + (order.paid ? 'paid' : 'pending'), order.paymentStatusLabel));
        if (order.createdAt) {
            meta.append(el('span', 'muted', ' · ' + new Date(order.createdAt).toLocaleString('es-CO',
                { dateStyle: 'medium', timeStyle: 'short' })));
        }
        box.append(meta);

        const table = el('table', 'detail-table');
        const head = el('tr');
        ['Cant.', 'Producto', 'Precio u.', 'Subtotal'].forEach(h => head.append(el('th', null, h)));
        const thead = el('thead');
        thead.append(head);
        table.append(thead);
        const body = el('tbody');
        groupItems(order.items).forEach(({ item, quantity }) => {
            const row = el('tr');
            const product = el('td');
            product.append(el('strong', null, item.productName));
            if (item.size) product.append(document.createTextNode(' · ' + item.size));
            if (item.modifiers.length) product.append(el('span', 'mods', item.modifiers.join(', ')));
            row.append(el('td', 'num', quantity + ' ×'), product,
                el('td', 'num', money(item.price)), el('td', 'num', money(Number(item.price) * quantity)));
            body.append(row);
        });
        table.append(body);
        box.append(table);

        const total = el('p', 'detail-total', 'Total ');
        total.append(el('strong', null, money(order.total)));
        box.append(total);
        return box;
    }

    // ---------- ventana emergente ----------
    let onClose = null;

    function modalParts() {
        return {
            root: document.getElementById('modal'),
            title: document.getElementById('modal-title'),
            body: document.getElementById('modal-body'),
            actions: document.getElementById('modal-actions')
        };
    }

    /**
     * Abre la ventana. actions: [{ label, style, onClick }]. onDismiss se llama al cerrar con ✕, Escape o fuera.
     */
    function openModal({ title, content, actions = [], onDismiss = null }) {
        const m = modalParts();
        m.title.textContent = title;
        m.body.replaceChildren(...[].concat(content));
        m.actions.replaceChildren();
        actions.forEach(a => {
            const button = el('button', 'btn ' + (a.style || 'secondary'), a.label);
            button.type = 'button';
            button.addEventListener('click', () => a.onClick(button));
            m.actions.append(button);
        });
        onClose = onDismiss;
        m.root.hidden = false;
        const first = m.actions.querySelector('button') || m.root.querySelector('.modal-close');
        if (first) first.focus();
    }

    function closeModal() {
        modalParts().root.hidden = true;
        onClose = null;
    }

    function dismiss() {
        const callback = onClose;
        closeModal();
        if (callback) callback();
    }

    document.addEventListener('DOMContentLoaded', () => {
        const root = document.getElementById('modal');
        if (!root) return;
        root.querySelector('.modal-close').addEventListener('click', dismiss);
        root.addEventListener('click', e => { if (e.target === root) dismiss(); });
        document.addEventListener('keydown', e => { if (e.key === 'Escape' && !root.hidden) dismiss(); });
    });

    return { csrfHeaders, money, el, groupItems, orderDetail, openModal, closeModal };
})();
