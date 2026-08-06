# 🌐 Sistema de Gestión de Clientes de Internet

Un sistema web local desarrollado para facilitar la administración de clientes, contratos y pagos para servicios de internet. Diseñado para ejecutarse de forma ágil y sin necesidad de configuraciones complejas de servidores de base de datos.

---

## 🚀 Funcionalidades Principales

* **Gestión de Clientes:** Alta, edición, consulta y control del estado de los clientes abonados.
* **Control de Pagos:** Registro y seguimiento del historial de pagos y cuotas del servicio.
* **Base de Datos Autónoma:** Utiliza SQLite (`clientes.db`), lo que permite guardar la información localmente sin instalar motores de base de datos adicionales.
* **Inicio Automático:** Incluye un script ejecutable (`ejecutar.bat`) que inicia el servidor Java y abre la interfaz gráfica en el navegador automáticamente.

---

## 🛠️ Tecnologías Utilizadas

* **Backend:** Java 17, Spring Boot, Spring Data JPA
* **Base de Datos:** SQLite
* **Frontend:** HTML5, CSS3, JavaScript (ES6+)
* **Gestor de Dependencias:** Maven

---

## 📋 Requisitos Previos

Para ejecutar la aplicación solo se requiere:
* **Java JDK/JRE 17** o superior instalado y configurado en las variables de entorno del sistema.

---

## ⚙️ Cómo Ejecutar la Aplicación

1. Descarga o clona este repositorio en tu computadora.
2. Haz doble clic en el archivo `ejecutar.bat` ubicado en la carpeta principal.
3. El script compilará el proyecto (si es la primera vez), iniciará el servidor local y abrirá la aplicación en tu navegador web predeterminado en la dirección:
   `http://localhost:8080/PagClientes.html`

> ⚠️ **Nota:** No cierres la ventana de la consola de comandos mientras estés utilizando el sistema.