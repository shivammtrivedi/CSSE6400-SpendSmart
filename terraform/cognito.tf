resource "aws_cognito_user_pool" "spendsmart" {
  name = "spendsmart"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  password_policy {
    minimum_length    = 8
    require_uppercase = true
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
  }

  schema {
    name                = "email"
    attribute_data_type = "String"
    required            = true
    mutable             = true
  }

  schema {
    name                = "name"
    attribute_data_type = "String"
    required            = false
    mutable             = true
  }

  lambda_config {
    post_confirmation = aws_lambda_function.signup.arn
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  lifecycle {
    # Existing Cognito pools do not allow removing/modifying previously created schema items.
    ignore_changes = [schema]
  }

  tags = { Name = "spendsmart" }
}

resource "aws_cognito_user_pool_client" "spendsmart" {
  name         = "spendsmart-client"
  user_pool_id = aws_cognito_user_pool.spendsmart.id

  generate_secret = false

  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "email", "profile"]

  supported_identity_providers = ["COGNITO"]

  callback_urls = [local.frontend_callback_url]
  logout_urls   = [local.frontend_logout_url]

  explicit_auth_flows = [
    "ALLOW_USER_SRP_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
  ]
}

resource "aws_cognito_user_pool_domain" "spendsmart" {
  domain       = var.cognito_domain_prefix
  user_pool_id = aws_cognito_user_pool.spendsmart.id
}
