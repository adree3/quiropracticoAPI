# Arquitectura de Almacenamiento Documental (Cloudflare R2) - MedicoxOS

## 1. Visión General del Sistema
MedicoxOS implementa una arquitectura híbrida de alta resiliencia para la gestión documental clínica, separando de forma estricta los metadatos transaccionales del almacenamiento físico de archivos binarios:

- **MySQL (Capa de Metadatos y Auditoría)**: Gestiona la entidad `DocumentoCliente`, indexación por paciente (`id_cliente`), aislamiento multi-tenant (`clinica_id`), tipos canónicos (`tipo_documento`), subtipos y etiquetas libres (`etiqueta`), estado del flujo Saga (`estado_subida`: `PENDIENTE`, `ACTIVO`, `ERROR_SUBIDA`), trazabilidad de auditoría (`BaseAuditEntity`), cuotas de almacenamiento en bytes y control de borrado lógico por imperativo legal (`activo = false`, `fecha_eliminacion_logica`).
- **Cloudflare R2 (Capa de Almacenamiento Físico Inmutable)**: Object Storage privado 100% compatible con la API de AWS S3 con latencia ultra-baja y sin costes de egreso (*zero egress fees*). Todos los buckets son estrictamente privados. El acceso seguro se realiza mediante URLs prefirmadas temporales (*Presigned URLs*) generadas bajo demanda (JIT) con validez de 15 minutos o entrega directa en memoria Base64.
- **Paradigma Zero-Storage en Borradores**: Durante el flujo asistencial y la captura de firmas en la tablet Kiosco, los documentos no firmados se generan **exclusivamente al vuelo en memoria RAM** mediante Flying Saucer / OpenPDF (`PdfFirmaService`). Jamás se guardan en disco local del servidor VPS ni se suben a Cloudflare R2 hasta que el paciente estampa físicamente su firma. Esto evita acumular borradores huérfanos, optimiza almacenamiento y blinda la privacidad clínica.

---

## 2. Configuración e Infraestructura (Backend)

### 2.1 Variables de Entorno Requeridas
La conexión con Cloudflare R2 se centraliza en la clase de configuración `R2Config.java`, la cual inyecta cuatro propiedades esenciales y aplica una sanitización automática (`.trim()`) para neutralizar cualquier espacio accidental o retorno de carro en las credenciales:

```properties
# Cloudflare R2 (Object Storage S3-Compatible)
r2.endpoint=${R2_ENDPOINT}
r2.access-key=${R2_ACCESS_KEY}
r2.secret-key=${R2_SECRET_KEY}
r2.bucket-name=${R2_BUCKET_NAME}

# Límites de carga multipart en Spring Boot
spring.servlet.multipart.max-file-size=15MB
spring.servlet.multipart.max-request-size=15MB
```

#### Ejemplo de configuración para desarrollo local (`application-dev.properties` o `.env`):
```properties
r2.endpoint=https://2460e2c93ea269586041827ba2c6d503.r2.cloudflarestorage.com
r2.access-key=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
r2.secret-key=z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4j3i2h1g0
r2.bucket-name=medicox-storage-dev
```

En `R2Config.java`, el cliente S3 y el presigner se configuran con `Region.of("auto")` requerido por Cloudflare:
```java
@Configuration
public class R2Config {

    @Value("${r2.endpoint}")
    private String endpoint;

    @Value("${r2.access-key}")
    private String accessKey;

    @Value("${r2.secret-key}")
    private String secretKey;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        String cleanEndpoint = endpoint != null ? endpoint.trim() : "";
        String cleanAccessKey = accessKey != null ? accessKey.trim() : "";
        String cleanSecretKey = secretKey != null ? secretKey.trim() : "";

        return S3Client.builder()
                .endpointOverride(URI.create(cleanEndpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cleanAccessKey, cleanSecretKey)
                ))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        String cleanEndpoint = endpoint != null ? endpoint.trim() : "";
        String cleanAccessKey = accessKey != null ? accessKey.trim() : "";
        String cleanSecretKey = secretKey != null ? secretKey.trim() : "";

        return S3Presigner.builder()
                .endpointOverride(URI.create(cleanEndpoint))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(cleanAccessKey, cleanSecretKey)
                ))
                .build();
    }

    public String getBucketName() {
        return bucketName != null ? bucketName.trim() : "";
    }
}
```

