// ============================================================
// FUNCIONES DE TEMA Y CONTADORES
// ============================================================

// Inicializar tema desde localStorage
function inicializarTema() {
    const temaGuardado = localStorage.getItem('tema-app') || 'light';
    aplicarTema(temaGuardado);
    actualizarBotonTema();
}

// Aplicar tema
function aplicarTema(tema) {
    const html = document.documentElement;
    html.setAttribute('data-bs-theme', tema);
    localStorage.setItem('tema-app', tema);
}

// Toggle de tema
function toggleTema() {
    const html = document.documentElement;
    const temaActual = html.getAttribute('data-bs-theme');
    const nuevoTema = temaActual === 'light' ? 'dark' : 'light';
    aplicarTema(nuevoTema);
    actualizarBotonTema();
}

// Actualizar icono del botón de tema
function actualizarBotonTema() {
    const html = document.documentElement;
    const temaActual = html.getAttribute('data-bs-theme');
    const btn = document.getElementById('btnTheme');
    
    if (btn) {
        btn.innerHTML = temaActual === 'light' ? '🌙' : '☀️';
        btn.title = temaActual === 'light' ? 'Modo oscuro' : 'Modo claro';
    }
}

// Actualizar contadores
function actualizarContadores(clientes) {
    if (!clientes || clientes.length === 0) {
        document.getElementById('countDeudores').textContent = '0';
        document.getElementById('countAlDia').textContent = '0';
        return;
    }

    const deudores = clientes.filter(c => c.mesesAdeudados > 0).length;
    const alDia = clientes.filter(c => c.mesesAdeudados === 0).length;

    document.getElementById('countDeudores').textContent = deudores;
    document.getElementById('countAlDia').textContent = alDia;
}

// Listener para el botón de tema
document.addEventListener("DOMContentLoaded", () => {
    const btnTheme = document.getElementById('btnTheme');
    if (btnTheme) {
        btnTheme.addEventListener('click', toggleTema);
    }
    
    // Inicializar tema
    inicializarTema();
});
