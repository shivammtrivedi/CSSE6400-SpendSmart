# KMS Customer Managed Key — encrypts OBP tokens at rest in DynamoDB
resource "aws_kms_key" "bank_auth_tokens" {
  description             = "Encrypts OBP tokens stored in the bank-auth-tokens DynamoDB table"
  deletion_window_in_days = 7
  enable_key_rotation     = true
}

resource "aws_kms_alias" "bank_auth_tokens" {
  name          = "alias/spendsmart-bank-auth-tokens"
  target_key_id = aws_kms_key.bank_auth_tokens.key_id
}

# DynamoDB table: PK = userId (String), SK = provider (String — "obp" or "bankapi")
resource "aws_dynamodb_table" "bank_auth_tokens" {
  name         = "bank-auth-tokens"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"
  range_key    = "provider"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "provider"
    type = "S"
  }

  server_side_encryption {
    enabled     = true
    kms_key_arn = aws_kms_key.bank_auth_tokens.arn
  }
}
