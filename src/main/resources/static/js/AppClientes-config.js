// ============================================================
// CONFIGURACIÓN DE PRECIOS Y PLANES (AppClientes-config.js)
// ============================================================

let configModal;

// 1. Abrir el modal y cargar los datos actuales
async function abrirModalConfig() {
    if (!configModal) {
        configModal = new bootstrap.Modal(document.getElementById('configuracionModal'));
    }
    
    await cargarPlanesEnConfig();
    await cargarInstalacionesEnConfig();
    await cargarPrecioFibraTV();
    await cargarPrecioCableTV();
    configModal.show();
}

// 2. HTML para filas de Planes
function crearFilaPlanHTML(plan) {
    if (!plan) plan = { id: '', cantidadMB: '', precioEfectivo: '', precioTransferencia: '' };

    const div = document.createElement('div');
    div.className = 'card mb-2 p-2 bg-light fila-plan';
    div.innerHTML = `
        <input type="hidden" class="plan-id" value="${plan.id || ''}">
        <div class="row g-2 align-items-center">
            <div class="col-3">
                <div class="input-group input-group-sm">
                    <input type="number" class="form-control plan-mb" placeholder="MB" value="${plan.cantidadMB || ''}">
                    <span class="input-group-text">MB</span>
                </div>
            </div>
            <div class="col-4">
                <input type="number" class="form-control form-control-sm plan-efectivo" placeholder="Efectivo $" value="${plan.precioEfectivo || ''}">
            </div>
            <div class="col-4">
                <input type="number" class="form-control form-control-sm plan-transf" placeholder="Transf. $" value="${plan.precioTransferencia || ''}">
            </div>
            <div class="col-1 text-end">
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="this.closest('.fila-plan').remove()">×</button>
            </div>
        </div>
    `;
    return div;
}

// 3. HTML para filas de Instalaciones (Fuerza MAYÚSCULAS)
function crearFilaInstalacionHTML(inst) {
    if (!inst) inst = { nombre: '', precio: '' };

    const div = document.createElement('div');
    div.className = 'card mb-2 p-2 bg-light fila-instalacion';
    div.innerHTML = `
        <div class="row g-2 align-items-center">
            <div class="col-6">
                <input type="text" 
                       class="form-control form-control-sm inst-nombre" 
                       placeholder="Tipo (ej: BASICA)" 
                       value="${(inst.nombre || '').toUpperCase()}" 
                       oninput="this.value = this.value.toUpperCase()" 
                       style="text-transform: uppercase;">
            </div>
            <div class="col-5">
                <input type="number" class="form-control form-control-sm inst-precio" placeholder="Precio $" value="${inst.precio || ''}">
            </div>
            <div class="col-1 text-end">
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="this.closest('.fila-instalacion').remove()">×</button>
            </div>
        </div>
    `;
    return div;
}

// 4. Cargar Planes
async function cargarPlanesEnConfig() {
    try {
        const resp = await fetch('/api/planes');
        let planes = await resp.json();
        const container = document.getElementById('listaPlanesConfig');
        container.innerHTML = '';

        if (Array.isArray(planes) && planes.length > 0) {
            planes.filter(p => p != null).forEach(plan => container.appendChild(crearFilaPlanHTML(plan)));
        } else {
            agregarFilaPlan();
        }
    } catch (e) {
        console.error("Error cargando planes:", e);
    }
}

// 5. Cargar Precios de Instalaciones
async function cargarInstalacionesEnConfig() {
    try {
        const resp = await fetch('/api/configuracion/instalaciones');
        const instalaciones = await resp.json();
        const container = document.getElementById('listaInstalacionesConfig');
        container.innerHTML = '';

        if (Array.isArray(instalaciones) && instalaciones.length > 0) {
            instalaciones.forEach(inst => container.appendChild(crearFilaInstalacionHTML(inst)));
        } else {
            // Filas por defecto si está vacío
            container.appendChild(crearFilaInstalacionHTML({ nombre: 'BASICA', precio: 30000 }));
            container.appendChild(crearFilaInstalacionHTML({ nombre: 'INTERMEDIA', precio: 40000 }));
            container.appendChild(crearFilaInstalacionHTML({ nombre: 'FULL', precio: 50000 }));
        }
    } catch (e) {
        console.error("Error cargando instalaciones:", e);
    }
}

function agregarFilaPlan() {
    document.getElementById('listaPlanesConfig').appendChild(crearFilaPlanHTML(null));
}

function agregarFilaInstalacion() {
    document.getElementById('listaInstalacionesConfig').appendChild(crearFilaInstalacionHTML(null));
}

async function cargarPrecioFibraTV() {
    try {
        const resp = await fetch('/api/configuracion/fibratv'); 
        if (resp.ok) {
            const config = await resp.json();
            document.getElementById('inputPrecioFibraTV').value = config.valorDouble ?? config.valor ?? 0;
        }
    } catch (e) { console.warn("Error cargando Fibra TV:", e); }
}

async function cargarPrecioCableTV() {
    try {
        const resp = await fetch('/api/configuracion/cabletv'); 
        if (resp.ok) {
            const config = await resp.json();
            document.getElementById('inputPrecioCableTV').value = config.valorDouble ?? config.valor ?? 0;
        }
    } catch (e) { console.warn("Error cargando Cable TV:", e); }
}

// 6. Guardar TODA la Configuración
async function guardarConfiguracionGeneral() {
    // A. Recolectar Planes
    const filasPlanes = document.querySelectorAll('.fila-plan');
    const planesNuevos = [];
    filasPlanes.forEach(fila => {
        const idInput = fila.querySelector('.plan-id');
        const id = idInput ? idInput.value : ''; 
        const mb = parseInt(fila.querySelector('.plan-mb').value);
        const ef = parseFloat(fila.querySelector('.plan-efectivo').value);
        const tr = parseFloat(fila.querySelector('.plan-transf').value);

        if (mb > 0 && !isNaN(mb)) {
            const planObj = { cantidadMB: mb, precioEfectivo: isNaN(ef) ? 0 : ef, precioTransferencia: isNaN(tr) ? 0 : tr };
            if (id && id.trim() !== "" && id !== "null") planObj.id = parseInt(id);
            planesNuevos.push(planObj);
        }
    });

    // B. Recolectar Instalaciones (Siempre en Mayúsculas)
    const filasInst = document.querySelectorAll('.fila-instalacion');
    const instalacionesNuevas = [];
    filasInst.forEach(fila => {
        const nombre = fila.querySelector('.inst-nombre').value.trim().toUpperCase();
        const precio = parseFloat(fila.querySelector('.inst-precio').value);

        if (nombre !== '' && !isNaN(precio)) {
            instalacionesNuevas.push({ nombre, precio });
        }
    });

    try {
        // Guardar Planes
        await fetch('/api/planes/sincronizar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(planesNuevos)
        });

        // Guardar Instalaciones
        await fetch('/api/configuracion/instalaciones', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(instalacionesNuevas)
        });

        // Guardar Servicios TV
        const precioTV = document.getElementById('inputPrecioFibraTV').value || '0';
        await fetch(`/api/configuracion/fibratv?precio=${precioTV}`, { method: 'POST' });

        const precioCableTV = document.getElementById('inputPrecioCableTV').value || '0';
        await fetch(`/api/configuracion/cabletv?precio=${precioCableTV}`, { method: 'POST' });

        alert("¡Configuración guardada exitosamente!");
        if (configModal) configModal.hide();
        window.location.reload();

    } catch (e) {
        console.error("Error guardando datos:", e);
        alert("Ocurrió un error al guardar los cambios.");
    }
}