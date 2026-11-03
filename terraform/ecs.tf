locals {
  # Internal service DNS via Cloud Map: http://<name>.spendsmart.local:<port>
  internal_services = {
    "expense-service"   = 8082
    "banking-service"   = 8083
    "query-service"     = 8084
    "alerts-service"    = 8086
    "bank-auth-service" = 8087
    "group-service"     = 8088
    "bank-api-nodejs"   = 3000
    "budget-service"    = 8089
  }

  db_url = "jdbc:postgresql://${aws_db_instance.spendsmart.address}:5432/spendsmart"

  cognito_jwks_uri = "https://cognito-idp.${var.aws_region}.amazonaws.com/${aws_cognito_user_pool.spendsmart.id}/.well-known/jwks.json"
}

# ---------------------------------------------------------------------------
# ECS Cluster
# ---------------------------------------------------------------------------

resource "aws_ecs_cluster" "spendsmart" {
  name = "spendsmart"
}

# ---------------------------------------------------------------------------
# Cloud Map — private DNS for inter-service communication
# ---------------------------------------------------------------------------

resource "aws_service_discovery_private_dns_namespace" "spendsmart" {
  count = var.enable_service_discovery ? 1 : 0
  name  = "spendsmart.local"
  vpc   = data.aws_vpc.default.id
}

resource "aws_service_discovery_service" "internal" {
  for_each = var.enable_service_discovery ? local.internal_services : {}

  name = each.key

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.spendsmart[0].id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

# ---------------------------------------------------------------------------
# CloudWatch log groups
# ---------------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "services" {
  for_each          = toset(local.service_names)
  name              = "/spendsmart/${each.key}"
  retention_in_days = 7
}

# ---------------------------------------------------------------------------
# Helper: common log configuration block for each service
# ---------------------------------------------------------------------------

locals {
  log_config = {
    for svc in local.service_names : svc => {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/spendsmart/${svc}"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "ecs"
        "awslogs-create-group"  = "true"
      }
    }
  }

  # Secrets Manager key references for DB credentials (injected into ECS tasks)
  db_username_secret = "${aws_secretsmanager_secret.db.arn}:username::"
  db_password_secret = "${aws_secretsmanager_secret.db.arn}:password::"

}

# ---------------------------------------------------------------------------
# Task Definitions
# ---------------------------------------------------------------------------

resource "aws_ecs_task_definition" "expense_service" {
  family                   = "spendsmart-expense-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name      = "expense-service"
    image     = "${aws_ecr_repository.services["expense-service"].repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{ containerPort = 8082, hostPort = 8082, protocol = "tcp" }]

    environment = [
      { name = "SPRING_DATASOURCE_URL", value = local.db_url },
      { name = "COGNITO_JWKS_URI", value = local.cognito_jwks_uri },
    ]
    secrets = [
      { name = "SPRING_DATASOURCE_USERNAME", valueFrom = local.db_username_secret },
      { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = local.db_password_secret },
    ]

    logConfiguration = local.log_config["expense-service"]
  }])

  depends_on = [docker_registry_image.services]
}

resource "aws_ecs_task_definition" "group_service" {
  family                   = "spendsmart-group-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name      = "group-service"
    image     = "${aws_ecr_repository.services["group-service"].repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{ containerPort = 8088, hostPort = 8088, protocol = "tcp" }]

    environment = [
      { name = "SPRING_DATASOURCE_URL", value = local.db_url },
      { name = "COGNITO_JWKS_URI", value = local.cognito_jwks_uri },
    ]
    secrets = [
      { name = "SPRING_DATASOURCE_USERNAME", valueFrom = local.db_username_secret },
      { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = local.db_password_secret },
    ]

    logConfiguration = local.log_config["group-service"]
  }])

  depends_on = [docker_registry_image.services]
}

resource "aws_ecs_task_definition" "alerts_service" {
  family                   = "spendsmart-alerts-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name      = "alerts-service"
    image     = "${aws_ecr_repository.services["alerts-service"].repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{ containerPort = 8086, hostPort = 8086, protocol = "tcp" }]

    environment = [
      { name = "SPRING_DATASOURCE_URL", value = local.db_url },
      { name = "COGNITO_JWKS_URI", value = local.cognito_jwks_uri },
    ]
    secrets = [
      { name = "SPRING_DATASOURCE_USERNAME", valueFrom = local.db_username_secret },
      { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = local.db_password_secret },
    ]

    logConfiguration = local.log_config["alerts-service"]
  }])

  depends_on = [docker_registry_image.services]
}

