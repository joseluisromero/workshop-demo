import json
import os
import boto3
from botocore.exceptions import ClientError

# Inicializar el cliente de SQS
sqs = boto3.client('sqs')
queue_url = os.environ.get('SQS_QUEUE_URL')

def handler(event, context):
    """
    Lambda handler que maneja solicitudes GET y POST
    GET: Muestra el formulario HTML
    POST: Envía mensaje a SQS
    """
    
    # Obtener el método HTTP
    http_method = event.get('requestContext', {}).get('http', {}).get('method', 'GET')
    
    if http_method == 'GET':
        # Devolver la interfaz HTML
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'text/html; charset=utf-8'
            },
            'body': get_html_form()
        }
    
    elif http_method == 'POST':
        # Procesar el envío del formulario
        try:
            # Obtener el body de la solicitud
            body = event.get('body', '')
            
            # Si el body está en base64, decodificarlo
            if event.get('isBase64Encoded', False):
                import base64
                body = base64.b64decode(body).decode('utf-8')
            
            # Parsear los datos del formulario
            params = parse_form_data(body)
            
            # Validar campos obligatorios: Email y products
            email = params.get('email', '').strip()
            products = params.get('products', '').strip()
            if not email or not products:
                return {
                    'statusCode': 400,
                    'headers': {'Content-Type': 'text/html; charset=utf-8'},
                    'body': get_html_form(error='Los campos Email y Productos son obligatorios')
                }

            # (Opcional) validación básica de formato de email
            if '@' not in email or email.startswith('@') or email.endswith('@'):
                return {
                    'statusCode': 400,
                    'headers': {'Content-Type': 'text/html; charset=utf-8'},
                    'body': get_html_form(error='Ingrese una dirección de Email válida')
                }

            # Crear el mensaje en formato JSON (solo email y products)
            message = {
                'email': email,
                'products': products
            }
            
            # Enviar mensaje a SQS
            response = sqs.send_message(
                QueueUrl=queue_url,
                MessageBody=json.dumps(message)
            )
            
            return {
                'statusCode': 200,
                'headers': {'Content-Type': 'text/html; charset=utf-8'},
                'body': get_html_form(success=True, message_id=response['MessageId'])
            }
            
        except ClientError as e:
            return {
                'statusCode': 500,
                'headers': {'Content-Type': 'text/html; charset=utf-8'},
                'body': get_html_form(error=f'Error al enviar mensaje a SQS: {str(e)}')
            }
        except Exception as e:
            return {
                'statusCode': 500,
                'headers': {'Content-Type': 'text/html; charset=utf-8'},
                'body': get_html_form(error=f'Error inesperado: {str(e)}')
            }
    
    else:
        return {
            'statusCode': 405,
            'headers': {'Content-Type': 'text/plain'},
            'body': 'Método no permitido'
        }

def parse_form_data(body):
    """
    Parsea los datos del formulario enviados como application/x-www-form-urlencoded
    """
    from urllib.parse import parse_qs
    params_dict = parse_qs(body)
    # Extraer el primer valor de cada parámetro
    return {key: values[0] if values else '' for key, values in params_dict.items()}

def get_html_form(error=None, success=False, message_id=None):
    """
    Genera el HTML del formulario
    """
    alert_html = ''
    
    if error:
        alert_html = f'''
        <div style="background-color: #f8d7da; color: #721c24; padding: 15px; border-radius: 5px; margin-bottom: 20px; border: 1px solid #f5c6cb;">
            <strong>Error:</strong> {error}
        </div>
        '''
    
    if success:
        alert_html = f'''
        <div style="background-color: #d4edda; color: #155724; padding: 15px; border-radius: 5px; margin-bottom: 20px; border: 1px solid #c3e6cb;">
            <strong>¡Éxito!</strong> Mensaje enviado correctamente a la cola SQS.<br>
            <small>Message ID: {message_id}</small>
        </div>
        '''
    
    html = f'''
    <!DOCTYPE html>
    <html lang="es">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Workshop Demo - Enviar Mensaje a SQS</title>
        <style>
            * {{
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }}
            
            body {{
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                display: flex;
                justify-content: center;
                align-items: center;
                padding: 20px;
            }}
            
            .container {{
                background: white;
                border-radius: 15px;
                box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                padding: 40px;
                max-width: 600px;
                width: 100%;
            }}
            
            h1 {{
                color: #333;
                margin-bottom: 10px;
                font-size: 28px;
                text-align: center;
            }}
            
            .subtitle {{
                color: #666;
                text-align: center;
                margin-bottom: 30px;
                font-size: 14px;
            }}
            
            .form-group {{
                margin-bottom: 20px;
            }}
            
            label {{
                display: block;
                margin-bottom: 8px;
                color: #333;
                font-weight: 600;
                font-size: 14px;
            }}
            
            .required {{
                color: #e74c3c;
            }}
            
            input[type="text"],
            input[type="email"],
            input[type="number"],
            textarea {{
                width: 100%;
                padding: 12px;
                border: 2px solid #e0e0e0;
                border-radius: 8px;
                font-size: 14px;
                transition: border-color 0.3s;
                font-family: inherit;
            }}
            
            input[type="text"]:focus,
            input[type="email"]:focus,
            input[type="number"]:focus,
            textarea:focus {{
                outline: none;
                border-color: #667eea;
            }}
            
            textarea {{
                resize: vertical;
                min-height: 100px;
            }}
            
            button {{
                width: 100%;
                padding: 15px;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                border: none;
                border-radius: 8px;
                font-size: 16px;
                font-weight: 600;
                cursor: pointer;
                transition: transform 0.2s, box-shadow 0.2s;
            }}
            
            button:hover {{
                transform: translateY(-2px);
                box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
            }}
            
            button:active {{
                transform: translateY(0);
            }}
            
            .info-box {{
                background-color: #e3f2fd;
                border-left: 4px solid #2196F3;
                padding: 15px;
                margin-bottom: 25px;
                border-radius: 4px;
                font-size: 13px;
                color: #0d47a1;
            }}
            
            .queue-info {{
                text-align: center;
                margin-top: 20px;
                padding-top: 20px;
                border-top: 1px solid #e0e0e0;
                font-size: 12px;
                color: #666;
            }}
            
            .queue-name {{
                font-weight: 600;
                color: #667eea;
            }}
        </style>
    </head>
    <body>
        <div class="container">
            <h1>📦 Workshop Demo</h1>
            <p class="subtitle">Enviar mensajes a la cola SQS</p>
            
            {alert_html}
            
            <div class="info-box">
                <strong>ℹ️ Información:</strong> Complete el formulario para enviar un mensaje a la cola SQS. 
                El microservicio procesará el pedido, generará una factura en S3 y enviará una notificación por SNS.
            </div>
            
            <form method="POST">
                <div class="form-group">
                    <label for="email">
                        Email <span class="required">*</span>
                    </label>
                    <input 
                        type="email" 
                        id="email" 
                        name="email" 
                        placeholder="ejemplo@correo.com"
                        required
                    >
                </div>

                <div class="form-group">
                    <label for="products">
                        Productos <span class="required">*</span>
                    </label>
                    <textarea 
                        id="products" 
                        name="products" 
                        placeholder="Laptop Dell XPS 15, Mouse Logitech MX Master 3"
                        required
                    ></textarea>
                </div>

                <button type="submit">🚀 Enviar Mensaje a SQS</button>
            </form>
            
            <div class="queue-info">
                Cola SQS: <span class="queue-name">{os.environ.get('SQS_QUEUE_URL', 'N/A')}</span>
            </div>
        </div>
    </body>
    </html>
    '''
    
    return html
