// ============================================================
// FUNCIONES DE FIBRA TV Y DEMO
// ============================================================

function renovarFibraTV(usuarioFibraTV) {
    // Copiar el usuario al portapapeles
    navigator.clipboard.writeText(usuarioFibraTV).then(() => {
        console.log('Usuario copiado al portapapeles:', usuarioFibraTV);
    }).catch(err => {
        console.error('Error al copiar:', err);
    });

    // Abrir popup
    const url = 'https://impakto.live/vend/index.php';
    const popup = window.open(url, 'FibraTV', 'width=1200,height=800');
    
    if (popup) {
        popup.focus();
        console.log('Popup abierto para renovar Fibra TV del usuario:', usuarioFibraTV);
    }
}

function crearDemoFibraTV(clienteId) {
    // Primero abrir impakto.live para crear la demo
    const popup = window.open('https://impakto.live/vend/index.php', 'FibraTV', 'width=1200,height=800');
    
    if (popup) {
        popup.focus();
    }
    
    // Luego abrir modal para ingresar usuario de Fibra TV
    setTimeout(() => {
        const modal = new bootstrap.Modal(document.getElementById('modalDemoUsuario'));
        const inputUsuario = document.getElementById('inputUsuarioDemoFibra');
        const btnConfirmar = document.getElementById('btnConfirmarDemo');
        
        // Limpiar input
        inputUsuario.value = '';
        
        // Guardar clienteId en un atributo del botón para usarlo después
        btnConfirmar.dataset.clienteId = clienteId;
        
        // Mostrar el modal
        modal.show();
    }, 500); // Esperar 500ms para que impakto.live abra primero
}

// Evento para el botón de confirmar en el modal
document.addEventListener("DOMContentLoaded", () => {
    const btnConfirmar = document.getElementById('btnConfirmarDemo');
    if (btnConfirmar) {
        btnConfirmar.addEventListener('click', confirmarCrearDemo);
    }
});

async function confirmarCrearDemo() {
    const inputUsuario = document.getElementById('inputUsuarioDemoFibra');
    const usuarioFibra = inputUsuario.value.trim();
    
    if (!usuarioFibra) {
        alert("Por favor ingrese el usuario de Fibra TV");
        return;
    }
    
    const clienteId = this.dataset.clienteId;
    
    // Calcular fecha de vencimiento (hoy + 5 días)
    const hoy = new Date();
    const fechaVencimiento = new Date(hoy);
    fechaVencimiento.setDate(fechaVencimiento.getDate() + 5);
    const fechaVencimientoStr = fechaVencimiento.toISOString().split('T')[0];
    
    try {
        const resp = await fetch(`/clientes/${clienteId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ 
                tieneFibraTV: true,
                esDemo: true,
                usuarioFibraTV: usuarioFibra,
                fechaVencimientoDemo: fechaVencimientoStr
            })
        });

        if (!resp.ok) {
            alert("Error al crear la demo");
            return;
        }

        // Cerrar el modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalDemoUsuario'));
        modal.hide();
        
        // Recargar tabla para reflejar el cambio
        fetchClientes(currentPage);
        
        // Copiar usuario al portapapeles
        navigator.clipboard.writeText(usuarioFibra);
        
        // Abrir página de impakto.live
        const popup = window.open('https://impakto.live/vend/index.php', 'FibraTV', 'width=1200,height=800');
        
        if (popup) {
            popup.focus();
            alert(`Usuario "${usuarioFibra}" copiado al portapapeles. Pégalo en impakto.live para crear la demo.`);
        }
        
    } catch (err) {
        console.error(err);
        alert("Error al crear la demo");
    }
}

// Función auxiliar (deprecated)
function abrirPaginaFibraTV(nombreCliente) {
    // Esta función ya no se usa, pero la dejamos por compatibilidad
    const url = `https://impakto.live/vend/index.php?search=${encodeURIComponent(nombreCliente)}`;
    const popup = window.open(url, 'FibraTV', 'width=1200,height=800');
    
    if (popup) {
        popup.focus();
        console.log('Popup abierto para cliente:', nombreCliente);
    }
}
