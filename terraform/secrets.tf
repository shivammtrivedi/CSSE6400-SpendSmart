resource "aws_secretsmanager_secret" "db" {
  name                    = "spendsmart/db"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    host     = aws_db_instance.spendsmart.address
    port     = 5432
    dbname   = "spendsmart"
    username = var.db_username
    password = var.db_password
  })
}

# BankApiNodeJs secrets: MongoDB Atlas URI and JWT signing key
resource "aws_secretsmanager_secret" "bank_api" {
  name                    = "spendsmart/bank-api"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "bank_api" {
  secret_id = aws_secretsmanager_secret.bank_api.id
  secret_string = jsonencode({
    token_key = var.bank_api_token_key
  })
}
