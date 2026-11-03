variable "aws_region" {
  description = "AWS region to deploy into"
  default     = "us-east-1"
}

variable "aws_profile" {
  description = "AWS shared credentials profile name to use for provider authentication"
  default     = "058264189730_CSSE6400-LabRole"
}

variable "environment" {
  description = "Deployment environment tag"
  default     = "prod"
}

variable "db_username" {
  description = "PostgreSQL master username"
  default     = "spendsmart"
}

variable "db_password" {
  description = "PostgreSQL master password"
  default     = "spendsmart123!"
  sensitive   = true
}

variable "cognito_domain_prefix" {
  description = "Globally unique prefix for the Cognito Hosted UI (e.g. spendsmart-yourteam)"
  default     = "spendsmart-team00-2026"
}

variable "image_tag" {
  description = "Docker image tag to deploy for all services"
  default     = "latest"
}

variable "frontend_bucket_name" {
  description = "Optional S3 bucket name for frontend hosting. Leave empty to auto-generate."
  type        = string
  default     = ""
}

variable "frontend_bucket_force_destroy" {
  description = "Whether Terraform can destroy the frontend bucket even if it contains objects."
  type        = bool
  default     = true
}

variable "enable_service_discovery" {
  description = "Enable AWS Cloud Map private DNS registration for ECS service-to-service discovery."
  type        = bool
  default     = false
}

variable "bank_api_token_key" {
  description = "JWT signing secret for BankApiNodeJs"
  sensitive   = true
  default     = "spendsmart-bankapi-secret-key"
}
