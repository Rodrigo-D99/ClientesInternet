// ============================================================
// FUNCIONES CRUD DE CLIENTES
// ============================================================

let clienteEditandoId = null;

// Mostrar modal para nuevo cliente
function nuevoCliente() {
    clienteEditandoId = null;
    limpiarFormulario();
    actualizarSelectPlanes(); // Carga los planes dinámicos
    document.getElementById("tituloForm").innerText = "Nuevo cliente";
    clienteModal.show();
}

// Obtener tamaño de página del select
function getPageSize() {
    const sizePageSelect = document.getElementById('sizePage');
    return sizePageSelect ? sizePageSelect.value : 50; // Valor por defecto 50
}


// Guardar cliente (crear o editar)
// 1. Modificar guardarCliente (solo guarda datos)[cite: 29]
async function guardarCliente(e) {
    e.preventDefault();
    const cliente = {
        nombre: document.getElementById("nombre").value,
        telefono: document.getElementById("telefono").value,
        direccion: document.getElementById("direccion").value,
        email: document.getElementById("email").value, 
        tieneTV: document.getElementById("tieneTV").checked,
        tieneFibraTV: document.getElementById("tieneFibraTV").checked,
        usuarioFibraTV: document.getElementById("usuarioFibraTV").value || null,
        cantidadMB: document.getElementById("cantidadMB").value !== '' ? Number(document.getElementById("cantidadMB").value) : null,
        deudaInstalacion: document.getElementById("deudaInstalacion").value,
        costoInstalacion: document.getElementById("costoInstalacion").value ? parseFloat(document.getElementById("costoInstalacion").value) : 0,
    };
    
    const method = clienteEditandoId ? "PUT" : "POST";
    const endpoint = clienteEditandoId ? `/clientes/${clienteEditandoId}` : "/clientes";

    try {
        const resp = await fetch(endpoint, {
            method: method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(cliente)
        });

        if (!resp.ok) throw new Error("Error al guardar el cliente en el servidor.");

        const clienteModalEl = bootstrap.Modal.getInstance(document.getElementById('clienteModal'));
        clienteModalEl.hide();
        fetchClientes(currentPage);
    } catch (error) {
        alert("Ocurrió un error: " + error.message);
    }
}

// 2. Modificar editarCliente (solo carga datos del cliente)[cite: 29]
async function editarCliente(id) {
    clienteEditandoId = id;
    const resp = await fetch(`/clientes/${id}`);
    const c = await resp.json();

    document.getElementById("tituloForm").innerText = "Editar cliente";

    document.getElementById("nombre").value = c.nombre ?? "";
    document.getElementById("telefono").value = c.telefono ?? "";
    document.getElementById("direccion").value = c.direccion ?? "";
    document.getElementById("email").value = c.email ?? "";
    document.getElementById("tieneTV").checked = c.tieneTV ?? false;
    document.getElementById("tieneFibraTV").checked = c.tieneFibraTV ?? false;
    document.getElementById("usuarioFibraTV").value = c.usuarioFibraTV ?? "";

    const selectDeuda = document.getElementById("deudaInstalacion");
    const inputCosto = document.getElementById("costoInstalacion");
    selectDeuda.value = c.deudaInstalacion || "NO";
    inputCosto.value = c.costoInstalacion || "";
    inputCosto.disabled = (selectDeuda.value === 'NO');

    await actualizarSelectPlanes();
    const cantidadMBEl = document.getElementById("cantidadMB");
    if (cantidadMBEl) cantidadMBEl.value = c.cantidadMB ?? "";
    const clienteModal = bootstrap.Modal.getInstance(document.getElementById('clienteModal')).show();
}

// 3. Crear las funciones para el nuevo modal de Pago[cite: 29]
function abrirModalPago(idCliente) {
    document.getElementById("formPago").reset();
    document.getElementById("pagoClienteId").value = idCliente;
    new bootstrap.Modal(document.getElementById('pagoModal')).show();
}

