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
        .then(resp => {
            if (!resp.ok) throw new Error("Error al exportar el archivo");
            return resp.blob();
        })
        .then(blob => {
            // 1. Creamos la URL temporal
            const urlBlob = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = urlBlob;
            link.download = 'clientes.xlsx';
            
            // 2. AGREGAR AL DOM: Engañamos al navegador poniendo el botón invisible en la página
            document.body.appendChild(link);
            
            // 3. Simulamos el clic
            link.click();
            
            // 4. LIMPIEZA: Borramos el botón invisible y liberamos la memoria
            document.body.removeChild(link);
            window.URL.revokeObjectURL(urlBlob);
        })
        .catch(error => {
            console.error("Hubo un problema con la exportación:", error);
            alert("No se pudo descargar el archivo Excel.");
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
    let clienteReciboActual = null;

    function abrirModalOpcionesRecibo(cliente) {
        clienteReciboActual = cliente;
        
        // Título del modal
        document.getElementById('nombreReciboCliente').innerText = `Cliente: ${cliente.nombre}`;
        
        const container = document.getElementById('botonesReciboContainer');
        container.innerHTML = ''; // Limpiamos botones anteriores

        // Siempre mostramos el botón de descargar individual
        container.innerHTML += `<button class="btn btn-primary" onclick="accionRecibo('DESCARGAR')"> Descargar PDF</button>`;

        const tieneTel = cliente.telefono && cliente.telefono.trim() !== "";
        const tieneEmail = cliente.email && cliente.email.trim() !== "";

        if (tieneTel) {
            container.innerHTML += `<button class="btn btn-success" onclick="accionRecibo('WHATSAPP')"> Enviar por WhatsApp</button>`;
        }
        if (tieneEmail) {
            container.innerHTML += `<button class="btn btn-secondary" onclick="accionRecibo('EMAIL')"> Enviar por Email</button>`;
        }
        if (tieneTel && tieneEmail) {
            container.innerHTML += `<button class="btn btn-dark" onclick="accionRecibo('AMBOS')"> Enviar a Ambos</button>`;
        }

        new bootstrap.Modal(document.getElementById('opcionesReciboModal')).show();
    }

    async function accionRecibo(tipo) {
        const id = clienteReciboActual.id;
        const mensajeWpp = `Hola ${clienteReciboActual.nombre} te mando el recibo del pago de internet`;

        switch(tipo) {
            case 'DESCARGAR':
                window.open(`/api/recibos/pdf-individual/${id}`, '_blank');
                break;
                
            case 'WHATSAPP':
                alert("WhatsApp Web no permite adjuntar archivos automáticamente. Se descargará tu recibo y se abrirá el chat. ¡Recuerda adjuntar el archivo manualmente!");
                window.open(`/api/recibos/pdf-individual/${id}`, '_blank'); // Descarga el archivo
                abrirWhatsapp(clienteReciboActual.telefono, mensajeWpp); 
                break;
                
            case 'EMAIL':
                await solicitarEnvioEmail(id);
                break;
                
            case 'AMBOS':
                alert("Se enviará el correo. A continuación se abrirá WhatsApp y se descargará el recibo para que lo adjuntes.");
                await solicitarEnvioEmail(id);
                window.open(`/api/recibos/pdf-individual/${id}`, '_blank');
                abrirWhatsapp(clienteReciboActual.telefono, mensajeWpp);
                break;
        }
    }

    async function solicitarEnvioEmail(id) {
        try {
            const resp = await fetch(`/api/recibos/enviar-email/${id}`, { method: 'POST' });
            if (resp.ok) {
                alert(" Correo enviado exitosamente");
            } else {
                throw new Error("Error del servidor");
            }
        } catch (error) {
            alert(" No se pudo enviar el correo. Asegúrate de tener el backend configurado.");
        }
    }

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

    document.getElementById("medioPago")?.addEventListener("change", validarDniRecomendado);

    document.getElementById("dni")
        ?.addEventListener("input", validarDniRecomendado);

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
// ============================================================
// ESTADÍSTICAS DEL DÍA
// ============================================================
async function actualizarEstadisticasHoy() {
    try {
        const resp = await fetch('/pagos/estadisticas/hoy'); // Asegúrate de que la ruta coincida con tu backend
        if (resp.ok) {
            const data = await resp.json();
            document.getElementById('countCobrosHoy').innerText = data.cantidadCobros || 0;
            document.getElementById('montoCobradoHoy').innerText = `$${data.totalRecaudado || 0}`;
        }
    } catch (error) {
        console.error("Error al cargar las estadísticas de hoy:", error);
    }
}

// Llama a esta función cuando la página cargue por primera vez
document.addEventListener("DOMContentLoaded", () => {
    actualizarEstadisticasHoy();
});
