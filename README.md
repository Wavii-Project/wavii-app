# Wavii — Tu música. Tu ritmo. Tu comunidad.

[![Expo](https://img.shields.io/badge/Expo-SDK%2054-000020?style=for-the-badge&logo=expo&logoColor=white)](https://expo.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

**Wavii** es una plataforma integral de aprendizaje musical gamificado diseñada como Proyecto de Fin de Grado (TFG) para DAM. Combina una experiencia móvil moderna con un potente backend escalable, integración de pagos con Stripe y gestión de certificaciones a través de Odoo.

---

##  Módulos del Proyecto

El repositorio está organizado como un monorepo que contiene todas las piezas del ecosistema Wavii:

-   **`frontend/`**: Aplicación móvil multiplataforma desarrollada con **Expo SDK 54** y React Native.
-   **`backend/`**: API REST robusta construida con **Spring Boot 3**, gestionando autenticación, lógica de negocio y persistencia dual (SQL/NoSQL).
-   **`docker/`**: Orquestación completa del entorno de desarrollo y producción (Base de Datos, Servidor, Proxy, ERP).
-   **`admin/`**: (En desarrollo) Panel de administración para la gestión de contenidos y usuarios.

---

##  Características Principales

-   **Aprendizaje Gamificado**: Sistema de experiencia (XP), rachas y niveles (Principiante, Intermedio, Avanzado).
-   **Suscripciones Flexibles**: Integración con **Stripe** para planes Free, Plus y Scholar (Educativo).
-   **Verificación de Profesores**: Flujo de validación de documentos integrado con **Odoo ERP**.
-   **Comunidad y Clases**: Gestión de tablaturas, clases particulares y tablones de anuncios.
-   **Seguridad**: Autenticación mediante **JWT**, verificación de email y recuperación de contraseña.
-   **Notificaciones**: Comunicación vía Email (SMTP) y SMS (Twilio).

---

##  Stack Tecnológico

### Frontend
-   **Framework**: React Native / Expo SDK 54 (TypeScript).
-   **Estado Global**: Zustand.
-   **Diseño**: Sistema de tokens propio basado en Vanilla CSS, fuente Nunito e Ionicons.
-   **Navegación**: React Navigation v7.
-   **API**: Axios con interceptores para JWT.

### Backend
-   **Lenguaje**: Java 17 / Spring Boot 3.2.5.
-   **Persistencia**: 
    -   **PostgreSQL**: Datos relacionales (Usuarios, Tokens, Suscripciones).
    -   **MongoDB**: Datos documentales (Lecciones, Contenido musical).
-   **Seguridad**: Spring Security + JJWT.
-   **Integraciones**: Stripe API, Twilio SDK, JavaMail, Odoo XML-RPC.
-   **Testing**: JUnit 5, Mockito, JaCoCo (100% coverage goal).

### Infraestructura
-   **Contenedores**: Docker & Docker Compose.
-   **Proxy Inverso**: Nginx.
-   **ERP/CRM**: Odoo 17.

---

##  Configuración y Arranque

### Requisitos Previos
-   Node.js v20+
-   Docker & Docker Desktop
-   Java 17 & Maven (para desarrollo local fuera de Docker)
-   App **Expo Go** en el dispositivo móvil

### 1. Levantar la Infraestructura (Docker)
Navega a la carpeta de docker y arranca los servicios:
```bash
cd docker
# Copia el ejemplo de env y edítalo con tus credenciales
cp .env.example .env 
docker compose up -d
```
Esto levantará PostgreSQL, MongoDB, el Backend, Nginx y Odoo.

### 2. Configuración del Backend
Si prefieres correr el backend localmente sin Docker:
```bash
cd backend
mvn spring-boot:run
```
*Nota: Asegúrate de configurar las variables de entorno en tu IDE o sistema.*

### 3. Arranque del Frontend
```bash
cd frontend
npm install
npx expo start
```
Escanea el código QR desde la app **Expo Go** para ver la aplicación en tu móvil.

---

## 📦 Estructura del Repositorio

```bash
wavii/
├── frontend/          # Expo / React Native (TypeScript)
│   ├── src/api/       # Llamadas al backend
│   ├── src/theme/     # Tokens de diseño (Colores, Spacing)
│   └── src/screens/   # Pantallas de la aplicación
├── backend/           # Spring Boot 3 / Java 17
│   ├── src/main/      # Código fuente y recursos
│   └── src/test/      # Tests unitarios e integración
├── docker/            # Configuraciones de despliegue
│   ├── nginx/         # Configuración del proxy
│   └── odoo/          # Configuración del ERP

```

---

## 📄 Licencia

Este proyecto es parte del Proyecto Intermodular de 2ºDAM y se distribuye bajo fines educativos.

---
Desarrollado con ❤️ por **Eduardo G. y Daniel R**
