const PAGE_SIZE = 50;
let currentPage = 0;
let totalPages = 0;

const clienteModal = new bootstrap.Modal(
    document.getElementById('clienteModal')
);

document.addEventListener("DOMContentLoaded", () => {

    const inputExcel = document.getElementById("inputExcel");
    const btnImport = document.getElementById("btnImport");
    const btnExport = document.getElementById("btnExport");

    const filterName = document.getElementById("filterName");
    const filterDeudores = document.getElementById("filterDeudores");
    // ---------------- Exportar Excel ----------------
    btnExport.addEventListener("click", () => {
    const name = filterName.value;
    const deudores = filterDeudores.value;

    const url = new URL('/clientes/export', window.location.origin);
    if (name) url.searchParams.set('nombre', name);
    if (deudores) url.searchParams.set('deudores', deudores);

    fetch(url)
        .then(resp => resp.blob())
        .then(blob => {
            const link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download = 'clientes.xlsx';
            link.click();
        });
    });
    // ---------------- Importar Excel ----------------
    btnImport.addEventListener("click", () => inputExcel.click());

    inputExcel.addEventListener("change", async () => {
    if (inputExcel.files.length === 0) return;

    fileName.value = inputExcel.files[0].name;

    const formData = new FormData();
    formData.append("file", inputExcel.files[0]);

    try {
        const resp = await fetch("/clientes/importar", {
            method: "POST",
            body: formData
        });

        if (!resp.ok) throw new Error("Error al importar");

        const count = await resp.text(); // si tu backend devuelve número de importados
        alert(`Clientes importados correctamente: ${count}`);
        fetchClientes(0); // recarga tabla
    } catch (err) {
        console.error(err);
        alert("Error al importar Excel");
    }
    });

    // ---------------- Filtros ----------------
    filterName.addEventListener('input', () => fetchClientes(0));
    filterDeudores.addEventListener('change', () => fetchClientes(0));

    // Form submit

    document.getElementById("formCliente")
            .addEventListener("submit", guardarCliente);
    // ---------------- Inicial ----------------
    fetchClientes(0); // carga la primera página

    document.getElementById("medioPago")
        .addEventListener("change", validarDniRecomendado);

    document.getElementById("dni")
        .addEventListener("input", validarDniRecomendado);

});

function nuevoCliente() {
    clienteEditandoId = null;
    limpiarFormulario();
    document.getElementById("tituloForm").innerText = "Nuevo cliente";
    clienteModal.show();
}


async function guardarCliente(e) {
    e.preventDefault();

    const cliente = {
        nombre: nombre.value,
        telefono: telefono.value,
        direccion: direccion.value
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

async function crearPagoInicial(clienteId) {

    const pago = {
        monto: Number(document.getElementById("monto").value),
        medioPago: document.getElementById("medioPago").value,
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


async function editarCliente(id) {
    clienteEditandoId = id;

    const resp = await fetch(`/clientes/${id}`);
    const c = await resp.json();

    document.getElementById("tituloForm").innerText = "Editar cliente";

    document.getElementById("nombre").value = c.nombre ?? "";
    document.getElementById("telefono").value = c.telefono ?? "";
    document.getElementById("direccion").value = c.direccion ?? "";

    clienteModal.show();
}
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

function cancelarEdicion() {
    document.getElementById("formCliente").hidden = true;
    limpiarFormulario();
}

function limpiarFormulario() {
    ["nombre", "telefono", "direccion"].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = "";
    });
}
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

// ---------------- Fetch clientes ----------------
async function fetchClientes(page = 0) {
    const name = filterName.value;
    const deudores = filterDeudores.value;

    const url = new URL('/clientes', window.location.origin);
    url.searchParams.set('page', page);
    url.searchParams.set('size', PAGE_SIZE);
    if (name) url.searchParams.set('nombre', name);
    if (deudores) url.searchParams.set('deudores', deudores);

    const resp = await fetch(url);
    const data = await resp.json();

    currentPage = data.number;
    totalPages = data.totalPages;

    renderTable(data.content);
    renderPagination();
}

// ---------------- Render tabla ----------------
function renderTable(clientes) {
    const tbody = document.getElementById("clientesTableBody");
    tbody.innerHTML = '';
    clientes.forEach(c => {
        const dni = (c.medioPago === 'TRANSFERENCIA' || c.medioPago === 'TARJETA')
                    ? (c.dni || '')
                    : '';

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>
                ${c.nombre}
                <span class="badge ms-2"
                      style="background-color: ${c.mesesAdeudados > 0 ? 'red' : 'green'};
                             width:12px; height:12px; display:inline-block; border-radius:50%;"></span>
            </td>
            <td>${c.telefono || ''}</td>
            <td>${c.direccion}</td>
            <td>${c.mesesAdeudados}</td>
            <td>${c.medioPago || ''}</td>
            <td>${dni}</td>
            <td>
                ${c.montoUltimoPago || ""} - ${c.nota || ""}
            </td>


            <td>${c.fechaUltimoPago || ''}</td>
            <td>
                <button class="btn btn-sm btn-warning"
                        onclick="editarCliente(${c.id})">
                    Editar
                </button>
                <button class="btn btn-sm btn-danger"
                        onclick="eliminarCliente(${c.id})">
                    Eliminar
                </button>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

/*async function actualizarNota(id, nota) {
    await fetch(`/pagos/{id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ nota })
    });
}*/

// ---------------- Render paginación ----------------
function renderPagination() {
    const container = document.getElementById("pagination");
    container.innerHTML = '';

    for (let i = 0; i < totalPages; i++) {
        const li = document.createElement('li');
        li.className = 'page-item ' + (i === currentPage ? 'active' : '');
        li.innerHTML = `<a class="page-link" href="#">${i + 1}</a>`;
        li.onclick = (e) => { e.preventDefault(); fetchClientes(i); };
        container.appendChild(li);
    }
}

