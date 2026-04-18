// ============================================================
// FUNCIONES CRUD DE CLIENTES
// ============================================================

let clienteEditandoId = null;

// Mostrar modal para nuevo cliente
function nuevoCliente() {
    clienteEditandoId = null;
    limpiarFormulario();
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
        nombre: nombre.value,
        telefono: telefono.value,
        direccion: direccion.value,
        tieneFibraTV: document.getElementById("tieneFibraTV").checked,
        usuarioFibraTV: document.getElementById("usuarioFibraTV").value || null
    };

    const method = clienteEditandoId ? "PUT" : "POST";
    const endpoint = clienteEditandoId
        ? `/clientes/${clienteEditandoId}`
        : "/clientes";

    const respCliente = await fetch(endpoint, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(cliente)
    });

    if (!respCliente.ok) {
        alert("Error al guardar cliente");
        return;
    }

    const clienteCreado = await respCliente.json();

    const monto = document.getElementById("monto").value;
    if (monto && Number(monto) > 0) {
        await crearPagoInicial(clienteCreado.id);
    }

    clienteModal.hide();
    limpiarFormulario();
    fetchClientes(currentPage);
}

// Crear pago inicial para cliente nuevo
async function crearPagoInicial(clienteId) {
    const pago = {
        monto: Number(document.getElementById("monto").value),
        medioPago: document.getElementById("medioPago").value,
        cantidadMeses: Number(document.getElementById("cantidadMeses").value) || 1,
        dniPagador: document.getElementById("dni").value || null,
        nota: document.getElementById("nota").value || null
    };

    const respPago = await fetch(`/pagos/${clienteId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(pago)
    });

    if (!respPago.ok) {
        alert("Cliente creado, pero error registrando pago");
    }
}

// Cargar cliente para editar
async function editarCliente(id) {
    clienteEditandoId = id;

    const resp = await fetch(`/clientes/${id}`);
    const c = await resp.json();

    document.getElementById("tituloForm").innerText = "Editar cliente";

    document.getElementById("nombre").value = c.nombre ?? "";
    document.getElementById("telefono").value = c.telefono ?? "";
    document.getElementById("direccion").value = c.direccion ?? "";
    document.getElementById("tieneFibraTV").checked = c.tieneFibraTV ?? false;
    document.getElementById("usuarioFibraTV").value = c.usuarioFibraTV ?? "";

    clienteModal.show();
}

// Eliminar cliente
async function eliminarCliente(id) {
    const confirmar = confirm("¿Seguro que querés eliminar este cliente?");
    if (!confirmar) return;

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
    ["nombre", "telefono", "direccion", "monto", "cantidadMeses", "dni", "nota", "medioPago", "usuarioFibraTV"].forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            if (id === "cantidadMeses") {
                el.value = "1";
            } else {
                el.value = "";
            }
        }
    });
    // Limpiar checkbox
    document.getElementById("tieneFibraTV").checked = false;
}

// Validar DNI recomendado según medio de pago
function validarDniRecomendado() {
    const medioPago = document.getElementById("medioPago").value;
    const dni = document.getElementById("dni").value;
    const warning = document.getElementById("dniWarning");

    if (
        (medioPago === "TRANSFERENCIA" || medioPago === "TARJETA") &&
        (!dni || dni.trim() === "")
    ) {
        warning.classList.remove("d-none");
        return false;
    } else {
        warning.classList.add("d-none");
        return true;
    }
}
