locals {
  services = {
    "expense-service"   = { port = 8082, paths = ["/api/v1/expense*"], health_path = "/api/v1/expense/health", priority = 10 }
    "banking-service"   = { port = 8083, paths = ["/api/v1/banking*"], health_path = "/api/v1/banking/health", priority = 20 }
    "query-service"     = { port = 8084, paths = ["/api/v1/query*"], health_path = "/api/v1/query/health", priority = 30 }
    "alerts-service"    = { port = 8086, paths = ["/api/v1/alerts*"], health_path = "/api/v1/alerts/health", priority = 40 }
    "bank-auth-service" = { port = 8087, paths = ["/api/v1/login*", "/api/v1/token*"], health_path = "/api/v1/login/health", priority = 50 }
    "group-service"     = { port = 8088, paths = ["/api/v1/groups*"], health_path = "/api/v1/groups/health", priority = 60 }
    "budget-service"    = { port = 8089, paths = ["/api/v1/budgets*"], health_path = "/api/v1/budgets/health", priority = 70 }
    # Broad /api/* catch-all must be evaluated LAST so it can't shadow /api/v1/* rules (e.g. budgets).
    "bank-api-nodejs"   = { port = 3000, paths = ["/api/*"], health_path = "/health", priority = 100 }
  }
}

resource "aws_lb" "spendsmart" {
  name               = "spendsmart"
  internal           = false
  load_balancer_type = "application"
  subnets            = data.aws_subnets.default.ids
  security_groups    = [aws_security_group.alb.id]

  tags = { Name = "spendsmart" }
}

resource "aws_lb_target_group" "services" {
  for_each = local.services

  name        = "spendsmart-${each.key}"
  port        = each.value.port
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    path                = each.value.health_path
    port                = tostring(each.value.port)
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
  }

  tags = { Name = "spendsmart-${each.key}" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.spendsmart.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "application/json"
      message_body = "{\"code\":\"NOT_FOUND\",\"message\":\"No route matched\"}"
      status_code  = "404"
    }
  }
}

resource "aws_lb_listener_rule" "services" {
  for_each = local.services

  listener_arn = aws_lb_listener.http.arn
  priority     = each.value.priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.services[each.key].arn
  }

  condition {
    path_pattern {
      values = each.value.paths
    }
  }
}

output "alb_dns_name" {
  description = "Raw ALB DNS name"
  value       = aws_lb.spendsmart.dns_name
}
