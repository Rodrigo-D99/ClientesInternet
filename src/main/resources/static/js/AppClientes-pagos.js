// ============================================================
// MÓDULO DE PAGOS Y CALCULADORA (AppClientes-pagos.js)
// ============================================================

let clienteSeleccionadoPago = null;
let listaPlanesCache = [];
let configPreciosCache = { precioFibraTV: 0, precioCableTV: 0 };
// Variables globales de estado para adicionales incluidos en el cobro actual
let incluirFibraState = true;
let incluirCableState = true;
let incluirInstalacionState = true;

// Función auxiliar de seguridad numérica para evitar fallos NaN
function numSeguro(valor, porDefecto = 0) {
    if (valor === null || valor === undefined || valor === "") return porDefecto;
    const numero = parseFloat(valor);
    return isNaN(numero) ? porDefecto : numero;
}

// Validar DNI recomendado según medio de pago
function validarDniRecomendado() {
    const medioPago = document.getElementById("medioPago")?.value;
    const dni = document.getElementById("dni")?.value;
    const warning = document.getElementById("dniWarning");

    if (!warning) return;

    if (
        (medioPago === "TRANSFERENCIA" || medioPago === "TARJETA") && 
        (!dni || dni.trim() === "")
    ) {
        warning.style.display = "block";
    } else {
        warning.style.display = "none";
    }
}
// Activar los botones de las filas al cargar el documento
document.addEventListener("DOMContentLoaded", () => {
    if (typeof inicializarTogglesModalPago === "function") {
        inicializarTogglesModalPago();
    }
});

async function abrirModalPago(idCliente) {
    try {
        const [respCliente, respPlanes, respFibra, respCable, respInst] = await Promise.all([
            fetch(`/clientes/${idCliente}`),
            fetch('/api/planes').catch(() => null),
            fetch('/api/configuracion/fibratv').catch(() => null),
            fetch('/api/configuracion/cabletv').catch(() => null),
            fetch('/api/configuracion/instalaciones').catch(() => null)
        ]);

        if (!respCliente.ok) throw new Error("No se pudo obtener la información del cliente.");
        
        clienteSeleccionadoPago = await respCliente.json();

        if (respPlanes && respPlanes.ok) {
            listaPlanesCache = await respPlanes.json();
        }

        // 1. Cargar instalaciones
        if (respInst && respInst.ok) {
            const instalaciones = await respInst.json();
            mapaPreciosInstalaciones = {};
            instalaciones.forEach(item => {
                if (item.nombre) mapaPreciosInstalaciones[item.nombre.toUpperCase()] = item.precio;
            });
        }

        // 2. Extraer precios de los JSON { clave, valor }
        let precioFibra = 0;
        let precioCable = 0;

        if (respFibra && respFibra.ok) {
            const configFibra = await respFibra.json().catch(() => null);
            if (configFibra && configFibra.valor !== undefined && configFibra.valor !== null) {
                precioFibra = numSeguro(configFibra.valor);
            }
        }

        if (respCable && respCable.ok) {
            const configCable = await respCable.json().catch(() => null);
            if (configCable && configCable.valor !== undefined && configCable.valor !== null) {
                precioCable = numSeguro(configCable.valor);
            }
        }

        configPreciosCache = {
            precioFibraTV: precioFibra,
            precioCableTV: precioCable
        };

        incluirFibraState = true;
        incluirCableState = true;
        incluirInstalacionState = true;

        document.getElementById("formPago").reset();
        document.getElementById("pagoClienteId").value = clienteSeleccionadoPago.id;
        document.getElementById("pagoMedioPago").value = "EFECTIVO";
        document.getElementById("pagoCantidadMeses").value = 1;
        document.getElementById("pagoDni").value = clienteSeleccionadoPago.dni || "";
        document.getElementById("pagoModalTitulo").innerText = `Registrar Pago - ${clienteSeleccionadoPago.nombre}`;

        recalcularModalPago(false);

        new bootstrap.Modal(document.getElementById('pagoModal')).show();
    } catch (error) {
        alert("Error al abrir el modal de pago: " + error.message);
    }
}

