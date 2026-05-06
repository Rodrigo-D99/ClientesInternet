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
async function guardarCliente(e) {
    e.preventDefault();

    const cliente = {
        nombre: document.getElementById("nombre").value,
        telefono: document.getElementById("telefono").value,
        direccion: document.getElementById("direccion").value,
        tieneTV: document.getElementById("tieneTV").checked,
        tieneFibraTV: document.getElementById("tieneFibraTV").checked,
        usuarioFibraTV: document.getElementById("usuarioFibraTV").value || null,
        dni: document.getElementById("dni").value || null,
        cantidadMB: document.getElementById("cantidadMB").value !== '' ? Number(document.getElementById("cantidadMB").value) : null
    };
    
    const method = clienteEditandoId ? "PUT" : "POST";
    const endpoint = clienteEditandoId
        ? `/clientes/${clienteEditandoId}`
        : "/clientes";

    try {
        const resp = await fetch(endpoint, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(cliente)
        });

        if (!resp.ok) {
            throw new Error("Error al guardar el cliente");
        }

        clienteModal.hide();
        fetchClientes(currentPage);
    } catch (error) {
        alert(error.message);
    }
}

// Editar cliente (cargar datos al modal)
async function editarCliente(id) {
    clienteEditandoId = id;

    const resp = await fetch(`/clientes/${id}`);
    const c = await resp.json();

    document.getElementById("tituloForm").innerText = "Editar cliente";

    document.getElementById("nombre").value = c.nombre ?? "";
    document.getElementById("telefono").value = c.telefono ?? "";
    document.getElementById("direccion").value = c.direccion ?? "";
    document.getElementById("tieneTV").checked = c.tieneTV ?? false;
    document.getElementById("tieneFibraTV").checked = c.tieneFibraTV ?? false;
    document.getElementById("usuarioFibraTV").value = c.usuarioFibraTV ?? "";

    // Cargar los planes ANTES de asignar el valor
    await actualizarSelectPlanes();
    
    // Poblar campos relacionados a último pago (medio de pago y DNI)
    const medioEl = document.getElementById("medioPago");
    if (medioEl) medioEl.value = c.medioPago ?? "";
    const dniEl = document.getElementById("dni");
    if (dniEl) dniEl.value = c.dni ?? "";
    const montoEl = document.getElementById("monto");
    if (montoEl) montoEl.value = c.montoUltimoPago ?? "";
    const notaEl = document.getElementById("nota");
    if (notaEl) notaEl.value = c.nota ?? "";
    const cantidadMBEl = document.getElementById("cantidadMB");
    if (cantidadMBEl) cantidadMBEl.value = c.cantidadMB ?? "";

    // Validar si mostrar advertencia de DNI según medio de pago
    if (typeof validarDniRecomendado === 'function') validarDniRecomendado();
    clienteModal.show();
}

// Eliminar cliente
async function eliminarCliente(id) {
    if (!confirm("¿Seguro que deseas eliminar este cliente?")) return;

    const resp = await fetch(`/clientes/${id}`, {
        method: "DELETE"
    });

    if (!resp.ok) {
        alert("Error al eliminar el cliente");
        return;
    }

    fetchClientes(currentPage);
}

// Cancelar edición
function cancelarEdicion() {
    document.getElementById("formCliente").hidden = true;
    limpiarFormulario();
}

// Limpiar formulario
function limpiarFormulario() {
    ["nombre", "telefono", "direccion", "monto", "cantidadMeses", "dni", "nota", "medioPago", "usuarioFibraTV", "cantidadMB"].forEach(id => {
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
        
        const planes = await resp.json();
        const selectMB = document.getElementById("cantidadMB");
        
        if (!selectMB) return;

        // Guardamos el valor seleccionado actualmente
        const valorActual = selectMB.value;

        // Limpiar opciones
        selectMB.innerHTML = '<option value="">Seleccione un plan...</option>';

        // Ordenar de menor a mayor MB
        planes.sort((a, b) => a.cantidadMB - b.cantidadMB);

        planes.forEach(plan => {
            const option = document.createElement("option");
            option.value = plan.cantidadMB;
            option.textContent = `${plan.cantidadMB} MB`;
            selectMB.appendChild(option);
        });

        // Restaurar valor si existía
        if (valorActual) selectMB.value = valorActual;
        console.log("Planes cargados con éxito.");

    } catch (error) {
        console.error("Error al actualizar el select de planes:", error);
    }
}