### 2.2 Política CORS del Bucket
Para que Flutter Web (tanto en entorno de desarrollo local `http://localhost:*` como en despliegues productivos en Vercel o dominios personalizados) pueda renderizar PDFs mediante `SfPdfViewer` o imágenes sin bloqueos de seguridad Cross-Origin, el bucket de Cloudflare R2 debe tener configurada la siguiente política CORS en su panel de administración:

```json
[
  {
    "AllowedOrigins": [
      "http://localhost:*",
      "http://127.0.0.1:*",
      "https://*.vercel.app",
      "https://app.medicoxos.com"
    ],
    "AllowedMethods": [
      "GET",
      "HEAD",
      "PUT"
    ],
    "AllowedHeaders": [
      "*"
    ],
    "ExposeHeaders": [
      "ETag",
      "Content-Type",
      "Content-Length",
      "Content-Disposition"
    ],
    "MaxAgeSeconds": 3600
  }
]
```

---

## 3. Topología de Directorios (StoragePathBuilder)

### Regla de Oro del Aislamiento Multi-Tenant
Toda ruta de almacenamiento en Cloudflare R2 **debe comenzar obligatoriamente por el prefijo `clinicas/{clinicaId}/`**. 
La clase `StoragePathBuilder` aplica una validación estricta en tiempo de ejecución: si `clinicaId` es nulo o menor o igual a cero, se lanza de forma inmediata una `IllegalArgumentException`, impidiendo cualquier escritura o lectura fuera del espacio de la clínica.

### Sanitización de Caracteres
Todos los segmentos dinámicos (`servicioClean`, `categoria`, `baseName`, `originalFilename` y extensiones) pasan por el método `sanitize(String input)`:
```java
private String sanitize(String input) {
    if (input == null) return "";
    return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
            .replaceAll("[^a-zA-Z0-9._-]", "_");
}
```
Esto garantiza la eliminación de tildes, diacríticos, eñes, espacios y caracteres especiales, evitando rutas corruptas o incompatibilidades en cabeceras HTTP.

### Árbol Jerárquico de Rutas
```text
clinicas/{clinicaId}/
│
├── clientes/{idCliente}/
│   │
│   ├── justificantes/
│   │   ├── Justificante_Sesion_{idCita}.pdf                 # Sesión suelta individual firmada
│   │   └── {servicioClean}_Bono_{idBonoActivo}.pdf          # Cartilla acumulada de sesiones del bono
│   │
│   ├── pruebas_medicas/
│   │   ├── {baseName}.ext                                   # Radiografías, Resonancias, Ecografías, etc.
│   │   └── {baseName}_thumb.jpg                             # Miniatura optimizada Zero-Copy (600x600 px)
│   │
│   ├── consentimientos/
│   │   └── {baseName}.ext                                   # Consentimientos LOPD y Tratamiento firmados
│   │
│   ├── informes/
│   │   └── {baseName}.ext                                   # Informes clínicos y valoraciones médicas
│   │
│   └── documentos/
│       └── {baseName}.ext                                   # Documentos genéricos asociados al cliente
│
├── facturacion/
│   └── {yyyy}/{mm}/
│       └── {idCliente}_{idDocumento}.ext                    # Justificantes de cobro y pagos (retención fiscal)
│
├── usuarios/{idUsuario}/
│   └── perfil/
│       └── foto_perfil.ext                                  # Foto de perfil de quiroprácticos/empleados
│
└── uploads/
    └── {timestamp}_{originalFilename}                       # Cargas temporales controladas
```

---

## 4. Flujos Críticos de Negocio

### 4.1 Captura de Firmas Kiosco (Justificantes y LOPD)
El proceso de firma digital desde la tablet Kiosco sigue un flujo altamente optimizado que combina WebSockets (STOMP), generación en memoria y transferencia directa en Base64:

