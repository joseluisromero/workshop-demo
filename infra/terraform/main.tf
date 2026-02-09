locals {
  lambda_name = "${var.project_prefix}-sqs-sender"
}

# Obtener la cola SQS existente
data "aws_sqs_queue" "workshop_queue" {
  name = var.sqs_queue_name
}

# Rol IAM para la función Lambda
resource "aws_iam_role" "lambda_execution_role" {
  name = "${var.project_prefix}-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Política para escribir logs en CloudWatch
resource "aws_iam_role_policy" "lambda_cloudwatch_policy" {
  name = "${var.project_prefix}-lambda-cloudwatch-policy"
  role = aws_iam_role.lambda_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# Política para enviar mensajes a SQS
resource "aws_iam_role_policy" "lambda_sqs_policy" {
  name = "${var.project_prefix}-lambda-sqs-policy"
  role = aws_iam_role.lambda_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:GetQueueUrl",
          "sqs:GetQueueAttributes"
        ]
        Resource = data.aws_sqs_queue.workshop_queue.arn
      }
    ]
  })
}

# Crear un archivo ZIP con el código de la Lambda
data "archive_file" "lambda_zip" {
  type        = "zip"
  source_dir  = "${path.module}/lambda"
  output_path = "${path.module}/lambda_function.zip"
}

# Función Lambda
resource "aws_lambda_function" "sqs_sender" {
  filename         = data.archive_file.lambda_zip.output_path
  function_name    = local.lambda_name
  role            = aws_iam_role.lambda_execution_role.arn
  handler         = "index.handler"
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  runtime         = "python3.11"
  timeout         = 30

  environment {
    variables = {
      SQS_QUEUE_URL = data.aws_sqs_queue.workshop_queue.url
    }
  }
}

# URL de función Lambda (acceso público)
resource "aws_lambda_function_url" "sqs_sender_url" {
  function_name      = aws_lambda_function.sqs_sender.function_name
  authorization_type = "NONE"

  cors {
    allow_credentials = true
    allow_origins     = ["*"]
    allow_methods     = ["GET", "POST"]
    allow_headers     = ["*"]
    expose_headers    = ["keep-alive", "date"]
    max_age           = 86400
  }
}

# CloudWatch Log Group para la Lambda
resource "aws_cloudwatch_log_group" "lambda_log_group" {
  name              = "/aws/lambda/${local.lambda_name}"
  retention_in_days = 7
}

resource "aws_lambda_permission" "allow_public_function_url" {
  statement_id           = "AllowPublicFunctionUrl"
  action                 = "lambda:InvokeFunctionUrl"
  function_name          = aws_lambda_function.sqs_sender.function_name
  principal              = "*"
  function_url_auth_type = "NONE"
  source_arn             = "arn:aws:lambda:us-east-1:959713283002:function:workshop-demo-joromero-sqs-sender"
}