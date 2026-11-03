# ALB — accepts HTTP traffic from the internet
resource "aws_security_group" "alb" {
  name        = "spendsmart-alb"
  description = "SpendSmart ALB: inbound HTTP from internet"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "spendsmart-alb" }
}

# ECS — all service containers share this group
# Allows: inbound from ALB (gateway port) + inbound from self (inter-service)
resource "aws_security_group" "ecs" {
  name        = "spendsmart-ecs"
  description = "SpendSmart ECS tasks: ALB ingress + inter-service"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "ALB traffic to all services"
    from_port       = 3000
    to_port         = 8089
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description = "Inter-service traffic within the ECS security group"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "spendsmart-ecs" }
}

# RDS — accepts PostgreSQL from ECS tasks and Lambda
resource "aws_security_group" "rds" {
  name        = "spendsmart-rds"
  description = "SpendSmart RDS: PostgreSQL inbound"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "PostgreSQL from ECS tasks"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  ingress {
    description = "PostgreSQL from Lambda (no VPC) and local dev"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "spendsmart-rds" }
}