function recalcularModalPago(montoEditadoManual = false) {
    if (!clienteSeleccionadoPago) return;

    const medioPago = document.getElementById("pagoMedioPago")?.value || "EFECTIVO";
    const cantidadMeses = Math.max(1, parseInt(document.getElementById("pagoCantidadMeses")?.value) || 1);

    let plan = clienteSeleccionadoPago.plan;
    if (!plan && clienteSeleccionadoPago.cantidadMB && listaPlanesCache.length > 0) {
        plan = listaPlanesCache.find(p => p.cantidadMB === clienteSeleccionadoPago.cantidadMB || p.id === clienteSeleccionadoPago.cantidadMB);
    }

    let precioInternet = plan ? ((medioPago === "EFECTIVO") ? numSeguro(plan.precioEfectivo) : numSeguro(plan.precioTransferencia || plan.precioEfectivo)) : 0;

    const tieneFibra = Boolean(clienteSeleccionadoPago.tieneFibraTV) && !clienteSeleccionadoPago.esDemo;
    const tieneCable = Boolean(clienteSeleccionadoPago.tieneTV);
    
    // Asignación directa garantizando el respeto al valor $0
    const precioFibraBase = tieneFibra ? numSeguro(configPreciosCache.precioFibraTV) : 0;
    const precioCableBase = tieneCable ? numSeguro(configPreciosCache.precioCableTV) : 0;

    const precioFibraCobrar = incluirFibraState ? precioFibraBase : 0;
    const precioCableCobrar = incluirCableState ? precioCableBase : 0;

    // Actualizar UI Fibra
    const rowFibra = document.getElementById("rowFibraTV");
    if (rowFibra) {
        if (tieneFibra) {
            rowFibra.style.display = "flex";
            rowFibra.style.textDecoration = incluirFibraState ? "none" : "line-through";
            rowFibra.style.opacity = incluirFibraState ? "1" : "0.5";
            document.getElementById("calcFibraTV").innerText = `${incluirFibraState ? '+' : '-'} $${precioFibraBase}`;
        } else {
            rowFibra.style.display = "none";
        }
    }

    // Actualizar UI Cable
    const rowCable = document.getElementById("rowCableTV");
    if (rowCable) {
        if (tieneCable) {
            rowCable.style.display = "flex";
            rowCable.style.textDecoration = incluirCableState ? "none" : "line-through";
            rowCable.style.opacity = incluirCableState ? "1" : "0.5";
            document.getElementById("calcCableTV").innerText = `${incluirCableState ? '+' : '-'} $${precioCableBase}`;
        } else {
            rowCable.style.display = "none";
        }
    }

    // Deuda de Instalación (sincronizada con tabla global de instalaciones)
    let deudaInstalacionBase = 0;
    const tipoDeuda = clienteSeleccionadoPago.deudaInstalacion;
    if (tipoDeuda && tipoDeuda !== "NO" && tipoDeuda !== "false" && tipoDeuda !== false) {
        const tipoKey = String(tipoDeuda).toUpperCase();
        if (typeof mapaPreciosInstalaciones !== 'undefined' && mapaPreciosInstalaciones[tipoKey] !== undefined) {
            deudaInstalacionBase = numSeguro(mapaPreciosInstalaciones[tipoKey]);
        } else {
            deudaInstalacionBase = numSeguro(clienteSeleccionadoPago.costoInstalacion ?? clienteSeleccionadoPago.montoInstalacion);
        }
    }

    const deudaInstalacionCobrar = incluirInstalacionState ? deudaInstalacionBase : 0;
    
    const rowInst = document.getElementById("rowInstalacion");
    if (rowInst) {
        if (deudaInstalacionBase > 0) {
            rowInst.style.display = "flex"; 
            rowInst.style.textDecoration = incluirInstalacionState ? "none" : "line-through";
            rowInst.style.opacity = incluirInstalacionState ? "1" : "0.5";
            document.getElementById("calcInstalacion").innerText = `${incluirInstalacionState ? '+' : '-'} $${deudaInstalacionBase}`;
        } else {
            rowInst.style.display = "none";
        }
    }

    // Totales finales
    const tarifaMensualServicios = precioInternet + precioFibraCobrar + precioCableCobrar;
    const montoSugerido = (tarifaMensualServicios * cantidadMeses) + deudaInstalacionCobrar;
    const montoSugeridoDeuda = (tarifaMensualServicios * (clienteSeleccionadoPago?.mesesAdeudados+1 || 0)) + deudaInstalacionBase;

    const inputMonto = document.getElementById("pagoMonto");
    if (inputMonto && !montoEditadoManual) {
        inputMonto.value = montoSugerido;
    }

    document.getElementById("calcTarifaBase").innerText = `$${precioInternet}`;
    document.getElementById("calcMontoSugerido").innerText = `$${montoSugerido}`;
    document.getElementById("calcDeudaTotal").innerText = `$${montoSugeridoDeuda}`;
    document.getElementById("calcSaldoPendiente").innerText = `${montoSugeridoDeuda - (inputMonto ? numSeguro(inputMonto.value) : 0)}$`;
    document.getElementById("calcMesesRestantes").innerText = `${Math.max(0, (clienteSeleccionadoPago?.mesesAdeudados+1 || 0) - cantidadMeses)} mes(es)`;
}

