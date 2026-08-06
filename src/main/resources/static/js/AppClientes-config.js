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
    await cargarPrecioFibraTV();
    configModal.show();
}

// 2. Función para crear el HTML de una fila de plan dinámico
function crearFilaPlanHTML(plan) {
    // BLINDAJE: Si el plan viene nulo o roto, creamos uno vacío por defecto
    if (!plan) {
        plan = { id: '', cantidadMB: '', precioEfectivo: '', precioTransferencia: '' };
    }

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
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="this.closest('.fila-plan').remove()" title="Borrar Plan">×</button>
            </div>
        </div>
    `;
    return div;
}

// 3. Cargar planes existentes desde la base de datos
async function cargarPlanesEnConfig() {
    try {
        const resp = await fetch('/api/planes');
        let planes = await resp.json();
        const container = document.getElementById('listaPlanesConfig');
        container.innerHTML = '';

        // BLINDAJE: Filtramos cualquier elemento null o undefined que venga de la BD
        if (Array.isArray(planes)) {
            planes = planes.filter(p => p !== null && p !== undefined);
        } else {
            planes = [];
        }

        if (planes.length > 0) {
            planes.forEach(plan => {
                container.appendChild(crearFilaPlanHTML(plan));
            });
        } else {
            agregarFilaPlan();
        }
    } catch (e) {
        console.error("Error cargando los planes:", e);
    }
}

// 4. Agregar una fila vacía (Botón "+ Agregar Plan")
function agregarFilaPlan() {
    const container = document.getElementById('listaPlanesConfig');
    container.appendChild(crearFilaPlanHTML(null));
}

// 5. Cargar el precio de Fibra TV
async function cargarPrecioFibraTV() {
    try {
        const resp = await fetch('/api/configuracion/fibratv'); 
        const config = await resp.json();
        document.getElementById('inputPrecioFibraTV').value = config.valor || 0;
    } catch (e) { 
        console.log("Aún no hay precio de Fibra TV registrado."); 
    }
}

// 6. Guardar TODA la configuración
async function guardarConfiguracionGeneral() {
    const filas = document.querySelectorAll('.fila-plan');
    const planesNuevos = [];

    // Recolectar datos de todas las filas de planes
    filas.forEach(fila => {
        const idInput = fila.querySelector('.plan-id');
        const id = idInput ? idInput.value : ''; 
        const mb = parseInt(fila.querySelector('.plan-mb').value);
        const ef = parseFloat(fila.querySelector('.plan-efectivo').value);
        const tr = parseFloat(fila.querySelector('.plan-transf').value);

        if (mb > 0 && !isNaN(mb)) {
            const planObj = { 
                cantidadMB: mb, 
                precioEfectivo: isNaN(ef) ? 0 : ef, 
                precioTransferencia: isNaN(tr) ? 0 : tr 
            };
            
            // BLINDAJE: Solo enviamos el ID si es un número real, si no, va sin ID para que el backend lo cree
            if (id && id !== "null" && id !== "undefined" && id.trim() !== "") {
                planObj.id = parseInt(id);
            }
            
            planesNuevos.push(planObj);
        }
    });

    try {
        // A. Guardar Planes (sincronización dinámica)
        const respPlanes = await fetch('/api/planes/sincronizar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(planesNuevos)
        });

        if (!respPlanes.ok) throw new Error("Error 500 guardando los planes en el servidor.");

        // B. Guardar Fibra TV
        const precioTV = document.getElementById('inputPrecioFibraTV').value;
        await fetch(`/api/configuracion/fibratv?precio=${precioTV}`, { method: 'POST' });

        alert("¡Configuración guardada! Los próximos recibos usarán estos precios.");
        configModal.hide();
        
       window.location.reload();
        
    } catch (e) {
        console.error("Error al guardar:", e);
        alert("Ocurrió un error al guardar la configuración. Revisa la consola.");
    }
}