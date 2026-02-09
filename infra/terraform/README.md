# Workshop Demo - Infraestructura Terraform

Este directorio contiene la configuración de Terraform para desplegar una función Lambda que permite enviar mensajes a la cola SQS utilizada por el microservicio Workshop Demo.

## 📋 Requisitos Previos

Antes de ejecutar Terraform, asegúrate de tener instalado:

1. **Terraform** (versión 1.0 o superior)
   - Descarga desde: https://www.terraform.io/downloads
   - Verifica la instalación: `terraform version`

2. **Credenciales de AWS**
   - Access Key ID
   - Secret Access Key
   - Permisos necesarios: Lambda, IAM, SQS, CloudWatch Logs

3. **Cola SQS existente**
   - Nombre predeterminado: `workshop-demo-queue`
   - Si usas un nombre diferente, actualízalo en `terraform.tfvars`

## 🚀 Configuración

### 1. Crear archivo de variables

Crea un archivo `terraform.tfvars` en el directorio `infra/terraform` con tus credenciales:

```hcl
aws_access_key = "TU_ACCESS_KEY_AQUI"
aws_secret_key = "TU_SECRET_KEY_AQUI"
aws_region     = "us-east-1"
sqs_queue_name = "workshop-demo-queue"
project_prefix = "workshop-demo-joromero"
```

**⚠️ IMPORTANTE:** Nunca subas el archivo `terraform.tfvars` a git. Ya está incluido en `.gitignore`.

### 2. Inicializar Terraform

Desde el directorio `infra/terraform`, ejecuta:

```bash
terraform init
```

Este comando:
- Descarga el proveedor de AWS
- Inicializa el backend de Terraform
- Prepara el directorio de trabajo

### 3. Revisar el plan de ejecución

Antes de aplicar cambios, revisa lo que Terraform va a crear:

```bash
terraform plan
```

Esto mostrará:
- ✅ Recursos que se crearán (función Lambda, rol IAM, políticas, URL de función)
- ℹ️ Recursos que se leerán (cola SQS existente)

### 4. Aplicar la configuración

Para crear los recursos en AWS:

```bash
terraform apply
```

Terraform te pedirá confirmación. Escribe `yes` para continuar.

El proceso tardará aproximadamente 1-2 minutos y creará:
- 🔐 Rol IAM para la Lambda (`workshop-demo-joromero-lambda-role`)
- 📋 Políticas IAM (CloudWatch Logs y SQS)
- ⚡ Función Lambda (`workshop-demo-joromero-sqs-sender`)
- 🌐 URL pública de la función Lambda
- 📊 Log Group en CloudWatch

## 🎯 Acceder a la Lambda

Una vez completado el despliegue, Terraform mostrará la URL de la función:

```
Outputs:

lambda_function_url = "https://abcd1234xyz.lambda-url.us-east-1.on.aws/"
```

### Usar la interfaz web

1. **Copia la URL** mostrada en los outputs de Terraform
2. **Abre la URL en tu navegador**
3. **Completa el formulario:**
   - Email (obligatorio)
   - Nombre del Cliente (opcional)
   - Productos (obligatorio)
   - Monto en USD (opcional)
4. **Haz clic en "Enviar Mensaje a SQS"**

El mensaje será enviado a la cola SQS y procesado por el microservicio, que:
- Guardará el pedido en la base de datos
- Generará una factura en S3
- Enviará una notificación por SNS

### Ver la URL después del despliegue

Si cerraste la terminal, puedes obtener la URL nuevamente con:

```bash
terraform output lambda_function_url
```

## 📊 Monitoreo

### Logs de CloudWatch

Para ver los logs de la Lambda:

1. Ve a AWS Console → CloudWatch → Log Groups
2. Busca: `/aws/lambda/workshop-demo-joromero-sqs-sender`
3. Selecciona el Log Stream más reciente

### Cola SQS

Para verificar que los mensajes llegan a SQS:

1. Ve a AWS Console → SQS → Queues
2. Selecciona `workshop-demo-queue`
3. Haz clic en "Send and receive messages" para monitorear

## 🛠️ Comandos Útiles

### Ver todos los outputs
```bash
terraform output
```

### Mostrar el estado actual
```bash
terraform show
```

### Validar la configuración
```bash
terraform validate
```

### Formatear archivos .tf
```bash
terraform fmt
```

## 🗑️ Destruir la infraestructura

Para eliminar todos los recursos creados por Terraform:

```bash
terraform destroy
```

**⚠️ ADVERTENCIA:** Esto eliminará permanentemente:
- La función Lambda
- El rol y políticas IAM
- Los logs de CloudWatch

La cola SQS **NO** será eliminada ya que es un recurso preexistente.

## 📁 Estructura de Archivos

```
infra/terraform/
├── provider.tf          # Configuración del proveedor AWS
├── variables.tf         # Definición de variables
├── main.tf             # Recursos principales (Lambda, IAM, URL)
├── outputs.tf          # Outputs (URL de la Lambda)
├── terraform.tfvars    # Valores de variables (NO SUBIR A GIT)
├── lambda/
│   └── index.py        # Código Python de la función Lambda
└── README.md           # Este archivo
```

## 🔒 Seguridad

- ✅ Las credenciales AWS se pasan como variables sensibles
- ✅ El archivo `terraform.tfvars` está excluido del control de versiones
- ⚠️ La URL de la Lambda es pública (sin autenticación)
- ✅ Las políticas IAM siguen el principio de menor privilegio

## 🐛 Troubleshooting

### Error: "No valid credential sources found"
- Verifica que `terraform.tfvars` existe y contiene las credenciales correctas
- Las credenciales deben tener comillas: `aws_access_key = "AKIA..."`

### Error: "Queue does not exist"
- Confirma que la cola SQS existe en tu cuenta de AWS
- Verifica el nombre en `terraform.tfvars` (`sqs_queue_name`)
- Confirma que la región es correcta (`aws_region`)

### La Lambda no envía mensajes a SQS
- Revisa los logs en CloudWatch Logs
- Verifica que la cola SQS tiene la URL correcta en las variables de entorno de la Lambda
- Confirma que el rol IAM tiene permisos para `sqs:SendMessage`

### No puedo acceder a la URL de la Lambda
- Espera 30-60 segundos después del despliegue para que la URL esté disponible
- Verifica que la URL comienza con `https://` y termina con `.on.aws/`
- Confirma que tienes conexión a Internet

## 📚 Recursos Adicionales

- [Documentación de Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Lambda Documentation](https://docs.aws.amazon.com/lambda/)
- [AWS SQS Documentation](https://docs.aws.amazon.com/sqs/)

## 🤝 Soporte

Si encuentras problemas:

1. Revisa los logs de CloudWatch
2. Ejecuta `terraform plan` para ver el estado esperado
3. Verifica las credenciales y permisos de AWS
4. Consulta la documentación oficial de Terraform y AWS

---

**Autor:** Workshop Demo Team  
**Última actualización:** Febrero 2026
