// ============================================================
// MÓDULO DE RECIBOS DE PAGO (AppClientes-recibos.js)
// ============================================================

let clienteReciboActual = null;

// Abrir modal consultando los precios reales al backend y el último pago
window.abrirModalOpcionesRecibo = async function(cliente) {
    try {
        // 1. TRUCO FRONTEND: Recordar el costo de instalación en esta sesión
        // Si la tabla todavía tiene la deuda, la guardamos antes de consultar al backend
        if (parseFloat(cliente.costoInstalacion) > 0) {
            sessionStorage.setItem('memoria_instalacion_' + cliente.id, cliente.costoInstalacion);
        }

        // 2. Traer datos actualizados del cliente
        const respCli = await fetch('/clientes/' + cliente.id);
        const cli = respCli.ok ? await respCli.json() : cliente;

        // 3. RECUPERAR instalación si el backend ya la puso en 0 después de pagar
        const costoInstalacionGuardado = sessionStorage.getItem('memoria_instalacion_' + cli.id);
        if (costoInstalacionGuardado && (!cli.costoInstalacion || cli.costoInstalacion === 0)) {
            cli.costoInstalacion = parseFloat(costoInstalacionGuardado);
            cli.deudaInstalacion = 'SI';
        }

        // 4. Traer el catálogo de planes para buscar el precio de Internet exacto
        const respPlanes = await fetch('/api/planes');
        let precioInternet = 0;
        if (respPlanes.ok) {
            const planes = await respPlanes.json();
            const planEncontrado = planes.find(p => p.cantidadMB === cli.cantidadMB);
            if (planEncontrado) {
                precioInternet = parseFloat(planEncontrado.precioEfectivo) || 0;
            }
        }
        cli.precioInternetReal = precioInternet;

        // 5. Traer los precios exactos de TV desde Configuracion
        let precioFibraGlobal = 0;
        let precioCableGlobal = 0;

        const respFibra = await fetch('/api/configuracion/fibratv');
        if (respFibra.ok) {
            const confFibra = await respFibra.json();
            precioFibraGlobal = parseFloat(confFibra.valor) || 0;
        }

        const respCable = await fetch('/api/configuracion/cabletv');
        if (respCable.ok) {
            const confCable = await respCable.json();
            precioCableGlobal = parseFloat(confCable.valor) || 0;
        }

        // Asignar los precios a cobrar según lo que el cliente tenga configurado
        cli.precioFibraCalculado = (cli.tieneFibraTV && !cli.esDemo) ? precioFibraGlobal : 0;
        cli.precioCableCalculado = (cli.tieneTV) ? precioCableGlobal : 0;

        // 6. Buscar el último pago registrado para saber cuánto abonó realmente
        try {
            const respHistorial = await fetch('/pagos/' + cli.id + '/historial');
            if (respHistorial.ok) {
                const historial = await respHistorial.json();
                if (historial.length > 0) {
                    cli.ultimoMontoPagado = parseFloat(historial[0].monto);
                }
            }
        } catch (e) {
            console.warn("No se pudo obtener el historial de pagos");
        }

        clienteReciboActual = cli;
    } catch (e) {
        console.error("Error obteniendo datos para el recibo:", e);
        clienteReciboActual = cliente; // Fallback
    }

    const cli = clienteReciboActual;
    document.getElementById('nombreReciboCliente').innerText = 'Cliente: ' + (cli.nombre || '');
    
    const container = document.getElementById('botonesReciboContainer');
    container.innerHTML = ''; 

    container.innerHTML += '<button class="btn btn-primary mb-2 w-100" onclick="accionRecibo(\'DESCARGAR\')">📄 Descargar PDF</button>';

    const tieneTel = cli.telefono && cli.telefono.trim() !== "";
    const tieneEmail = cli.email && cli.email.trim() !== "";

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

// Generar el desglose usando los valores exactos calculados
function generarTextoRecibo(cliente, esAmbos) {
    const hoy = new Date();
    const fechaFormateada = hoy.toLocaleDateString('es-AR');

    // 1. Precios calculados previamente
    const precioInternet = cliente.precioInternetReal || 0;
    const mb = cliente?.cantidadMB || cliente?.planCantidadMB;
    const nombrePlan = mb ? `Internet ${mb}MB` : 'Servicio de Internet';

    const precioFibra = cliente.precioFibraCalculado || 0;
    const precioCable = cliente.precioCableCalculado || 0;

    // 2. Deuda de Instalación
    const tieneDeudaInstalacion = cliente?.deudaInstalacion && String(cliente.deudaInstalacion).toUpperCase() !== 'NO';
    const montoInstalacion = tieneDeudaInstalacion ? Number(cliente?.costoInstalacion || 0) : 0;

    // 3. Sumas, Pagos y Saldos
    const totalFacturado = precioInternet + precioFibra + precioCable + montoInstalacion;
    const montoAbonado = cliente.ultimoMontoPagado !== undefined ? cliente.ultimoMontoPagado : totalFacturado;

    // Lógica inteligente de Saldo Pendiente
    // a. Lo que falta pagar de la factura actual
    let saldoCalculado = totalFacturado - montoAbonado;
    if (saldoCalculado < 0) saldoCalculado = 0;

    // b. Lo que dice el backend (por si arrastra deuda de meses anteriores)
    const saldoBackend = parseFloat(cliente.saldoPendiente) || 0;
    
    // c. Nos quedamos con el mayor para nunca dejar de mostrar la deuda
    const saldoFinal = Math.max(saldoCalculado, saldoBackend);

    const mesActual = hoy.toISOString().slice(0, 7);
    const pagoMes = cliente?.pagoMes || mesActual;
    const direccion = cliente?.direccion || 'N/A';

    // 4. Construcción del texto
    let msg = `*RECIBO DE PAGO - ${fechaFormateada}*\n`;
    msg += `*Cliente:* ${cliente?.nombre || ''}\n`;
    msg += `*Dirección:* ${direccion}\n`;
    msg += `*Período:* ${pagoMes}\n`;
    msg += `-----------------------------------\n`;
    msg += `*Detalle de Conceptos del Mes:*\n`;
    msg += `• ${nombrePlan}: $${precioInternet.toFixed(2)}\n`;

    if (precioFibra > 0) {
        msg += `• Servicio Fibra TV: $${precioFibra.toFixed(2)}\n`;
    }
    if (precioCable > 0) {
        msg += `• Servicio TV Cable: $${precioCable.toFixed(2)}\n`;
    }
    if (montoInstalacion > 0) {
        msg += `• Costo Instalación: $${montoInstalacion.toFixed(2)}\n`;
    }

    msg += `-----------------------------------\n`;
    msg += `*TOTAL FACTURADO:* $${totalFacturado.toFixed(2)}\n`;
    msg += `*MONTO ABONADO:* $${montoAbonado.toFixed(2)}\n`;

    if (saldoFinal > 0) {
        msg += `*SALDO PENDIENTE:* $${saldoFinal.toFixed(2)}\n`;
    }

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