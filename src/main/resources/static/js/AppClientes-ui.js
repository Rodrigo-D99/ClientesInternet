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
                fibraTVCell = `<td><span class="badge bg-warning text-dark">DEMO</span><br><small>${c.usuarioFibraTV || ''} (${diasRestantes} días)</small></td>`;
            } else {
                // Demo expirada
                fibraTVCell = `<td><span class="badge bg-danger">Expirada</span><br><small>${c.usuarioFibraTV || ''}</small> <button class="btn btn-sm btn-info" onclick="renovarFibraTV('${c.usuarioFibraTV || ''}')">Renovar</button></td>`;
            }
        } else if (c.tieneFibraTV) {
            // Subscripción activa
            fibraTVCell = `<td><span class="badge bg-success">Sí</span><br><small>${c.usuarioFibraTV || ''}</small> <button class="btn btn-sm btn-info" onclick="renovarFibraTV('${c.usuarioFibraTV || ''}')">Renovar</button></td>`;
        } else {
            // Sin Fibra TV
            fibraTVCell = `<td><span class="badge bg-danger">No</span> <button class="btn btn-sm btn-success" onclick="crearDemoFibraTV(${c.id})">Demo 5 días</button></td>`;
        }

        // Crear fila
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
            <td>${c.mesesPagados || 0}</td>
            ${fibraTVCell}
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
                ${c.mesesAdeudados > 0 && c.telefono ? `
                <button class="btn btn-sm btn-success"
                        onclick='avisarDeuda(${JSON.stringify(c.nombre)}, ${JSON.stringify(c.telefono)}, ${c.mesesAdeudados})'>
                    📱 WhatsApp
                </button>
                ` : ""}
            </td>
        `;

        tbody.appendChild(tr);
    });
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
