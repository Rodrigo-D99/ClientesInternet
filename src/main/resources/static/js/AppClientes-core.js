// ============================================================
// CORE - VARIABLES GLOBALES Y FUNCIONES PRINCIPALES
// ============================================================

// Variables globales de estado
let currentPage = 0;
let totalPages = 0;
let currentSort = null;
let currentDir = "asc";

// Modal para clientes
const clienteModal = new bootstrap.Modal(
    document.getElementById('clienteModal')
);

// Inicialización al cargar la página
document.addEventListener("DOMContentLoaded", () => {

    const inputExcel = document.getElementById("inputExcel");
    const btnImport = document.getElementById("btnImport");
    const btnExport = document.getElementById("btnExport");
    const sizePage = document.getElementById("sizePage");
    const filterName = document.getElementById("filterName");
    const filterDeudores = document.getElementById("filterDeudores");

    // ============================================================
    // EXPORTAR EXCEL
    // ============================================================
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

    // ============================================================
    // IMPORTAR EXCEL
    // ============================================================
    btnImport.addEventListener("click", () => inputExcel.click());

    inputExcel.addEventListener("change", async () => {
        if (inputExcel.files.length === 0) return;

        const formData = new FormData();
        formData.append("file", inputExcel.files[0]);

        try {
            const resp = await fetch("/clientes/importar", {
                method: "POST",
                body: formData
            });

            if (!resp.ok) throw new Error("Error al importar");

            const count = await resp.text();
            alert(`Clientes importados correctamente: ${count}`);
            fetchClientes(0); // recarga tabla
        } catch (err) {
            console.error(err);
            alert("Error al importar Excel");
        }
    });  
    // ============================================================
    // CREAR RECIBO
    // ============================================================
    const btnGenerarRecibos = document.getElementById('btnGenerarRecibos');
    
    if(btnGenerarRecibos) {
        btnGenerarRecibos.addEventListener('click', () => {
            // Esto le indicará al navegador que abra una pestaña nueva apuntando al endpoint de Spring Boot
            window.open('/api/recibos/pdf-masivo', '_blank');
        });
    }
    function generarReciboIndividual(id) {
    if (!id) return;
    window.open(`/api/recibos/pdf-individual/${id}`, '_blank');
    }

    window.generarReciboIndividual = generarReciboIndividual;

    // ============================================================
    // FILTROS
    // ============================================================
    filterName.addEventListener('input', () => fetchClientes(0));
    filterDeudores.addEventListener('change', () => fetchClientes(0));

    // ============================================================
    // FORM Y VALIDACIÓN
    // ============================================================
    document.getElementById("formCliente")
        .addEventListener("submit", guardarCliente);

    document.getElementById("medioPago")
        .addEventListener("change", validarDniRecomendado);

    document.getElementById("dni")
        .addEventListener("input", validarDniRecomendado);

    // ============================================================
    // TAMAÑO DE PÁGINA
    // ============================================================
    cargarPreferenciaTamaño();
    
    sizePage.addEventListener('change', () => {
        guardarPreferenciaTamaño();
        fetchClientes(0);
    });

    // ============================================================
    // CARGAR DATOS INICIALES
    // ============================================================
    fetchClientes(0);

});

// ============================================================
// FETCH CLIENTES - Obtener lista con filtros y paginación
// ============================================================
async function fetchClientes(page = 0) {
    const name = filterName.value;
    const deudoresValue = filterDeudores.value;

    const url = new URL('/clientes', window.location.origin);
    url.searchParams.set('page', page);

    const pageSize = getPageSize();
    url.searchParams.set('size', pageSize);

    if (name) url.searchParams.set('nombre', name);
    
    // Solo agregar parámetro deudores si está seleccionado algo diferente de vacío
    if (deudoresValue === "true" || deudoresValue === "false") {
        url.searchParams.set('deudores', deudoresValue);
    }
    
    if (currentSort) {
        url.searchParams.set('sort', currentSort);
        url.searchParams.set('dir', currentDir);
    }

    const resp = await fetch(url);
    const data = await resp.json();

    currentPage = data.number || (data.page ? data.page.number : 0);
    totalPages = data.totalPages || (data.page ? data.page.totalPages : 1);
    renderTable(data.content || data);
    renderPagination();
}

// ============================================================
// ORDENAR POR COLUMNA
// ============================================================
function ordenarPor(campo) {
    if (currentSort === campo) {
        currentDir = currentDir === "asc" ? "desc" : "asc";
    } else {
        currentSort = campo;
        currentDir = "asc";
    }
    actualizarFlechas();
    fetchClientes(currentPage);
}

// ============================================================
// PREFERENCIAS DE TAMAÑO DE PÁGINA
// ============================================================
function guardarPreferenciaTamaño() {
    const sizePageSelect = document.getElementById('sizePage');
    if (sizePageSelect) {
        localStorage.setItem('pageSizePreference', sizePageSelect.value);
    }
}

function cargarPreferenciaTamaño() {
    const savedSize = localStorage.getItem('pageSizePreference');
    const sizePageSelect = document.getElementById('sizePage');

    if (savedSize && sizePageSelect) {
        sizePageSelect.value = savedSize;
    } else if (sizePageSelect) {
        sizePageSelect.value = "50";
        localStorage.setItem('pageSizePreference', "50");
    }
}
