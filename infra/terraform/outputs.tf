output "lambda_function_url" {
  description = "URL de la función Lambda para enviar mensajes a SQS"
  value       = aws_lambda_function_url.sqs_sender_url.function_url
}

output "lambda_function_name" {
  description = "Nombre de la función Lambda"
  value       = aws_lambda_function.sqs_sender.function_name
}

output "lambda_role_arn" {
  description = "ARN del rol IAM de la Lambda"
  value       = aws_iam_role.lambda_execution_role.arn
}
