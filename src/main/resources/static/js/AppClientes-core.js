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
    // Variable global para guardar los datos del cliente seleccionado
let clienteReciboActual = null;

// Hacemos la funcion global agregando "window."
window.abrirModalOpcionesRecibo = function(cliente) {
    clienteReciboActual = cliente;
    
    document.getElementById('nombreReciboCliente').innerText = 'Cliente: ' + cliente.nombre;
    
    const container = document.getElementById('botonesReciboContainer');
    container.innerHTML = ''; 

    container.innerHTML += '<button class="btn btn-primary mb-2" onclick="accionRecibo(\'DESCARGAR\')">Descargar PDF</button>';

    const tieneTel = cliente.telefono && cliente.telefono.trim() !== "";
    const tieneEmail = cliente.email && cliente.email.trim() !== "";

    if (tieneTel) {
        container.innerHTML += '<button class="btn btn-success mb-2" onclick="accionRecibo(\'WHATSAPP\')">Enviar por WhatsApp</button>';
    }
    if (tieneEmail) {
        container.innerHTML += '<button class="btn btn-secondary mb-2" onclick="accionRecibo(\'EMAIL\')">Enviar por Email</button>';
    }
    if (tieneTel && tieneEmail) {
        container.innerHTML += '<button class="btn btn-dark mb-2" onclick="accionRecibo(\'AMBOS\')">Enviar a Ambos</button>';
    }

    new bootstrap.Modal(document.getElementById('opcionesReciboModal')).show();
};
// Función para construir el mensaje de WhatsApp usando la estructura real de Cliente y PlanInternet
function generarTextoRecibo(cliente, esAmbos) {
    const hoy = new Date();
    const fechaFormateada = hoy.toLocaleDateString('es-AR');

    // 1. Extracción de datos del Plan anidado
    const plan = cliente.plan || {};
    const nombrePlan = plan.cantidadMB ? `Internet ${plan.cantidadMB}MB` : 'Servicio de Internet';
    
    // Obtención de precios desde la entidad PlanInternet
    const efecVal = plan.precioEfectivo || 0;
    const tranfVal = plan.precioTransferencia || 0;

    const efec = Number(efecVal).toFixed(2);
    const tranf = Number(tranfVal).toFixed(2);

    // 2. Extracción de datos de la entidad Cliente
    const direccion = cliente.direccion || 'N/A';
    
    // Mes de pago actual (YYYY-MM)
    const mesActual = hoy.toISOString().slice(0, 7);
    const pagoMes = cliente.pagoMes || mesActual;

    // Cable TV e Instalación
    const tieneTv = cliente.tieneTV ? "Sí" : "No";
    const instalacionVal = cliente.costoInstalacion || 0;
    const instalacion = Number(instalacionVal).toFixed(2);

    // 3. Construcción del texto formateado para WhatsApp
    let msg = `Hola! te enviamos el \n*RECIBO DE PAGO - ${fechaFormateada}*\n`;
    msg += `*Cliente:* ${cliente.nombre} | *Dir:* ${direccion} | *Pago mes:* ${pagoMes}\n`;
    msg += `*Tiene TV por cable:* ${tieneTv}\n`;


    // Nota adicional si se selecciona la opción AMBOS
    if (esAmbos) {
        msg += `\n\n También te enviamos el comprobante en formato PDF a tu correo electrónico.`;
    }

    msg += `\n\n¡Muchas gracias por pagar este mes!`;

    return msg;
}

// Handler de eventos para los botones del recibo
window.accionRecibo = async function(tipo) {
    const id = clienteReciboActual.id;

    switch(tipo) {
        case 'DESCARGAR':
            window.open('/api/recibos/pdf-individual/' + id, '_blank');
            break;
            
        case 'WHATSAPP': {
            const mensajeWpp = generarTextoRecibo(clienteReciboActual, false);
            abrirWhatsapp(clienteReciboActual.telefono, mensajeWpp);
            break;
        }
            
        case 'EMAIL':
            await solicitarEnvioEmail(id);
            break;
            
        case 'AMBOS': {
            const mensajeWppAmbos = generarTextoRecibo(clienteReciboActual, true);
            // Abrimos WhatsApp primero para prevenir el bloqueo de ventanas emergentes del navegador
            abrirWhatsapp(clienteReciboActual.telefono, mensajeWppAmbos);
            // Ejecutamos la petición asíncrona de envío de correo
            await solicitarEnvioEmail(id);
            break;
        }
    }
};
window.solicitarEnvioEmail = async function(id) {
    try {
        const resp = await fetch('/api/recibos/enviar-email/' + id, { 
            method: 'POST' 
        });

        // Leemos la respuesta como texto para evitar el error de JSON
        const mensaje = await resp.text();

        if (resp.ok) {
            alert(mensaje); 
        } else {
            throw new Error(mensaje);
        }
    } catch (error) {
        alert("No se pudo enviar el correo: " + error.message);
    }
};
// Funcion global para el modal de configuracion de email
window.abrirModalConfigEmail = function() {
    fetch('/api/configuracion/email')
        .then(res => res.text())
        .then(email => {
            document.getElementById('adminEmail').value = email;
            document.getElementById('adminPassword').value = '';
            new bootstrap.Modal(document.getElementById('configEmailModal')).show();
        })
        .catch(err => alert("Error al cargar la configuracion"));
};
//configurar mail
document.getElementById('formConfigEmail').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const email = document.getElementById('adminEmail').value;
    const password = document.getElementById('adminPassword').value;
    console.log("Email a guardar:", email);
    console.log("Password a guardar:", password);
    const payload = { email: email };
    if (password.trim() !== '') {
        payload.password = password;
    }

    try {
        const resp = await fetch('/api/configuracion/email', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (resp.ok) {
            alert('Configuración guardada exitosamente');
            bootstrap.Modal.getInstance(document.getElementById('configEmailModal')).hide();
        } else {
            throw new Error("Error del servidor");
        }
    } catch (error) {
        alert('Error al guardar la configuración');
    }
});
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