// Inicializar event listeners para hacer clic en los filas del modal de cobro
function inicializarTogglesModalPago() {
    const rowFibra = document.getElementById("rowFibraTV");
    const rowCable = document.getElementById("rowCableTV");
    const rowInstalacion = document.getElementById("rowInstalacion");

    if (rowFibra) {
        rowFibra.style.cursor = "pointer";
        rowFibra.onclick = () => {
            incluirFibraState = !incluirFibraState;
            recalcularModalPago(false);
        };
    }
    if (rowCable) {
        rowCable.style.cursor = "pointer";
        rowCable.onclick = () => {
            incluirCableState = !incluirCableState;
            recalcularModalPago(false);
        };
    }
    if (rowInstalacion) {
        rowInstalacion.style.cursor = "pointer";
        rowInstalacion.onclick = () => {
            incluirInstalacionState = !incluirInstalacionState;
            recalcularModalPago(false);
        };
    }
}

// Registrar pago enviando formulario
async function registrarPago(e) {
    e.preventDefault();
    const clienteId = document.getElementById("pagoClienteId").value;
    let notaAutomatica = document.getElementById("pagoNota").value;

    // Se obtienen las variables directamente del cliente seleccionado
    const tieneFibra = Boolean(clienteSeleccionadoPago?.tieneFibraTV) && !clienteSeleccionadoPago?.esDemo;
    const tieneCable = Boolean(clienteSeleccionadoPago?.tieneTV);
    const tipoDeuda = clienteSeleccionadoPago?.deudaInstalacion;
    const deudaInstalacionBase = (tipoDeuda && tipoDeuda !== "NO" && tipoDeuda !== "false" && tipoDeuda !== false) 
        ? numSeguro(clienteSeleccionadoPago?.costoInstalacion ?? clienteSeleccionadoPago?.montoInstalacion) 
        : 0;

    if (!notaAutomatica || notaAutomatica.trim() === "") {
        const cantMeses = parseInt(document.getElementById("pagoCantidadMeses").value) || 1;
        let detalles = [];
        
        if (cantMeses > 1) detalles.push(`Abona ${cantMeses} meses`);
        if (incluirFibraState && tieneFibra) detalles.push("Fibra TV");
        if (incluirCableState && tieneCable) detalles.push("Cable");
        if (incluirInstalacionState && deudaInstalacionBase > 0) {
            detalles.push(`Pago Instalación (${tipoDeuda}): $${deudaInstalacionBase}`);
        }

        notaAutomatica = detalles.length > 0 ? detalles.join(", ") : `Abona ${cantMeses} mes(es)`;
    }

    const pagoReq = {
        monto: parseFloat(document.getElementById("pagoMonto").value),
        medioPago: document.getElementById("pagoMedioPago").value,
        cantidadMeses: parseInt(document.getElementById("pagoCantidadMeses").value) || 1,
        dniPagador: document.getElementById("pagoDni").value || null,
        saldaInstalacion: incluirInstalacionState,
        nota: notaAutomatica
    };
    
    if (!pagoReq.medioPago || isNaN(pagoReq.monto)) {
        alert("Debe ingresar un Medio de Pago y un Monto válido.");
        return;
    }

    if (pagoReq.medioPago !== 'EFECTIVO' && (!pagoReq.dniPagador || pagoReq.dniPagador.trim() === '')) {
        if (!confirm("Atención: No has ingresado un DNI para este pago electrónico. ¿Deseas continuar de todos modos?")) {
            return;
        }
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

        const pagoResp = await resp.json();
        
        const pagoModalEl = bootstrap.Modal.getInstance(document.getElementById('pagoModal'));
        if (pagoModalEl) pagoModalEl.hide();

        if (typeof fetchClientes === 'function') fetchClientes(currentPage);
        if (typeof actualizarEstadisticasHoy === 'function') actualizarEstadisticasHoy();

        if (pagoResp.advertencia) {
            alert(pagoResp.advertencia);
        } else {
            alert("Pago registrado correctamente");
        }

    } catch (error) {
        alert("Error al registrar el pago: " + error.message);
    }
}

