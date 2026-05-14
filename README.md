# 📱 Cuidado Compartido — SeniorCareConnect

> Aplicación Android nativa para la coordinación del cuidado de adultos mayores con enfermedades crónicas.

---

## 📋 Descripción

**Cuidado Compartido** es una aplicación móvil Android que permite a cuidadores formales e informales coordinar y dar seguimiento a la salud de adultos mayores. La app conecta a cuidadores con sus pacientes mediante un sistema de invitación por código o QR, y centraliza el registro de medicamentos, signos vitales, estados de ánimo, agenda médica y alertas de emergencia.

---

## ✨ Funcionalidades Principales

### 👨‍⚕️ Perfil Caregiver (Cuidador)
- **Dashboard** con resumen diario: medicamentos del día, próximos eventos y últimas lecturas médicas de todos los adultos mayores conectados
- **Gestión de Medicamentos** con wizard de registro por frecuencia (diaria, semanal, mensual, por intervalos, etc.), recordatorios y notificaciones
- **Historial de medicamentos** con estado: A tiempo, Omitido, Atrasado
- **Diario de Salud**: registro de signos vitales (pulso, glucosa, temperatura, presión, saturación, peso), estados de ánimo, síntomas, y niveles de apetito, energía y capacidad funcional
- **Gráficas de tendencias** con análisis por periodo y top 5 emociones predominantes
- **Agenda**: citas médicas y tareas cotidianas (asignables entre cuidadores)
- **Conexión con adultos mayores** mediante código de 6 dígitos o escaneo QR

### 👴 Perfil Elderly (Adulto Mayor)
- **Dashboard** con medicamentos pendientes del día y próximas citas médicas
- **Botón de emergencia** que envía notificación con ubicación a todos sus cuidadores
- **Generación de códigos QR** de invitación para conectar cuidadores
- **Gestión de solicitudes** de conexión (aceptar/rechazar cuidadores)

### 🔐 Autenticación
- Registro con selección de tipo de usuario (Caregiver / Elderly)
- Inicio de sesión con correo y contraseña
- Sesión persistente

---

## 🛠️ Stack Tecnológico

| Categoría | Tecnología |
|---|---|
| **Lenguaje** | Kotlin 100% |
| **IDE** | Android Studio |
| **Arquitectura** | MVVM + Repository Pattern |
| **UI** | XML Layouts + Material Design Components |
| **Navegación** | Navigation Component (Safe Args) |
| **Base de datos** | Firebase Firestore (NoSQL) |
| **Autenticación** | Firebase Authentication |
| **Imágenes** | Cloudinary (via OkHttp REST) + Glide |
| **Notificaciones** | WorkManager + AlarmManager + NotificationCompat |
| **Gráficas** | MPAndroidChart |
| **QR** | ZXing Android Embedded |
| **Async** | Kotlin Coroutines + LiveData |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 35 |

**Principios de arquitectura:**
- Single Activity con múltiples Fragments
- Separación clara de capas: `UI → ViewModel → Repository → Firebase`
- LiveData para observación reactiva de datos
- Inyección de dependencias manual (sin Hilt)

---

## 📂 Estructura del Proyecto

```
app/src/main/java/com/isa/cuidadocompartidomayor/
│
├── data/
│   ├── model/          # Modelos de datos (User, Medication, VitalSign, MoodEntry, etc.)
│   └── repository/     # Repositorios por dominio (Auth, Medication, Diary, Agenda, etc.)
│
├── notifications/
│   └── MedicationAlarmReceiver.kt
│
├── ui/
│   ├── auth/           # Login, Register, Welcome
│   ├── caregiver/      # Dashboard, conexión con Elderly, lista de pacientes
│   ├── medications/    # Lista, historial, wizard de registro
│   ├── diary/          # Signos vitales, estados de ánimo, tendencias
│   ├── agenda/         # Citas médicas y tareas
│   ├── elderly/        # Dashboard Elderly, emergencia, códigos QR, solicitudes
│   └── profile/        # Edición de perfil y configuración
│
├── utils/              # Helpers: alarmas, notificaciones, gráficas, PDF, QR, etc.
├── MedicationApp.kt
└── MainActivity.kt
```

