


          
# CRM Sneakers

## Descripción
CRM Sneakers es una aplicación de gestión especializada para tiendas de zapatillas deportivas, desarrollada como proyecto académico para el 2º curso de Desarrollo de Aplicaciones Multiplataforma (DAM). Esta solución integral permite administrar todos los aspectos del negocio, desde el inventario hasta las relaciones con los clientes.

## 🚀 Características Principales

### 👟 Gestión de Inventario
- Control completo del stock de zapatillas
- Categorización por marca, modelo, talla y precio
- Alertas de stock bajo
- Seguimiento de tendencias de productos

### 👥 Gestión de Clientes
- Base de datos completa de clientes
- Historial de compras
- Preferencias y tallas
- Sistema de fidelización

### 💰 Gestión de Ventas
- Registro detallado de transacciones
- Generación de facturas
- Análisis de ventas por período
- Reportes de rendimiento

### 👨‍💼 Administración de Usuarios
- Diferentes niveles de acceso
- Seguimiento de actividad
- Gestión de permisos

## 💻 Tecnologías Utilizadas
- **Java**: Lenguaje principal de desarrollo
- **JavaFX**: Framework para la interfaz gráfica
- **FXML**: Diseño de interfaces de usuario
- **CSS**: Estilizado de la aplicación
- **Base de datos relacional**: Almacenamiento persistente de datos

## 📋 Requisitos del Sistema
- Java 11 o superior
- JavaFX 17 o superior
- Conexión a base de datos
- 4GB RAM mínimo
- 100MB espacio en disco

## 🔧 Instalación

1. Clona el repositorio:
```bash
git clone https://github.com/tu-usuario/crmsneakers.git
```

2. Navega al directorio del proyecto:
```bash
cd crmsneakers
```

3. Compila el proyecto:
```bash
mvn clean install
```

4. Ejecuta la aplicación:
```bash
mvn javafx:run
```

## 📝 Configuración

### Base de Datos
1. Crea una base de datos en tu sistema gestor preferido
2. Configura los parámetros de conexión en el archivo `config.properties`
3. Ejecuta el script de inicialización incluido en `/scripts/init_db.sql`

## 📊 Capturas de Pantalla

![Screenshot 1](https://i.imgur.com/f6fNzTf.png) ![Screenshot 2](https://i.imgur.com/PBIussH.png) ![Screenshot 3](https://i.imgur.com/6rtTGZi.png) ![Screenshot 4](https://i.imgur.com/lw53BlF.png)

## 🔄 Flujo de Trabajo
1. **Login**: Acceso seguro al sistema
2. **Dashboard**: Vista general del negocio
3. **Módulos específicos**: Navegación intuitiva entre funcionalidades
4. **Reportes**: Generación de informes personalizados

## 👨‍💻 Desarrollo

### Estructura del Proyecto
```
crmsneakers/
├── src/
│   ├── main/
│   │   ├── java/org/crmsneakers/
│   │   │   ├── controllers/
│   │   │   ├── models/
│   │   │   ├── db/
│   │   │   └── utils/
│   │   └── resources/
│   │       ├── css/
│   │       ├── fxml/
│   │       └── images/
│   └── test/
├── pom.xml
└── README.md
```

## 🤝 Contribución
Este proyecto es parte de un trabajo académico. Si deseas contribuir, por favor contacta primero con el autor.

## 📜 Licencia
Este proyecto está licenciado bajo [incluir licencia]

## ✍️ Autor
Alejandro Martinez Navarro
Estudiante de 2º DAM


---

*"El mejor CRM para tu negocio de sneakers"*