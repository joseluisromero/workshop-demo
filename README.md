# Workshop Demo - AWS Microservicio de Procesamiento de Órdenes

Microservicio Spring Boot que procesa órdenes de compra integrando múltiples servicios de AWS: SQS, PostgreSQL (Aurora DSQL), S3 y SNS.

## Características

- **Spring Boot 3.2.2** con Java 17
- **Gradle** como gestor de dependencias
- **Lombok** para reducir código boilerplate
- **Spring Cloud AWS** para integración con SQS
- **AWS SDK** para S3 y SNS
- **Spring Data JDBC** para persistencia
- **PostgreSQL** (Aurora DSQL) con autenticación IAM
- **Jackson** para procesamiento JSON

## Arquitectura del Flujo

El microservicio implementa el siguiente flujo de procesamiento:

1. **Recepción de Mensajes (SQS)**: Escucha mensajes de una cola SQS con información de órdenes
2. **Persistencia (PostgreSQL)**: Guarda la orden en una base de datos Aurora DSQL
3. **Generación de Factura (S3)**: Crea un archivo de factura en formato texto y lo sube a S3
4. **Notificación (SNS)**: Envía el contenido de la factura a un tópico SNS

```
SQS → Validación → PostgreSQL → S3 → SNS
```

## Configuración

### Variables de Entorno

Configura las siguientes variables de entorno o propiedades en `application.yml`:

**AWS Credentials:**
- `AWS_ACCESS_KEY_ID`: Tu AWS Access Key
- `AWS_SECRET_ACCESS_KEY`: Tu AWS Secret Key
- `AWS_REGION`: Región de AWS (ej: us-east-1)

**SQS:**
- `SQS_QUEUE_NAME`: Nombre de la cola SQS
- `SQS_QUEUE_URL`: URL completa de la cola SQS

**Database:**
- `DB_URL`: JDBC URL de la base de datos Aurora DSQL
- `DB_USERNAME`: Usuario de la base de datos
- `DB_PASSWORD`: Token IAM para autenticación (se renueva automáticamente)

**S3:**
- `S3_BUCKET_NAME`: Nombre del bucket S3 para almacenar facturas
- `S3_INVOICE_PREFIX`: Prefijo para los archivos de facturas (ej: invoices)

**SNS:**
- `SNS_TOPIC_ARN`: ARN del tópico SNS para notificaciones

### application.yml

```yaml
aws:
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
  region:
    static: ${AWS_REGION:us-east-1}
  sqs:
    queue-name: ${SQS_QUEUE_NAME}
    queue-url: ${SQS_QUEUE_URL}
  s3:
    bucket-name: ${S3_BUCKET_NAME}
    invoice-prefix: ${S3_INVOICE_PREFIX:invoices}
  sns:
    topic-arn: ${SNS_TOPIC_ARN}

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
```

## Permisos IAM Requeridos

El usuario IAM necesita los siguientes permisos:

**SQS:**
```json
{
  "Effect": "Allow",
  "Action": [
    "sqs:ReceiveMessage",
    "sqs:DeleteMessage",
    "sqs:GetQueueAttributes"
  ],
  "Resource": "arn:aws:sqs:REGION:ACCOUNT_ID:QUEUE_NAME"
}
```

**S3:**
```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:GetObject"
  ],
  "Resource": "arn:aws:s3:::BUCKET_NAME/*"
}
```

**SNS:**
```json
{
  "Effect": "Allow",
  "Action": [
    "sns:Publish"
  ],
  "Resource": "arn:aws:sns:REGION:ACCOUNT_ID:TOPIC_NAME"
}
```

**RDS (Aurora DSQL):**
```json
{
  "Effect": "Allow",
  "Action": [
    "rds-db:connect"
  ],
  "Resource": "arn:aws:rds-db:REGION:ACCOUNT_ID:dbuser:*"
}
```

## Formato de Mensaje

Los mensajes en SQS deben seguir este formato JSON:

```json
{
  "email": "cliente@example.com",
  "products": "Producto 1, Producto 2, Producto 3"
}
```

**Campos obligatorios:**
- `email`: Email del cliente
- `products`: Descripción de los productos comprados

## Base de Datos

### Esquema