---

## ⚙️ Configuración del Proyecto

### Requisitos previos
- Android Studio Hedgehog o superior
- JDK 21
- Cuenta en [Firebase](https://firebase.google.com/) con proyecto configurado
- Cuenta en [Cloudinary](https://cloudinary.com/) con upload preset configurado

### 1. Clonar el repositorio

```bash
git clone https://github.com/Isay444/SenniorCareConnect.git
cd SenniorCareConnect
```

### 2. Configurar Firebase

Coloca tu archivo `google-services.json` en la carpeta `app/`:

```
app/
└── google-services.json   ← aquí
```

> ⚠️ Este archivo **no está incluido** en el repositorio por seguridad. Descárgalo desde tu consola de Firebase.

### 3. Configurar Cloudinary

Agrega las siguientes variables en tu archivo `local.properties` (en la raíz del proyecto):

```properties
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_UPLOAD_PRESET=tu_upload_preset
```

> ⚠️ El archivo `local.properties` **no está incluido** en el repositorio. Créalo manualmente.

### 4. Sincronizar y ejecutar

Abre el proyecto en Android Studio, sincroniza Gradle y ejecuta en un emulador o dispositivo físico con Android 8.0+.

---

## 🔥 Firebase

### Colecciones en Firestore
El proyecto utiliza **11 colecciones**:

`users` · `medications` · `medicationLogs` · `vital_signs` · `mood_entries` · `events` · `relationships` · `caregiverRequests` · `inviteCodes` · `caregiverNotifications` · `emergencyAlerts`

### Reglas de Seguridad
Actualmente configuradas para permitir lectura/escritura solo a usuarios autenticados:
```
allow read, write: if request.auth != null;
```

### Autenticación habilitada
- Correo electrónico / contraseña ✅

---

## 🗺️ Roadmap

- [ ] Implementar generación de reportes PDF/Excel
- [ ] Mejorar implementación de modo oscuro (themes, styles, colors)
- [ ] Bloquear orientación horizontal en toda la app
- [ ] Completar módulo de configuración (actualmente solo cierra sesión)
- [ ] Deshabilitar botón de emergencia si Elderly no tiene cuidadores conectados
- [ ] Mejorar navegación del perfil Elderly (back stack)
- [ ] Eliminar datos asociados al borrar cuenta de usuario
- [ ] Rediseño del dashboard de Caregiver

---

## 📸 Capturas de Pantalla

- Tomar medicamento

<img width="480" height="270" alt="Screen-TomarMedicamento" src="https://github.com/user-attachments/assets/1cb12417-f29c-461e-a9a4-d47b4d00fe4b" />

- Generar Conexion Caregiver-Elderly

<img width="480" height="270" alt="Screen-Codigos" src="https://github.com/user-attachments/assets/08eda9a8-3801-4f92-9129-42daa65a408f" />

- Página principal Cuidador

<img width="270" height="600" alt="Screen-DashbpardCuidador-Parte1" src="https://github.com/user-attachments/assets/7a66b9c8-6dd2-4792-b582-ce00fa6b506a" />
<img width="270" height="600" alt="Screen-DashboardCuidador-Parte2" src="https://github.com/user-attachments/assets/e61f700e-ba46-4257-914b-1802684e4eb1" />

- Lista + Historial de Medicamentos

<img width="270" height="600" alt="Screen-Modulo-MedicamentosLista" src="https://github.com/user-attachments/assets/c7b72487-16a9-4eda-a8bc-888ea5234c1e" />
<img width="270" height="600" alt="Screen-Modulo-MedicamentosHisotrial" src="https://github.com/user-attachments/assets/244aa2cb-3f28-4f51-ac75-1d460f8eda33" />

- Modulo de datos clinicos
<img width="1920" height="1080" alt="Screen-ModuloDiarioSalud" src="https://github.com/user-attachments/assets/010e7a42-6f5f-4b96-8469-5c9df6649a1a" />

---

Este proyecto es de uso personal/académico.
