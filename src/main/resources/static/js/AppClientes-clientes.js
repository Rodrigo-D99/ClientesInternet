// ============================================================
// FUNCIONES CRUD DE CLIENTES (AppClientes-clientes.js)
// ============================================================

let clienteEditandoId = null;
const PASSWORD_SEGURIDAD = "admin123"; 

// Mostrar modal para nuevo cliente
function nuevoCliente() {
    clienteEditandoId = null;
    limpiarFormulario();
    actualizarSelectPlanes();
    document.getElementById("tituloForm").innerText = "Nuevo cliente";
    clienteModal.show();
}

// Obtener tamaño de página del select
function getPageSize() {
    const sizePageSelect = document.getElementById('sizePage');
    return sizePageSelect ? sizePageSelect.value : 50;
}

// Guardar cliente (crear o editar)
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

// Cargar datos de cliente para edición
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
    bootstrap.Modal.getInstance(document.getElementById('clienteModal')).show();
}

// Limpiar formulario de cliente
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
    document.getElementById("tieneTV").checked = false;
    document.getElementById("tieneFibraTV").checked = false;
}

// Eliminar un cliente
async function eliminarCliente(id) {
    const passwordIngresada = prompt("Seguridad: Ingrese la contraseña para eliminar este cliente:");

    if (passwordIngresada !== PASSWORD_SEGURIDAD) {
        alert("❌ Contraseña incorrecta o acción cancelada. No se eliminó el cliente.");
        return;
    }

    if (!confirm("¿Seguro que deseas eliminar este cliente definitivamente?")) return;

    const resp = await fetch(`/clientes/${id}`, { method: "DELETE" });

    if (!resp.ok) {
        alert("Error al eliminar el cliente");
        return;
    }

    fetchClientes(currentPage);
}

// Vaciar base de datos
async function borrarTodos() {
    const passwordIngresada = prompt("⚠️ ACCIÓN PELIGROSA: Ingrese la contraseña maestra para vaciar la base de datos:");

    if (passwordIngresada !== PASSWORD_SEGURIDAD) {
        alert("❌ Contraseña incorrecta o acción cancelada.");
        return; 
    }

    if (!confirm("⚠️ ¿ESTÁS SEGURO? Esta acción eliminará a TODOS los clientes.")) return;
    if (!confirm("CONFIRMACIÓN FINAL: ¿Realmente deseas vaciar la base de datos?")) return;

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

// Cargar select dinámico de planes
async function actualizarSelectPlanes() {
    try {
        const resp = await fetch('/api/planes');
        if (!resp.ok) throw new Error("No se pudieron cargar los planes");
        
        let planes = await resp.json();
        planes = planes.filter(p => p !== null && p !== undefined);

        const selectMB = document.getElementById("cantidadMB");
        if (!selectMB) return;

        const valorActual = selectMB.value;
        selectMB.innerHTML = '<option value="">Seleccione un plan...</option>';

        planes.sort((a, b) => (a.cantidadMB || 0) - (b.cantidadMB || 0));

        planes.forEach(plan => {
            const option = document.createElement("option");
            option.value = plan.id; 
            option.textContent = `${plan.cantidadMB} MB`;
            selectMB.appendChild(option);
        });

        if (valorActual) selectMB.value = valorActual;

    } catch (error) {
        console.error("Error al actualizar el select de planes:", error);
    }

    document.getElementById('deudaInstalacion')?.addEventListener('change', function() {
        const inputCosto = document.getElementById('costoInstalacion');
        if (inputCosto) {
            if (this.value === 'NO') {
                inputCosto.value = '';
                inputCosto.disabled = true;
            } else {
                inputCosto.disabled = false;
            }
        }
    });
}

// Modal informativo de cliente
function verInfoCliente(cliente) {
    document.getElementById("infoNombre").innerText = cliente.nombre || 'Sin Nombre';
    document.getElementById("infoDni").innerText = cliente.dni || 'No registrado';
    document.getElementById("infoMedioPago").innerText = cliente.medioPago || 'Sin medio registrado';
    document.getElementById("infoMonto").innerText = cliente.montoUltimoPago ? `$ ${cliente.montoUltimoPago}` : '-';
    document.getElementById("infoNota").innerText = cliente.nota || 'Sin notas adicionales registradas.';

    const modal = new bootstrap.Modal(document.getElementById('infoClienteModal'));
    modal.show();
}

// Variable global para mantener los precios de instalaciones cargados
let mapaPreciosInstalaciones = {};

// Cargar precios de instalaciones desde la API o LocalStorage
async function cargarPreciosInstalaciones() {
    try {
        const resp = await fetch('/api/configuracion/instalaciones');
        if (resp.ok) {
            const instalaciones = await resp.json();
            mapaPreciosInstalaciones = {};
            instalaciones.forEach(item => {
                if (item.nombre) {
                    mapaPreciosInstalaciones[item.nombre.toUpperCase()] = item.precio;
                }
            });
        }
    } catch (e) {
        console.error("Error al cargar precios de instalaciones:", e);
    }
}

// Escuchador para autocompletar el precio según la opción seleccionada
document.addEventListener("DOMContentLoaded", () => {
    cargarPreciosInstalaciones();

    const selectDeuda = document.getElementById('deudaInstalacion');
    const inputCosto = document.getElementById('costoInstalacion');

    if (selectDeuda && inputCosto) {
        selectDeuda.addEventListener('change', function() {
            const seleccion = this.value.toUpperCase();

            if (seleccion === 'NO') {
                inputCosto.value = '';
                inputCosto.disabled = true;
            } else {
                inputCosto.disabled = false;
                // Asigna el precio si existe en el mapa de precios
                if (mapaPreciosInstalaciones[seleccion] !== undefined) {
                    inputCosto.value = mapaPreciosInstalaciones[seleccion];
                }
            }
        });
    }
});