El microservicio utiliza el siguiente esquema en PostgreSQL:

```sql
CREATE SCHEMA IF NOT EXISTS workshop;

CREATE TABLE IF NOT EXISTS workshop.orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    products TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

## Ejecución

### Compilar el proyecto

```bash
./gradlew clean build
```

### Ejecutar la aplicación

```bash
./gradlew bootRun
```

O con variables de entorno:

```bash
export AWS_ACCESS_KEY_ID=tu-key
export AWS_SECRET_ACCESS_KEY=tu-secret
export AWS_REGION=us-east-1
export SQS_QUEUE_NAME=mi-cola
export SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/mi-cola
export S3_BUCKET_NAME=mi-bucket
export SNS_TOPIC_ARN=arn:aws:sns:us-east-1:123456789012:mi-topico
export DB_URL=jdbc:postgresql://host:5432/postgres
export DB_USERNAME=admin
export DB_PASSWORD=password

./gradlew bootRun
```

## Funcionamiento Detallado

### 1. Recepción de Mensajes (SQS)

El `SqsMessageListener` escucha mensajes de la cola configurada:
- Valida que los campos obligatorios estén presentes
- Parsea el JSON del mensaje
- Captura y registra errores de validación

### 2. Persistencia (PostgreSQL)

La orden validada se guarda en la base de datos:
- Genera un UUID como identificador único
- Registra la fecha y hora de creación
- Almacena el email y productos

### 3. Generación de Factura (S3)

El `InvoiceService` genera una factura en formato texto:
- Formato: `{prefix}_{orderId}_{timestamp}.txt`
- Incluye: ID de orden, fecha, email del cliente y productos
- Sube directamente a S3 sin escribir localmente

**Ejemplo de factura:**
```
========================================
           FACTURA DE COMPRA            
========================================

ID Orden: 88c6f32f-758f-4d7d-973b-69cfad29aeb6
Fecha: 03/02/2026 16:39:29
Cliente: cliente@example.com

========================================
              PRODUCTOS                 
========================================

Producto 1, Producto 2, Producto 3

========================================
   Gracias por su compra!              
========================================
```

### 4. Notificación (SNS)

El `NotificationService` envía el contenido de la factura al tópico SNS:
- Subject: "Nueva Factura Generada - Orden {orderId}"
- Message: Contenido completo de la factura
- Permite notificaciones por email, SMS o Lambda

## Estructura del Proyecto

```
workshop-demo/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/aws/workshop/
│       │       ├── WorkshopDemoApplication.java
│       │       ├── config/
│       │       │   ├── AwsSqsConfig.java
│       │       │   ├── AwsS3Config.java
│       │       │   ├── AwsSnsConfig.java
│       │       │   └── JacksonConfig.java
│       │       ├── dto/
│       │       │   └── OrderMessage.java
│       │       ├── listener/
│       │       │   └── SqsMessageListener.java
│       │       ├── model/
│       │       │   └── Order.java
│       │       ├── repository/
│       │       │   └── OrderRepository.java
│       │       └── service/
│       │           ├── InvoiceService.java
│       │           └── NotificationService.java
│       └── resources/
│           └── application.yml
├── build.gradle
└── README.md
```

## Logging

El microservicio incluye logging detallado en cada etapa:
- Nivel `DEBUG` para componentes del workshop
- Nivel `INFO` para logs generales
- Logs de errores con stack traces completos

## Manejo de Errores

- **Validación**: Errores de validación lanzan `IllegalArgumentException`
- **Base de datos**: Errores de BD se registran y propagan
- **S3**: Errores de carga a S3 se registran con detalles
- **SNS**: Errores de publicación se registran y propagan
- **SQS**: Los mensajes que fallan permanecen en la cola para reintentos

## Tecnologías Utilizadas

- **Spring Boot 3.2.2**: Framework principal
- **Spring Cloud AWS 3.1.0**: Integración con servicios AWS
- **AWS SDK v2**: Clientes para S3 y SNS
- **PostgreSQL Driver**: Conectividad con Aurora DSQL
- **Jackson**: Serialización/deserialización JSON
- **Lombok**: Reducción de código boilerplate
- **SLF4J/Logback**: Sistema de logging
