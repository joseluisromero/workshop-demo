variable "aws_region" {
  description = "AWS region donde se desplegarán los recursos"
  type        = string
  default     = "us-east-1"
}

variable "aws_access_key" {
  description = "AWS Access Key ID del usuario de servicio"
  type        = string
  sensitive   = true
}

variable "aws_secret_key" {
  description = "AWS Secret Access Key del usuario de servicio"
  type        = string
  sensitive   = true
}

variable "sqs_queue_name" {
  description = "Nombre de la cola SQS existente"
  type        = string
  default     = "workshop-demo-queue"
}

variable "project_prefix" {
  description = "Prefijo para nombrar los recursos"
  type        = string
  default     = "workshop-demo-joromero"
}