resource "aws_ecs_task_definition" "banking_service" {
  family                   = "spendsmart-banking-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name      = "banking-service"
    image     = "${aws_ecr_repository.services["banking-service"].repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{ containerPort = 8083, hostPort = 8083, protocol = "tcp" }]

    environment = [
      { name = "COGNITO_JWKS_URI",      value = local.cognito_jwks_uri },
      { name = "BANK_AUTH_SERVICE_URL", value = var.enable_service_discovery ? "http://bank-auth-service.spendsmart.local:8087" : "http://${aws_lb.spendsmart.dns_name}" },
      { name = "BANK_API_BASE_URL",     value = var.enable_service_discovery ? "http://bank-api-nodejs.spendsmart.local:3000" : "http://${aws_lb.spendsmart.dns_name}" },
    ]

    logConfiguration = local.log_config["banking-service"]
  }])

  depends_on = [docker_registry_image.services]
}

resource "aws_ecs_task_definition" "query_service" {
  family                   = "spendsmart-query-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name      = "query-service"
    image     = "${aws_ecr_repository.services["query-service"].repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{ containerPort = 8084, hostPort = 8084, protocol = "tcp" }]

    environment = [
      { name = "COGNITO_JWKS_URI", value = local.cognito_jwks_uri },
      { name = "BANKING_SERVICE_URL", value = var.enable_service_discovery ? "http://banking-service.spendsmart.local:8083" : "http://${aws_lb.spendsmart.dns_name}" },
      { name = "SPRING_DATASOURCE_URL", value = local.db_url },
    ]
    secrets = [
      { name = "SPRING_DATASOURCE_USERNAME", valueFrom = local.db_username_secret },
      { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = local.db_password_secret },
    ]

    logConfiguration = local.log_config["query-service"]
  }])

  depends_on = [docker_registry_image.services]
}

resource "aws_ecs_task_definition" "bank_auth_service" {
  family                   = "spendsmart-bank-auth-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name      = "bank-auth-service"
    image     = "${aws_ecr_repository.services["bank-auth-service"].repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{ containerPort = 8087, hostPort = 8087, protocol = "tcp" }]

    environment = [
      { name = "COGNITO_JWKS_URI",        value = local.cognito_jwks_uri },
      { name = "AWS_REGION",              value = var.aws_region },
      { name = "AWS_DYNAMODB_TABLE_NAME", value = aws_dynamodb_table.bank_auth_tokens.name },
      { name = "BANK_API_BASE_URL",       value = var.enable_service_discovery ? "http://bank-api-nodejs.spendsmart.local:3000" : "http://${aws_lb.spendsmart.dns_name}" },
    ]

    logConfiguration = local.log_config["bank-auth-service"]
  }])

  depends_on = [docker_registry_image.services]
}

# ---------------------------------------------------------------------------
# ECS Services
# ---------------------------------------------------------------------------

resource "aws_ecs_service" "expense_service" {
  name                  = "expense-service"
  cluster               = aws_ecs_cluster.spendsmart.id
  task_definition       = aws_ecs_task_definition.expense_service.arn
  desired_count         = 1
  launch_type           = "FARGATE"
  wait_for_steady_state = true
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["expense-service"].arn
    container_name   = "expense-service"
    container_port   = 8082
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["expense-service"].arn
    }
  }

  depends_on = [aws_lb_listener.http, aws_db_instance.spendsmart]
}

resource "aws_ecs_service" "group_service" {
  name            = "group-service"
  cluster         = aws_ecs_cluster.spendsmart.id
  task_definition = aws_ecs_task_definition.group_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["group-service"].arn
    container_name   = "group-service"
    container_port   = 8088
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["group-service"].arn
    }
  }

  depends_on = [aws_lb_listener.http]
}

resource "aws_ecs_service" "alerts_service" {
  name            = "alerts-service"
  cluster         = aws_ecs_cluster.spendsmart.id
  task_definition = aws_ecs_task_definition.alerts_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["alerts-service"].arn
    container_name   = "alerts-service"
    container_port   = 8086
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["alerts-service"].arn
    }
  }

  depends_on = [aws_lb_listener.http]
}

resource "aws_ecs_service" "banking_service" {
  name            = "banking-service"
  cluster         = aws_ecs_cluster.spendsmart.id
  task_definition = aws_ecs_task_definition.banking_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["banking-service"].arn
    container_name   = "banking-service"
    container_port   = 8083
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["banking-service"].arn
    }
  }

  depends_on = [aws_lb_listener.http]
}

