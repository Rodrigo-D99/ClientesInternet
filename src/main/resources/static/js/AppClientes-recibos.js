// ============================================================
// MÓDULO DE RECIBOS DE PAGO (AppClientes-recibos.js)
// ============================================================

let clienteReciboActual = null;
let configPreciosReciboCache = { precioFibraTV: 0, precioCableTV: 0 };

// Carga segura de precios para recibos evitando llamadas a backend inexistente
async function cargarConfiguracionPreciosRecibo() {
    const inputFibra = parseFloat(document.getElementById("inputPrecioFibraTV")?.value) || 0;
    const inputCable = parseFloat(document.getElementById("inputPrecioCableTV")?.value) || 0;

    configPreciosReciboCache = {
        precioFibraTV: inputFibra,
        precioCableTV: inputCable
    };
}

// Abrir modal con opciones de recibo (PDF, WhatsApp, Email)
window.abrirModalOpcionesRecibo = async function(cliente) {
    clienteReciboActual = cliente;
    await cargarConfiguracionPreciosRecibo();
    
    document.getElementById('nombreReciboCliente').innerText = 'Cliente: ' + cliente.nombre;
    
    const container = document.getElementById('botonesReciboContainer');
    container.innerHTML = ''; 

    container.innerHTML += '<button class="btn btn-primary mb-2 w-100" onclick="accionRecibo(\'DESCARGAR\')">📄 Descargar PDF</button>';

    const tieneTel = cliente.telefono && cliente.telefono.trim() !== "";
    const tieneEmail = cliente.email && cliente.email.trim() !== "";

    if (tieneTel) {
        container.innerHTML += '<button class="btn btn-success mb-2 w-100" onclick="accionRecibo(\'WHATSAPP\')">💬 Enviar por WhatsApp</button>';
    }
    if (tieneEmail) {
        container.innerHTML += '<button class="btn btn-secondary mb-2 w-100" onclick="accionRecibo(\'EMAIL\')">✉️ Enviar por Email</button>';
    }
    if (tieneTel && tieneEmail) {
        container.innerHTML += '<button class="btn btn-dark mb-2 w-100" onclick="accionRecibo(\'AMBOS\')">📲 Enviar a Ambos</button>';
    }

    new bootstrap.Modal(document.getElementById('opcionesReciboModal')).show();
};

// Generar el desglose dinámico y exacto del recibo
function generarTextoRecibo(cliente, esAmbos) {
    const hoy = new Date();
    const fechaFormateada = hoy.toLocaleDateString('es-AR');

    // 1. Tarifa del Plan de Internet
    const plan = cliente.plan || {};
    const precioInternet = parseFloat(plan.precioEfectivo || plan.precioTransferencia || 0);
    const nombrePlan = plan.cantidadMB ? `Internet ${plan.cantidadMB}MB` : 'Servicio de Internet';

    // 2. Adicionales de Televisión / Fibra
    const tieneFibra = Boolean(cliente.tieneFibraTV) && !cliente.esDemo;
    const precioFibra = tieneFibra ? (configPreciosReciboCache.precioFibraTV || configPreciosReciboCache.precioFibra || 0) : 0;

    const tieneCable = Boolean(cliente.tieneTV);
    const precioCable = tieneCable ? (configPreciosReciboCache.precioCableTV || configPreciosReciboCache.precioCable || 0) : 0;

    // 3. Deuda o Costo de Instalación
    // 3. Deuda o Costo de Instalación (Corregido)
    const tieneDeudaInstalacion = cliente.deudaInstalacion && cliente.deudaInstalacion.toUpperCase() !== 'NO';
    const montoInstalacion = tieneDeudaInstalacion ? parseFloat(cliente.costoInstalacion || cliente.montoInstalacion || 0) : 0;
    // 4. Suma Total Facturada
    const totalFacturado = precioInternet + precioFibra + precioCable + montoInstalacion;

    const mesActual = hoy.toISOString().slice(0, 7);
    const pagoMes = cliente.pagoMes || mesActual;
    const direccion = cliente.direccion || 'N/A';

    // 5. Construcción del texto
    let msg = `*RECIBO DE PAGO - ${fechaFormateada}*\n`;
    msg += `*Cliente:* ${cliente.nombre}\n`;
    msg += `*Dirección:* ${direccion}\n`;
    msg += `*Período:* ${pagoMes}\n`;
    msg += `-----------------------------------\n`;
    msg += `*Detalle de Conceptos:*\n`;
    msg += `• ${nombrePlan}: $${precioInternet.toFixed(2)}\n`;

    if (tieneFibra) {
        msg += `• Servicio Fibra TV: $${precioFibra.toFixed(2)}\n`;
    }
    if (tieneCable) {
        msg += `• Servicio TV Cable: $${precioCable.toFixed(2)}\n`;
    }
    if (montoInstalacion > 0) {
        msg += `• Cargo / Instalación: $${montoInstalacion.toFixed(2)}\n`;
    }

    msg += `-----------------------------------\n`;
    msg += `*TOTAL FACTURADO:* $${totalFacturado.toFixed(2)}\n`;

    if (esAmbos) {
        msg += `\n*Nota:* También enviamos el comprobante en formato PDF a tu correo electrónico.`;
    }

    msg += `\n\n¡Muchas gracias por su pago!`;

    return msg;
}

// Función auxiliar para abrir WhatsApp Web / App
function abrirWhatsapp(telefono, mensaje) {
    const numLimpio = telefono.replace(/\D/g, '');
    const url = `https://api.whatsapp.com/send?phone=${numLimpio}&text=${encodeURIComponent(mensaje)}`;
    window.open(url, '_blank');
}

// Enviar correo electrónico vía API
window.solicitarEnvioEmail = async function(id) {
    try {
        const resp = await fetch('/api/recibos/enviar-email/' + id, { 
            method: 'POST' 
        });
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

// Manejador central de los botones de acción del recibo
window.accionRecibo = async function(tipo) {
    if (!clienteReciboActual) return;
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
            abrirWhatsapp(clienteReciboActual.telefono, mensajeWppAmbos);
            await solicitarEnvioEmail(id);
            break;
        }
    }
};