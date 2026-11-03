# Build the Lambda package in a Linux Docker container so psycopg2-binary
# is compiled for the Lambda runtime (Amazon Linux), not the host OS.
resource "null_resource" "build_signup_lambda" {
  triggers = {
    handler_hash      = filemd5("${path.root}/../lambdas/signup-function/handler.py")
    requirements_hash = filemd5("${path.root}/../lambdas/signup-function/requirements.txt")
  }

  provisioner "local-exec" {
    command = <<-EOT
      docker run --rm \
        -v "${path.root}/../lambdas/signup-function:/var/task" \
        python:3.12-slim \
        bash -c "
          pip install -r /var/task/requirements.txt -t /var/task/package/ --quiet &&
          cp /var/task/handler.py /var/task/package/
        "
    EOT
  }
}

data "archive_file" "signup_lambda" {
  type        = "zip"
  source_dir  = "${path.root}/../lambdas/signup-function/package"
  output_path = "${path.root}/../lambdas/signup-function/signup-function.zip"
  depends_on  = [null_resource.build_signup_lambda]
}

resource "aws_lambda_function" "signup" {
  function_name    = "spendsmart-post-confirmation"
  filename         = data.archive_file.signup_lambda.output_path
  source_code_hash = data.archive_file.signup_lambda.output_base64sha256

  runtime = "python3.12"
  handler = "handler.handler"
  timeout = 30

  role = data.aws_iam_role.lab.arn

  environment {
    variables = {
      DB_SECRET_ARN = aws_secretsmanager_secret.db.arn
    }
  }

  tags = { Name = "spendsmart-post-confirmation" }
}

resource "aws_lambda_permission" "cognito_invoke" {
  statement_id  = "AllowCognitoInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.signup.function_name
  principal     = "cognito-idp.amazonaws.com"
  source_arn    = aws_cognito_user_pool.spendsmart.arn
}

resource "aws_cloudwatch_log_group" "signup_lambda" {
  name              = "/aws/lambda/spendsmart-post-confirmation"
  retention_in_days = 7
}
