// Tableros de Pedidos (caja) y Cocina. Consultan la API cada pocos segundos con fetch
// y se vuelven a pintar sin recargar la página. Los botones que aparecen salen de las
// banderas que manda el servidor (canPay, cancellable, canDeliver, canTake, canMarkReady):
// el frontend no decide las reglas, y el backend las vuelve a validar al recibir la acción.
(() => {
    const REFRESH_MS = 7000;
    const root = document.getElementById('live-board');
    const mode = root.dataset.mode; // 'caja' | 'cocina'
    const message = document.getElementById('board-message');
    const lastUpdate = document.getElementById('last-update');
    const source = mode === 'cocina' ? '/api/orders/kitchen' : '/api/orders/active';
    let busy = false;

    // Botones de cada tablero: [bandera, texto, clase, endpoint, cuerpo, confirmación]
    const ACTIONS = mode === 'cocina'
        ? [
            ['canTake', 'Tomar', 'primary', 'take'],
            ['canMarkReady', 'Listo', 'primary', 'ready']
        ]
        : [
            ['canPay', 'Cobrar', 'primary', 'payment', { paymentStatus: 'PAGADO' }],
            ['cancellable', 'Cancelar', 'danger', 'cancel', null, '¿Cancelar este pedido?'],
            ['canDeliver', 'Entregar', 'primary', 'deliver']
        ];

    // Token CSRF que pone Thymeleaf en <head>: Spring Security lo exige en POST/PATCH.
    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]');
        const header = document.querySelector('meta[name="_csrf_header"]');
        return token && header ? { [header.content]: token.content } : {};
    }

    const money = value => '$' + Number(value).toLocaleString('es-CO', { maximumFractionDigits: 0 });

    // La API devuelve una unidad por ítem: aquí se agrupan las iguales → "3 × Latte · Grande (...)".
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

    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function showMessage(text, type) {
        message.textContent = text;
        message.className = 'alert ' + type;
        message.hidden = false;
    }

    function renderCard(order) {
        const card = el('article', 'order-card');
        const header = el('header');
        header.append(el('strong', 'order-number', order.orderNumber), el('span', 'customer', order.customerName));
        card.append(header);

        const list = el('ul', 'items');
        groupItems(order.items).forEach(({ item, quantity }) => {
            const li = el('li');
            li.append(el('strong', 'qty', quantity + ' ×'), document.createTextNode(' ' + item.productName));
            if (item.size) li.append(document.createTextNode(' · ' + item.size));
            if (item.modifiers.length) li.append(el('span', 'mods', item.modifiers.join(', ')));
            list.append(li);
        });
        card.append(list);

        if (mode === 'caja') {
            const total = el('p', 'total', 'Total ' + money(order.total) + ' ');
            total.append(el('span', 'badge ' + (order.paid ? 'paid' : 'pending'), order.paymentStatusLabel));
            card.append(total);
            if (order.status === 'RECIBIDO' && order.paid) {
                card.append(el('p', 'hint', 'Pagado: esperando a que cocina lo tome.'));
            }
        }

        const actions = el('div', 'actions');
        ACTIONS.forEach(([flag, label, style, endpoint, body, question]) => {
            if (!order[flag]) return;
            const button = el('button', 'btn ' + style, label);
            button.type = 'button';
            button.addEventListener('click', () => runAction(order, endpoint, body, question, button));
            actions.append(button);
        });
        if (actions.children.length) card.append(actions);
        return card;
    }

    function render(orders) {
        root.querySelectorAll('.column').forEach(column => {
            const inColumn = orders.filter(o => o.status === column.dataset.status);
            column.querySelector('.count').textContent = inColumn.length;
            const cards = column.querySelector('.cards');
            cards.replaceChildren();
            if (inColumn.length === 0) cards.append(el('p', 'empty', 'Sin pedidos'));
            inColumn.forEach(order => cards.append(renderCard(order)));
        });
        lastUpdate.textContent = 'Actualizado ' + new Date().toLocaleTimeString('es-CO');
    }

    async function refresh() {
        if (busy) return;
        try {
            const response = await fetch(source, { headers: { Accept: 'application/json' } });
            if (response.status === 401) {          // la sesión venció
                window.location.href = '/login';
                return;
            }
            if (!response.ok) throw new Error('HTTP ' + response.status);
            render(await response.json());
        } catch (e) {
            lastUpdate.textContent = 'Sin conexión con el servidor; reintentando…';
        }
    }

    async function runAction(order, endpoint, body, question, button) {
        if (question && !window.confirm(question + ' ' + order.orderNumber + ' · ' + order.customerName)) return;
        busy = true;
        button.disabled = true;
        try {
            const options = { method: 'PATCH', headers: Object.assign({ Accept: 'application/json' }, csrfHeaders()) };
            if (body) {
                options.headers['Content-Type'] = 'application/json';
                options.body = JSON.stringify(body);
            }
            const response = await fetch('/api/orders/' + order.id + '/' + endpoint, options);
            if (response.status === 401) {
                window.location.href = '/login';
                return;
            }
            const data = await response.json().catch(() => ({}));
            if (response.ok) {
                showMessage('Pedido ' + data.orderNumber + ': ' + data.statusLabel + ' · ' + data.paymentStatusLabel + '.', 'success');
            } else {
                // Por ejemplo, otra persona ya cambió el pedido: el servidor responde 400/409 con el motivo.
                showMessage(data.message || 'No se pudo completar la acción (error ' + response.status + ').', 'error');
            }
        } catch (e) {
            showMessage('No hay conexión con el servidor.', 'error');
        } finally {
            busy = false;
            refresh();
        }
    }

    refresh();
    setInterval(() => { if (!document.hidden) refresh(); }, REFRESH_MS);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
})();