```
[Dashboard Recepción]       [Backend Spring Boot]          [Tablet Kiosco (Flutter)]
        │                             │                                │
        │── POST /solicitar-firma ───>│                                │
        │                             │── STOMP /topic/kiosk ─────────>│ (Abre pantalla firma)
        │                             │   {action: "OPEN_SIGNATURE"}   │
        │                             │                                │
        │                             │<── GET /{id}/borrador-pdf ─────│ (Zero-Storage RAM)
        │                             │── Stream PDF en bytes ────────>│ Renderiza SfPdfViewer.memory
        │                             │                                │
        │                             │                                │ [Paciente realiza rúbrica]
        │                             │<── POST /{id}/firmar ──────────│ Envía firmaBase64
        │                             │    (Genera PDF definitivo)     │
        │                             │    (Guarda en R2 + MySQL)      │
        │                             │── Responde JSON ──────────────>│ Recibe signedPdfBase64
        │                             │   {signedPdfBase64: "..."}     │
        │                             │                                │ [Renderiza al instante sin GET a R2]
        │<── STOMP /topic/citas ──────│                                │ [Muestra pantalla de agradecimiento]
        │    {action: "CITA_FIRMADA"} │                                │
```

1. **Apertura vía WebSocket**: El operador solicita la firma desde la vista de Citas. El backend emite a `/topic/kiosk` el payload con los datos de la cita (`idCita`, `nombrePaciente`, `fecha`, `horaInicio`, `esBono`, etc.).
2. **Generación al Vuelo del Borrador (`GET /api/citas/{id}/borrador-pdf`)**: La tablet Kiosco (`kiosk_view.dart`) solicita el borrador en memoria. `CitaServiceImpl.generarBorradorPdf` procesa la plantilla Thymeleaf mediante OpenPDF y retorna los bytes sin tocar almacenamiento físico. La tablet lo muestra con `SfPdfViewer.memory(_pdfBytes)`.
3. **Captura y Envío de la Rúbrica**: El paciente firma con el lápiz/dedo en el canvas (`SignatureController`). Al pulsar confirmar, Flutter convierte el trazo a PNG Base64 y realiza `POST /api/citas/{id}/firmar`.
4. **Almacenamiento Físico y Retorno Base64 (Zero-Second-Call)**:
   - `CitaServiceImpl.procesarFirma` actualiza la cita a `completada` y `firmada = true`.
   - Genera el PDF definitivo con la rúbrica estampada, lo persiste en R2 (`storeBytes`) y registra metadatos en `DocumentoCliente` con `tipoDocumento = TipoDocumento.JUSTIFICANTE_ASISTENCIA`.
   - El endpoint responde un Map con la clave `"signedPdfBase64"`.
   - `kiosk_view.dart` decodifica la cadena con `base64Decode` y actualiza inmediatamente la pantalla con `SfPdfViewer.memory(signedBytes)` sin realizar ninguna llamada HTTP secundaria contra Cloudflare R2.

### 4.2 Categorización y Etiquetas (UI)

#### Enum Canónico `TipoDocumento`
Frontend (`tipo_documento.dart`) y Backend (`TipoDocumento.java`) comparten exactamente los 7 valores oficiales:
- `PRUEBA_MEDICA`: Pruebas diagnósticas e imágenes médicas (radiografías, resonancias, ecografías).
- `JUSTIFICANTE_ASISTENCIA`: Justificantes de citas individuales o bonos acumulados firmados.
- `CONSENTIMIENTO_LOPD`: Documento firmado de protección de datos personales (RGPD).
- `CONSENTIMIENTO_TRATAMIENTO`: Consentimiento informado para ajustes quiroprácticos.
- `INFORME_MEDICO`: Informes clínicos de derivación o evolución del paciente.
- `FACTURACION`: Justificantes de cobro y recibos de pago.
- `OTRO`: Documentos administrativos generales.

