// ============================================================
// FUNCIONES DE RENDERIZACIÓN Y UI
// ============================================================

// Renderizar la tabla de clientes
function renderTable(clientes) {
    const tbody = document.getElementById("clientesTableBody");
    tbody.innerHTML = '';
    
    clientes.forEach(c => {
        const dni = (c.medioPago === 'TRANSFERENCIA' || c.medioPago === 'TARJETA')
                    ? (c.dni || '')
                    : '';

        // Generar celda de Fibra TV con estado
        let fibraTVCell = '';
        
        if (c.esDemo && c.fechaVencimientoDemo) {
            const hoy = new Date();
            const fechaVencimiento = new Date(c.fechaVencimientoDemo);
            const estaVigente = fechaVencimiento >= hoy;
            
            if (estaVigente) {
                // Demo vigente
                const diasRestantes = Math.ceil((fechaVencimiento - hoy) / (1000 * 60 * 60 * 24));
                fibraTVCell = `<td>
                    <div class="fibra-tv-container">
                        <div class="fibra-tv-row">
                            <span class="badge bg-warning text-dark">DEMO</span>
                            <small>${c.usuarioFibraTV || ''} (${diasRestantes} días)</small>
                        </div>
                    </div>
                </td>`;
            } else {
                // Demo expirada
                fibraTVCell = `<td>
                    <div class="fibra-tv-container">
                        <div class="fibra-tv-row">
                            <span class="badge bg-danger">Expirada</span>
                            <small>${c.usuarioFibraTV || ''}</small>
                        </div>
                        <button class="btn btn-sm btn-info" onclick="renovarFibraTV('${c.usuarioFibraTV || ''}')">Renovar</button>
                    </div>
                </td>`;
            }
        } else if (c.tieneFibraTV) {
            // Subscripción activa
            fibraTVCell = `<td>
                <div class="fibra-tv-container">
                    <div class="fibra-tv-row">
                        <span class="badge bg-success">Sí</span>
                        <small>${c.usuarioFibraTV || ''}</small>
                    </div>
                    <button class="btn btn-sm btn-info" onclick="renovarFibraTV('${c.usuarioFibraTV || ''}')">Renovar</button>
                </div>
            </td>`;
        } else {
            // Sin Fibra TV
            fibraTVCell = `<td>
                <div class="fibra-tv-container">
                    <span class="badge bg-danger">No</span>
                    <button class="btn btn-sm btn-success" onclick="crearDemoFibraTV(${c.id})">Demo 5 días</button>
                </div>
            </td>`;
        }
        let estadoInstalacion = c.deudaInstalacion === 'NO' || !c.deudaInstalacion
        ? `<span class="badge bg-success">NO</span>`
        : `<span class="badge bg-danger">${c.deudaInstalacion}</span>`;

        let montoMostrar = (c.deudaInstalacion !== 'NO' && c.costoInstalacion > 0) 
        ? `<strong class="text-danger">$ ${c.costoInstalacion}</strong>` 
        : `<span class="text-muted"></span>`;
        let TVCell = '';

        if (c.tieneTV) {
            // Subscripción activa
            TVCell = `<td>
                <div class="fibra-tv-container">
                    <div class="fibra-tv-row">
                        <span class="badge bg-success">Sí</span>
                    </div>
                </div>
            </td>`;
        } else {
            // Sin Fibra TV
            TVCell = `<td>
                <div class="fibra-tv-container">
                    <span class="badge bg-danger">No</span>
                </div>
            </td>`;}

        const notaTexto = c.nota || "";
        const notaSegura = notaTexto.replace(/'/g, "\\'").replace(/"/g, '&quot;');
        const notaHTML = notaTexto ? `
            <div style="max-width: 180px; min-width: 120px;">
                <div style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.2; font-size: 0.85rem; color: #555;">
                    ${notaTexto}
                </div>
                ${notaTexto.length > 40 ? `<a href="#" class="text-primary" style="font-size: 0.75rem; font-weight: bold; text-decoration: none;" onclick="alert('${notaSegura}'); return false;">Ver más...</a>` : ""}
            </div>
        ` : "";
        // Crear fila
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>
                ${c.nombre}
                <span class="badge ms-2"
                      style="background-color: ${c.mesesAdeudados > 0 ? 'red' : 'green'};
                             width:16px; height:16px; display:inline-block; border-radius:50%;"></span>
            </td>
            <td>${c.telefono || ''}</td>
            <td>${c.direccion}</td>
            <td>${c.mesesAdeudados}</td>
            <td>${c.mesesPagados || 0}</td>
            ${fibraTVCell}
            ${TVCell}
            <td>${c.medioPago || ''}</td>
            <td>${dni}</td>
            <td>${estadoInstalacion}${montoMostrar}</td>
            <td>${c.montoUltimoPago || ""} - ${notaHTML || ""}</td>
            <td>${c.fechaUltimoPago || ''}</td>
            <td>${c.cantidadMB || ''}</td>
            <td>
                <div class="actions-container">
                    <div>
                        <button class="btn btn-sm btn-warning"
                            onclick="editarCliente(${c.id})"> Editar
                        </button>
                            <button class="btn btn-sm btn-info" 
                            onclick="generarReciboIndividual(${c.id})"> Generar Recibo
                        </button>
                    </div>
                    <div>
                        <button class="btn btn-sm btn-danger"
                                onclick="eliminarCliente(${c.id})"> Eliminar
                        </button>
                        ${c.mesesAdeudados > 0 && c.telefono ? `
                        <button class="btn btn-sm btn-success"
                                onclick='avisarDeuda(${JSON.stringify(c.nombre)}, ${JSON.stringify(c.telefono)}, ${c.mesesAdeudados})'>
                            WhatsApp
                        </button>
                        ` : ""}
                    </div>
                </div>
            </td>
        `;

        tbody.appendChild(tr);
    });
    
    // Actualizar contadores
    actualizarContadores(clientes);
    
}

// Renderizar paginación
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

// Actualizar flechas de ordenamiento en la tabla
function actualizarFlechas() {
    document
        .querySelectorAll("th span")
        .forEach(s => s.textContent = "⬍");

    const arrow = currentDir === "asc" ? "▲" : "▼";
    const span = document.getElementById(`sort-${currentSort}`);
    if (span) span.textContent = arrow;
}