async function registrarPago(e) {
    e.preventDefault();
    const clienteId = document.getElementById("pagoClienteId").value;
    
    const pagoReq = {
        monto: parseFloat(document.getElementById("pagoMonto").value),
        medioPago: document.getElementById("pagoMedioPago").value,
        cantidadMeses: parseInt(document.getElementById("pagoCantidadMeses").value) || 1,
        nota: document.getElementById("pagoNota").value || null,
        dniPagador: document.getElementById("pagoDni").value || null
    };
    
    if (!pagoReq.medioPago || isNaN(pagoReq.monto)) {
        alert("Debe seleccionar un Medio de Pago y un Monto válido.");
        return;
    }
    
    try {
        const resp = await fetch(`/pagos/${clienteId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pagoReq)
        });
        
        if (!resp.ok) {
            const errorDelBackend = await resp.text(); 
            throw new Error(`Detalle: ${errorDelBackend}`);
        }
        
        const pagoModalEl = bootstrap.Modal.getInstance(document.getElementById('pagoModal'));
        pagoModalEl.hide();
        fetchClientes(currentPage);
        
    } catch (error) {
        alert("Error al registrar el pago: " + error.message);
    }
}

// Limpiar formulario
function limpiarFormulario() {
    ["nombre", "telefono", "direccion", "email", "monto", "cantidadMeses", "dni", "nota", "medioPago", "usuarioFibraTV",
            "cantidadMB","deudaInstalacion","costoInstalacion"].forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                if (id === "cantidadMeses") {
                    el.value = "1";
                } else {
                    el.value = "";
                }
            }
    });
    // Limpiar checkboxes
    document.getElementById("tieneTV").checked = false;
    document.getElementById("tieneFibraTV").checked = false;
    }
    
    async function ejecutarPagoDirecto(clienteId) {
    const pagoReq = {
        monto: parseFloat(document.getElementById("monto").value),
        medioPago: document.getElementById("medioPago").value,
        cantidadMeses: parseInt(document.getElementById("cantidadMeses").value) || 1,
        nota: document.getElementById("nota").value || null,
        dniPagador: document.getElementById("dni").value || null
    };

    //console.log("PASO 5: Datos del pago que se van a enviar:", pagoReq);

    if (!pagoReq.medioPago) {
        throw new Error("Debe seleccionar un Medio de Pago (Efectivo, Transferencia, etc.) para registrar el monto.");
    }

    const resp = await fetch(`/pagos/${clienteId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(pagoReq)
    });

    if (!resp.ok) {
        const errorDelBackend = await resp.text(); 
        throw new Error(`El pago rebotó en el servidor (Error ${resp.status}). Detalle: ${errorDelBackend}`);
    }
    
    //console.log("PASO 6: ¡Pago registrado con éxito en la base de datos!");
}
// Eliminar cliente


const PASSWORD_SEGURIDAD = "admin123"; 


async function eliminarCliente(id) {
   
    const passwordIngresada = prompt("Seguridad: Ingrese la contraseña para eliminar este cliente:");

   
    if (passwordIngresada !== PASSWORD_SEGURIDAD) {
        alert("❌ Contraseña incorrecta o acción cancelada. No se eliminó el cliente.");
        return;
    }

    // Si la contraseña es correcta, pedimos una última confirmación
    if (!confirm("¿Seguro que deseas eliminar este cliente definitivamente?")) return;

    const resp = await fetch(`/clientes/${id}`, {
        method: "DELETE"
    });

    if (!resp.ok) {
        alert("Error al eliminar el cliente");
        return;
    }

    fetchClientes(currentPage);
}

// Borrar todos los clientes 
async function borrarTodos() {
    const passwordIngresada = prompt("⚠️ ACCIÓN PELIGROSA: Ingrese la contraseña maestra para vaciar la base de datos:");

    
    if (passwordIngresada !== PASSWORD_SEGURIDAD) {
        alert("❌ Contraseña incorrecta o acción cancelada. La base de datos está a salvo.");
        return; 
    }

    if (!confirm("⚠️ ¿ESTÁS SEGURO? Esta acción eliminará a TODOS los clientes y sus registros de forma permanente.")) {
        return;
    }

    if (!confirm("CONFIRMACIÓN FINAL: ¿Realmente deseas vaciar la base de datos de clientes?")) {
        return;
    }

    try {
        const resp = await fetch("/clientes/all", { method: "DELETE" });
        if (resp.ok) {
            alert("Todos los clientes han sido eliminados.");
            fetchClientes(0);
        } else {
            throw new Error("Error al intentar borrar los clientes.");
        }
    } catch (error) {
        alert(error.message);
    }
}
// Cancelar edición
function cancelarEdicion() {
    document.getElementById("formCliente").hidden = true;
    limpiarFormulario();
}


// Validar DNI recomendado según medio de pago
function validarDniRecomendado() {
    const medioPago = document.getElementById("medioPago").value;
    const dni = document.getElementById("dni").value;
    const warning = document.getElementById("dniWarning");

    if (!warning) return; // Por si no existe en el HTML

    if (
        (medioPago === "TRANSFERENCIA" || medioPago === "TARJETA") && 
        (!dni || dni.trim() === "")
    ) {
        warning.style.display = "block";
    } else {
        warning.style.display = "none";
    }
}


// ============================================================
// FUNCIONES DE PLANES (SELECT DINÁMICO)
// ============================================================

async function actualizarSelectPlanes() {
    try {
        console.log("Buscando planes en el servidor...");
        const resp = await fetch('/api/planes');
        
        if (!resp.ok) throw new Error("No se pudieron cargar los planes");
        
        let planes = await resp.json();
        
        // 1. FILTRAR ELEMENTOS NULOS (Esto evita el error del sort)
        planes = planes.filter(p => p !== null && p !== undefined);

        const selectMB = document.getElementById("cantidadMB");
        if (!selectMB) return;

        // Guardamos el valor seleccionado actualmente para no perderlo
        const valorActual = selectMB.value;

        // Limpiar opciones
        selectMB.innerHTML = '<option value="">Seleccione un plan...</option>';

        // 2. ORDENAR (con seguridad por si falta cantidadMB)
        planes.sort((a, b) => (a.cantidadMB || 0) - (b.cantidadMB || 0));

        planes.forEach(plan => {
            const option = document.createElement("option");
            // CAMBIO CLAVE: El value ahora es el ID del plan
            option.value = plan.id; 
            option.textContent = `${plan.cantidadMB} MB`;
            selectMB.appendChild(option);
        });

        // Restaurar valor si existía
        if (valorActual) selectMB.value = valorActual;
        console.log("Planes cargados con éxito.");

    } catch (error) {
        console.error("Error al actualizar el select de planes:", error);
    }

    document.getElementById('deudaInstalacion').addEventListener('change', function() {
        const inputCosto = document.getElementById('costoInstalacion');
        
        if (this.value === 'NO') {
            inputCosto.value = ''; // Limpiamos el valor
            inputCosto.disabled = true; // Lo bloqueamos
        } else {
            inputCosto.disabled = false; // Lo habilitamos si es Básica/Intermedia/Full
        }
    });
}