#### Flexibilidad con el campo `etiqueta` (VARCHAR 100)
Para evitar la proliferación de constantes fijas en el enum para cada subtipo de prueba médica, el sistema utiliza el campo libre `etiqueta`:
- Un archivo se sube con `tipoDocumento = PRUEBA_MEDICA`.
- En el campo `etiqueta` se almacena el subtipo clínico real (ej. *"Radiografía Cervical Lateral"*, *"Resonancia Lumbar L5-S1"*, *"Informe Traumatología"*).
- El backend almacena esta etiqueta en `DocumentoCliente` y la devuelve en `DocumentoDto`.

#### Carpetas Virtuales en Flutter
En `cliente_archivos_tab.dart` y `document_folder_grid.dart`, los archivos no se estructuran en carpetas físicas rígidas en el servidor, sino que el frontend construye dinámicamente **Carpetas Lógicas**:
- La clase `CarpetaLogica` evalúa cada `Documento` mediante `matches(Documento d, List<CarpetaLogica> allFolders)`.
- La UI organiza visualmente:
  - **Imágenes Médicas** (`PRUEBA_MEDICA`) con filtro rápido por `etiqueta`.
  - **Citas Firmadas** (`JUSTIFICANTE_ASISTENCIA`).
  - **Consentimientos** (`CONSENTIMIENTO_LOPD` y `CONSENTIMIENTO_TRATAMIENTO`).
  - **Informes Clínicos** (`INFORME_MEDICO`).
  - **Facturación** (`FACTURACION`).
  - **Otros Archivos** (`OTRO`).
  - **Papelera** (documentos con `activo = false`, permitiendo su restauración).

---

## 5. Prevención de Riesgos y Reglas de Oro

1. **Aislamiento Multi-Tenant Estricto**:
   Jamás construyas rutas manuales mediante concatenación directa de cadenas. Utiliza siempre `StoragePathBuilder` y verifica que `clinicaId` provenga del contexto autenticado (`TenantContext.getTenantId()`).
2. **Descarga de URLs Prefirmadas en Flutter (Clean Dio Instance)**:
   Al descargar o previsualizar un archivo desde una URL prefirmada de Cloudflare R2, **nunca utilices la instancia principal de `ApiService.dio`**. Los interceptores inyectan la cabecera `Authorization: Bearer <JWT>`, lo cual provoca que AWS S3 / Cloudflare R2 rechace la petición con un error `400 Bad Request` debido a la colisión entre la firma HMAC en los query params y la cabecera de autenticación. Utiliza siempre una instancia limpia: `final cleanDio = Dio();`.
3. **Inmutabilidad de Rutas en R2**:
   Nunca modifiques el valor de `path_archivo` en la base de datos sin haber movido o copiado físicamente el objeto en Cloudflare R2; de lo contrario, se generarán referencias huérfanas en el sistema.
4. **Patrón Saga Sin `@Transactional` Global en Subidas**:
   Las operaciones de red hacia Cloudflare R2 no deben ejecutarse dentro de un bloque `@Transactional` de base de datos. Mantener una transacción SQL abierta durante una subida HTTP bloquea conexiones del pool HikariCP. Aplica el patrón Saga: guarda metadatos en estado tentativo (`PENDIENTE`), sube a R2 en bloque try-catch aislado y actualiza el estado a `ACTIVO` o captura la excepción pasando a `ERROR_SUBIDA`.
5. **RGPD y Borrado Lógico Obligatorio**:
   Por normativa de la Ley de Autonomía del Paciente y el RGPD, los documentos clínicos **nunca se eliminan físicamente de Cloudflare R2** en operaciones ordinarias de usuario. Se realiza borrado lógico (`activo = false`, `fecha_eliminacion_logica = now()`). Solo un proceso legal expreso de derecho al olvido autoriza la destrucción física del objeto en R2.
6. **Sanitización Previa de Caracteres**:
   Ningún nombre de archivo o prefijo enviado por el usuario puede llegar virgen a R2. Debe pasar siempre por `Normalizer` para eliminar caracteres diacríticos, acentos o espacios antes de invocar `putObject` o `storeBytes`.