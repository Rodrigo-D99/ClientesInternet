// ============================================================
// FUNCIONES DE WHATSAPP
// ============================================================

// Variables globales para el modal
let whatsappData = {
    telefono: '',
    mensaje: ''
};

/**
 * Abre WhatsApp Web con un mensaje preconfigurado
 * @param {string} telefono - Número de teléfono del cliente
 * @param {string} mensaje - Mensaje a enviar
 */
function abrirWhatsapp(telefono, mensaje) {
    if (!telefono) {
        alert("El cliente no tiene teléfono registrado");
        return;
    }

    // Limpiar: solo números
    let tel = telefono.replace(/\D/g, "");

    // Asegurar formato Argentina: 549 + número
    if (!tel.startsWith("549")) {
        if (tel.startsWith("9")) {
            tel = "54" + tel;
        } else {
            tel = "549" + tel;
        }
    }

    const url = `https://wa.me/${tel}?text=${encodeURIComponent(mensaje)}`;
    window.open(url, "_blank");
}

/**
 * Abre modal para editar y confirmar mensaje antes de enviarlo
 * @param {string} telefono - Número de teléfono del cliente
 * @param {string} mensaje - Mensaje inicial a enviar
 */
function abrirModalWhatsapp(telefono, mensaje) {
    // Guardar datos globales
    whatsappData.telefono = telefono;
    whatsappData.mensaje = mensaje;

    // Cargar mensaje en textarea del modal
    document.getElementById('mensajeWhatsapp').value = mensaje;

    // Mostrar modal
    const modal = new bootstrap.Modal(document.getElementById('modalWhatsapp'));
    modal.show();
}

/**
 * Enviar mensaje desde el modal (permitiendo edición)
 */
function enviarWhatsappDesdeModal() {
    const mensajeEditado = document.getElementById('mensajeWhatsapp').value;

    if (!mensajeEditado.trim()) {
        alert("El mensaje no puede estar vacío");
        return;
    }

    // Cerrar modal
    const modal = bootstrap.Modal.getInstance(document.getElementById('modalWhatsapp'));
    modal.hide();

    // Abrir WhatsApp con el mensaje editado
    abrirWhatsapp(whatsappData.telefono, mensajeEditado);

    // Limpiar datos
    whatsappData = { telefono: '', mensaje: '' };
}

/**
 * Restaurar mensaje original en el modal
 */
function restaurarMensajeOriginal() {
    document.getElementById('mensajeWhatsapp').value = whatsappData.mensaje;
}

/**
 * Avisa a un cliente sobre su deuda pendiente
 * @param {string} nombre - Nombre del cliente
 * @param {string} telefono - Teléfono del cliente
 * @param {number} meses - Cantidad de meses adeudados
 */
function avisarDeuda(nombre, telefono, meses) {
    const mensaje = `Hola ${nombre} 

Te recordamos que tenes ${meses} mes(es) adeudado(s) del servicio de internet.
A partir del día 21 se procederá al corte del servicio si no se regulariza el pago.
Podes regularizar tu cuenta cuando quieras 
¡Gracias!`;

    abrirModalWhatsapp(telefono, mensaje);
}

/**
 * Avisa a un cliente sobre su deuda con meses específicos
 * @param {string} nombre - Nombre del cliente
 * @param {string} telefono - Teléfono del cliente
 * @param {number} meses - Cantidad de meses adeudados
 */
function avisarDeudaDetallado(nombre, telefono, meses) {
    const mesesTexto = meses === 1 ? "1 mes" : `${meses} meses`;
    
    const mensaje = `Hola ${nombre} 

Te recordamos que tenés ${mesesTexto} adeudado(s) del servicio de internet.
Fecha de corte próxima: 21 del mes actual
¡Regularizá tu cuenta para mantener el servicio activo! 
¡Gracias!`;

    abrirModalWhatsapp(telefono, mensaje);
}
