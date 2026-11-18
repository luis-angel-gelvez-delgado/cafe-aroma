// Tomar pedido como kiosko: Categorías → Productos → Opciones → carrito.
// El menú (tamaños y adiciones válidos por categoría, con sus recargos) viene de GET /api/products.
// El precio que se ve aquí es solo una vista previa: el que manda es el que calcula el servidor
// al registrar el pedido (POST /api/orders), que además vuelve a validar todas las reglas.
(() => {
    const MAX_QTY = 20;
    const CATEGORY_ICONS = { BEBIDA_CALIENTE: '☕', BEBIDA_FRIA: '🧊', PANADERIA: '🥐' };
    // Panadería: "Para llevar" y "Consumir en el local" se excluyen entre sí.
    const EXCLUSIVE = [['PARA_LLEVAR', 'CONSUMIR_EN_LOCAL']];

    const $ = id => document.getElementById(id);
    const backButton = $('kiosk-back');
    const toast = $('kiosk-toast');
    const cartList = $('cart-items');
    const nameInput = $('customer-name');
    const submitButton = $('submit-order');
    const errorBox = $('order-error');

    let products = [];
    let categories = [];          // [{ code, label }] en el orden del menú
    let current = { screen: 'categories', category: null, product: null };
    let selection = { size: null, modifiers: [], quantity: 1 };
    const cart = [];

    // ---------- utilidades ----------

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

    function tile(title, subtitle, onClick, icon) {
        const button = el('button', 'tile');
        button.type = 'button';
        if (icon) button.append(el('span', 'tile-icon', icon));
        button.append(el('span', 'tile-title', title));
        if (subtitle) button.append(el('span', 'tile-sub', subtitle));
        button.addEventListener('click', onClick);
        return button;
    }

    // ---------- navegación entre pasos ----------

    function show(screen) {
        current.screen = screen;
        document.querySelectorAll('.kiosk-screen').forEach(s => { s.hidden = s.dataset.screen !== screen; });
        const order = ['categories', 'products', 'options'];
        document.querySelectorAll('#kiosk-steps li').forEach(li => {
            const index = order.indexOf(li.dataset.step);
            li.classList.toggle('done', index < order.indexOf(screen));
            li.classList.toggle('current', li.dataset.step === screen);
        });
        backButton.hidden = screen === 'categories';
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    backButton.addEventListener('click', () => {
        if (current.screen === 'options') show('products');
        else if (current.screen === 'products') show('categories');
    });

    // ---------- paso 1: categorías ----------

    function renderCategories() {
        const list = $('category-list');
        list.replaceChildren();
        categories.forEach(category => {
            list.append(tile(category.label, null, () => openCategory(category), CATEGORY_ICONS[category.code]));
        });
    }

    // ---------- paso 2: productos de la categoría ----------

    function openCategory(category) {
        current.category = category;
        $('products-title').textContent = category.label;
        const list = $('product-list');
        list.replaceChildren();
        products.filter(p => p.category === category.code).forEach(product => {
            list.append(tile(product.name, 'Desde ' + money(product.basePrice), () => openProduct(product)));
        });
        show('products');
    }

    // ---------- paso 3: opciones del producto ----------

    function openProduct(product) {
        current.product = product;
        selection = {
            size: product.sizes.length ? product.sizes[0].code : null,   // por defecto, el más pequeño
            modifiers: [],
            quantity: 1
        };
        $('options-title').textContent = product.name;
        $('size-group').hidden = product.sizes.length === 0;
        $('modifier-group').hidden = product.modifiers.length === 0;
        renderOptions();
        show('options');
    }

    function choice(option, selected, onClick) {
        const button = el('button', 'choice' + (selected ? ' selected' : ''));
        button.type = 'button';
        button.setAttribute('aria-pressed', String(selected));
        button.append(el('span', null, option.label));
        if (Number(option.surcharge) > 0) button.append(el('span', 'choice-extra', '+' + money(option.surcharge)));
        button.addEventListener('click', onClick);
        return button;
    }

    function renderOptions() {
        const product = current.product;

        const sizes = $('size-list');
        sizes.replaceChildren();
        product.sizes.forEach(size => {
            sizes.append(choice(size, selection.size === size.code, () => {
                selection.size = size.code;
                renderOptions();
            }));
        });

        const modifiers = $('modifier-list');
        modifiers.replaceChildren();
        product.modifiers.forEach(modifier => {
            const selected = selection.modifiers.includes(modifier.code);
            modifiers.append(choice(modifier, selected, () => toggleModifier(modifier.code)));
        });

        $('qty-value').textContent = selection.quantity;
        $('qty-minus').disabled = selection.quantity <= 1;
        $('qty-plus').disabled = selection.quantity >= MAX_QTY;

        const unit = unitPrice(product, selection);
        $('unit-price').textContent = money(unit) + ' c/u';
        $('line-price').textContent = money(unit * selection.quantity);
    }

    function toggleModifier(code) {
        if (selection.modifiers.includes(code)) {
            selection.modifiers = selection.modifiers.filter(m => m !== code);
        } else {
            const rivals = EXCLUSIVE.filter(group => group.includes(code)).flat().filter(m => m !== code);
            selection.modifiers = selection.modifiers.filter(m => !rivals.includes(m)).concat(code);
        }
        renderOptions();
    }

    // Vista previa con los mismos recargos que envía el servidor en /api/products.
    function unitPrice(product, sel) {
        let price = Number(product.basePrice);
        const size = product.sizes.find(s => s.code === sel.size);
        if (size) price += Number(size.surcharge);
        product.modifiers.filter(m => sel.modifiers.includes(m.code)).forEach(m => { price += Number(m.surcharge); });
        return price;
    }

    $('qty-minus').addEventListener('click', () => {
        selection.quantity = Math.max(1, selection.quantity - 1);
        renderOptions();
    });
    $('qty-plus').addEventListener('click', () => {
        selection.quantity = Math.min(MAX_QTY, selection.quantity + 1);
        renderOptions();
    });

    $('add-to-order').addEventListener('click', () => {
        const product = current.product;
        const size = product.sizes.find(s => s.code === selection.size);
        // Las adiciones se guardan en el orden del menú, así dos líneas iguales se reconocen.
        const chosen = product.modifiers.filter(m => selection.modifiers.includes(m.code));
        const item = {
            productId: product.id,
            productName: product.name,
            size: size ? size.code : null,
            sizeLabel: size ? size.label : null,
            modifiers: chosen.map(m => m.code),
            modifierLabels: chosen.map(m => m.label),
            unitPrice: unitPrice(product, selection),
            quantity: selection.quantity
        };
        // Si ya hay una línea igual (mismo producto, tamaño y adiciones), solo suma la cantidad.
        const key = i => i.productId + '|' + i.size + '|' + i.modifiers.join(',');
        const existing = cart.find(i => key(i) === key(item));
        if (existing) {
            existing.quantity = Math.min(MAX_QTY, existing.quantity + item.quantity);
        } else {
            cart.push(item);
        }
        renderCart();
        showToast('Agregado: ' + describe(item));
        show('categories');
    });

    function showToast(text) {
        toast.textContent = text;
        toast.hidden = false;
        clearTimeout(showToast.timer);
        showToast.timer = setTimeout(() => { toast.hidden = true; }, 3000);
    }

    // ---------- carrito ----------

    function describe(item) {
        let text = item.quantity + ' × ' + item.productName;
        if (item.sizeLabel) text += ' · ' + item.sizeLabel;
        if (item.modifierLabels.length) text += ' (' + item.modifierLabels.join(', ') + ')';
        return text;
    }

    function renderCart() {
        cartList.replaceChildren();
        const units = cart.reduce((sum, i) => sum + i.quantity, 0);
        const total = cart.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0);
        $('cart-total').textContent = money(total);
        $('cart-count').textContent = units;
        $('cart-fab').hidden = units === 0;

        if (cart.length === 0) {
            cartList.append(el('li', 'empty', 'Aún no hay productos.'));
            return;
        }
        cart.forEach((item, index) => {
            const li = el('li');
            const text = el('span', null, describe(item));
            const price = el('span', 'cart-line-price', money(item.unitPrice * item.quantity));
            const remove = el('button', 'btn link', 'Quitar');
            remove.type = 'button';
            remove.addEventListener('click', () => {
                cart.splice(index, 1);
                renderCart();
            });
            li.append(text, price, remove);
            cartList.append(li);
        });
    }

    function showError(text) {
        errorBox.textContent = text;
        errorBox.hidden = false;
    }

    submitButton.addEventListener('click', async () => {
        errorBox.hidden = true;
        // Se envía aunque falten datos: el backend debe rechazarlo con su mensaje (regla del reto).
        const body = {
            customerName: nameInput.value,
            // Cada unidad se envía como un ítem: el servidor calcula el precio de cada una.
            items: cart.flatMap(({ productId, size, modifiers, quantity }) =>
                Array.from({ length: quantity }, () => ({ productId, size, modifiers })))
        };
        submitButton.disabled = true;
        try {
            const response = await fetch('/api/orders', {
                method: 'POST',
                headers: Object.assign({ 'Content-Type': 'application/json' }, csrfHeaders()),
                body: JSON.stringify(body)
            });
            if (response.status === 401) {
                window.location.href = '/login';
                return;
            }
            const data = await response.json().catch(() => ({}));
            if (!response.ok) {
                showError(data.message || 'No se pudo registrar el pedido (error ' + response.status + ').');
                return;
            }
            window.location.href = '/pedido?nuevo=' + data.id;
        } catch (e) {
            showError('No hay conexión con el servidor.');
        } finally {
            submitButton.disabled = false;
        }
    });

    // ---------- arranque ----------

    async function loadMenu() {
        try {
            const response = await fetch('/api/products', { headers: { Accept: 'application/json' } });
            if (response.status === 401) {
                window.location.href = '/login';
                return;
            }
            if (!response.ok) throw new Error('HTTP ' + response.status);
            products = await response.json();
            products.forEach(p => {
                if (!categories.some(c => c.code === p.category)) {
                    categories.push({ code: p.category, label: p.categoryLabel });
                }
            });
            renderCategories();
        } catch (e) {
            $('category-list').replaceChildren(el('p', 'alert error', 'No se pudo cargar el menú. Recarga la página.'));
        }
    }

    renderCart();
    show('categories');
    loadMenu();
})();