resource "aws_ecs_service" "query_service" {
  name            = "query-service"
  cluster         = aws_ecs_cluster.spendsmart.id
  task_definition = aws_ecs_task_definition.query_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["query-service"].arn
    container_name   = "query-service"
    container_port   = 8084
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["query-service"].arn
    }
  }

  depends_on = [aws_lb_listener.http, aws_db_instance.spendsmart]
}

resource "aws_ecs_service" "bank_auth_service" {
  name            = "bank-auth-service"
  cluster         = aws_ecs_cluster.spendsmart.id
  task_definition = aws_ecs_task_definition.bank_auth_service.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["bank-auth-service"].arn
    container_name   = "bank-auth-service"
    container_port   = 8087
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["bank-auth-service"].arn
    }
  }

  depends_on = [aws_lb_listener.http]
}

# ---------------------------------------------------------------------------
# Budget Service
# ---------------------------------------------------------------------------

resource "aws_ecs_task_definition" "budget_service" {
  family                   = "spendsmart-budget-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([{
    name      = "budget-service"
    image     = "${aws_ecr_repository.services["budget-service"].repository_url}:${var.image_tag}"
    essential = true

    portMappings = [{ containerPort = 8089, hostPort = 8089, protocol = "tcp" }]

    environment = [
      { name = "SPRING_DATASOURCE_URL", value = local.db_url },
      { name = "COGNITO_JWKS_URI",      value = local.cognito_jwks_uri },
    ]
    secrets = [
      { name = "SPRING_DATASOURCE_USERNAME", valueFrom = local.db_username_secret },
      { name = "SPRING_DATASOURCE_PASSWORD", valueFrom = local.db_password_secret },
    ]

    logConfiguration = local.log_config["budget-service"]
  }])

  depends_on = [docker_registry_image.services]
}

resource "aws_ecs_service" "budget_service" {
  name                  = "budget-service"
  cluster               = aws_ecs_cluster.spendsmart.id
  task_definition       = aws_ecs_task_definition.budget_service.arn
  desired_count         = 1
  launch_type           = "FARGATE"
  wait_for_steady_state = true

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["budget-service"].arn
    container_name   = "budget-service"
    container_port   = 8089
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["budget-service"].arn
    }
  }

  depends_on = [aws_lb_listener.http, aws_db_instance.spendsmart]
}

# ---------------------------------------------------------------------------
# BankApiNodeJs — internal-only, no ALB routing
# ---------------------------------------------------------------------------

resource "aws_ecs_task_definition" "bank_api_nodejs" {
  family                   = "spendsmart-bank-api-nodejs"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 256
  memory                   = 1024
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn

  container_definitions = jsonencode([
    {
      name      = "mongodb"
      image     = "mongo:6"
      essential = false

      portMappings = [{ containerPort = 27017, hostPort = 27017, protocol = "tcp" }]

      logConfiguration = local.log_config["bank-api-nodejs"]
    },
    {
      name      = "bank-api-nodejs"
      image     = "${aws_ecr_repository.services["bank-api-nodejs"].repository_url}:${var.image_tag}"
      essential = true

      portMappings = [{ containerPort = 3000, hostPort = 3000, protocol = "tcp" }]

      dependsOn = [{ containerName = "mongodb", condition = "START" }]

      environment = [
        { name = "PORT",      value = "3000" },
        { name = "MONGO_URI", value = "mongodb://localhost:27017/bankapi" },
        { name = "TOKEN_KEY", value = var.bank_api_token_key },
        { name = "BANK_API_SEED_DATA", value = "true" },
        { name = "BANK_API_SEED_EMAIL", value = "test@example.com" },
        { name = "BANK_API_SEED_PASSWORD", value = "password123" },
      ]

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:3000/health || exit 1"]
        interval    = 10
        timeout     = 5
        retries     = 2
        startPeriod = 30
      }

      logConfiguration = local.log_config["bank-api-nodejs"]
    }
  ])

  depends_on = [docker_registry_image.services]
}

resource "aws_ecs_service" "bank_api_nodejs" {
  name            = "bank-api-nodejs"
  cluster         = aws_ecs_cluster.spendsmart.id
  task_definition = aws_ecs_task_definition.bank_api_nodejs.arn
  desired_count   = 1
  launch_type     = "FARGATE"
  enable_execute_command = true
  health_check_grace_period_seconds = 120

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.services["bank-api-nodejs"].arn
    container_name   = "bank-api-nodejs"
    container_port   = 3000
  }

  dynamic "service_registries" {
    for_each = var.enable_service_discovery ? [1] : []
    content {
      registry_arn = aws_service_discovery_service.internal["bank-api-nodejs"].arn
    }
  }

  depends_on = [aws_lb_listener.http]
}