// Ejecución de pago directo sin modal extenso
async function ejecutarPagoDirecto(clienteId) {
    const pagoReq = {
        monto: parseFloat(document.getElementById("monto").value),
        medioPago: document.getElementById("medioPago").value,
        cantidadMeses: parseInt(document.getElementById("cantidadMeses").value) || 1,
        nota: document.getElementById("nota").value || null,
        dniPagador: document.getElementById("dni").value || null
    };

    if (!pagoReq.medioPago) {
        throw new Error("Debe seleccionar un Medio de Pago para registrar el monto.");
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
}

// Historial de Pagos
async function verHistorialPagos(clienteId) {
    try {
        const resp = await fetch(`/pagos/${clienteId}/historial`);
        if (!resp.ok) throw new Error("Error al obtener el historial de pagos");
        
        let historial = await resp.json();
        const tbody = document.getElementById("historialTableBody");
        tbody.innerHTML = ""; 

        if (!historial || historial.length === 0) {
            tbody.innerHTML = "<tr><td colspan='7' class='text-center text-muted'>No hay pagos registrados para este cliente.</td></tr>";
            new bootstrap.Modal(document.getElementById('historialModal')).show();
            return;
        }

        historial.sort((a, b) => (a.id || 0) - (b.id || 0));

        let fechaCobertura = null;

        historial.forEach(p => {
            const fechaPagoObj = p.fechaPago ? new Date(p.fechaPago + "T00:00:00") : new Date();
            
            if (!fechaCobertura || fechaPagoObj > fechaCobertura) {
                fechaCobertura = new Date(fechaPagoObj);
            }
            
            fechaCobertura.setMonth(fechaCobertura.getMonth() + (p.cantidadMeses || 1));
            
            const mes = String(fechaCobertura.getMonth() + 1).padStart(2, '0');
            const anio = fechaCobertura.getFullYear();
        });

        historial.sort((a, b) => (b.id || 0) - (a.id || 0));

        historial.forEach(p => {
            const fechaFormat = p.fechaPago ? p.fechaPago.split('-').reverse().join('/') : '-';
            const notaSegura = p.nota ? p.nota.replace(/'/g, "\\'") : '';
            const idPago = p.id; 

            tbody.innerHTML += `
                <tr id="pago-row-${idPago}">
                    <td><strong>${fechaFormat}</strong></td>
                    <td class="col-monto text-success fw-bold">$${p.monto}</td>
                    <td class="col-medio"><span class="badge bg-secondary">${p.medioPago}</span></td>
                    <td class="col-meses">${p.cantidadMeses}</td>
                    <td class="col-nota text-start">${p.nota || '-'}</td>
                    <td class="col-acciones">
                        <button class="btn btn-sm btn-outline-primary" onclick="activarEdicionFila(${idPago}, ${p.monto}, '${p.medioPago}', ${p.cantidadMeses}, '${notaSegura}')">✏️</button>
                    </td>
                </tr>
            `;
        });
        
        new bootstrap.Modal(document.getElementById('historialModal')).show();
        
    } catch (error) {
        alert("Error al cargar el historial: " + error.message);
    }
}

// Edición de fila individual en historial
function activarEdicionFila(id, monto, medio, meses, nota) {
    const row = document.getElementById(`pago-row-${id}`);
    
    row.querySelector('.col-monto').innerHTML = `<input type="number" id="edit-monto-${id}" class="form-control form-control-sm" value="${monto}" min="0">`;
    row.querySelector('.col-medio').innerHTML = `
        <select id="edit-medio-${id}" class="form-select form-select-sm">
            <option value="EFECTIVO" ${medio === 'EFECTIVO' ? 'selected' : ''}>EFECTIVO</option>
            <option value="TRANSFERENCIA" ${medio === 'TRANSFERENCIA' ? 'selected' : ''}>TRANSFERENCIA</option>
            <option value="TARJETA" ${medio === 'TARJETA' ? 'selected' : ''}>TARJETA</option>
        </select>`;
        
    row.querySelector('.col-meses').innerHTML = `<input type="number" id="edit-meses-${id}" class="form-control form-control-sm" value="${meses}" min="1">`;
    row.querySelector('.col-nota').innerHTML = `<input type="text" id="edit-nota-${id}" class="form-control form-control-sm" value="${nota}">`;
    row.querySelector('.col-acciones').innerHTML = `<button class="btn btn-sm btn-success" onclick="guardarEdicionFila(${id})">✔️</button>`;
}

async function guardarEdicionFila(id) {
    const data = {
        monto: parseFloat(document.getElementById(`edit-monto-${id}`).value),
        medioPago: document.getElementById(`edit-medio-${id}`).value,
        cantidadMeses: parseInt(document.getElementById(`edit-meses-${id}`).value),
        nota: document.getElementById(`edit-nota-${id}`).value
    };

    try {
        const resp = await fetch(`/pagos/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (!resp.ok) throw new Error("Error al modificar el pago " + id);

        location.reload();
    } catch (error) {
        alert(error.message);
    }
}