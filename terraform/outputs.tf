output "gateway_url" {
  description = "Public URL for the SpendSmart API gateway"
  value       = "http://${aws_lb.spendsmart.dns_name}"
}

output "cognito_hosted_ui_url" {
  description = "Cognito Hosted UI login page"
  value       = "https://${aws_cognito_user_pool_domain.spendsmart.domain}.auth.${var.aws_region}.amazoncognito.com/login?client_id=${aws_cognito_user_pool_client.spendsmart.id}&response_type=code&scope=openid+email+profile&redirect_uri=${local.frontend_callback_url}"
}

output "cognito_user_pool_id" {
  description = "Cognito User Pool ID"
  value       = aws_cognito_user_pool.spendsmart.id
}

output "cognito_client_id" {
  description = "Cognito App Client ID"
  value       = aws_cognito_user_pool_client.spendsmart.id
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.spendsmart.address
}

output "frontend_bucket_name" {
  description = "S3 bucket hosting the static frontend"
  value       = aws_s3_bucket.frontend.bucket
}

output "frontend_website_url" {
  description = "Public HTTPS URL for the frontend (CloudFront)"
  value       = local.frontend_base_url
}

output "frontend_cloudfront_domain_name" {
  description = "CloudFront domain name serving the frontend"
  value       = aws_cloudfront_distribution.frontend.domain_name
}
