locals {
  frontend_api_base_url = "/api/v1"
  frontend_bucket_name  = var.frontend_bucket_name != "" ? var.frontend_bucket_name : "spendsmart-frontend-${data.aws_caller_identity.current.account_id}-${var.aws_region}-${var.environment}"
  frontend_base_url     = "https://${aws_cloudfront_distribution.frontend.domain_name}"
  frontend_callback_url = "${local.frontend_base_url}/auth-callback.html"
  frontend_logout_url   = "${local.frontend_base_url}/index.html"

  frontend_content_types = {
    ".html" = "text/html; charset=utf-8"
    ".css"  = "text/css; charset=utf-8"
    ".js"   = "application/javascript; charset=utf-8"
    ".json" = "application/json; charset=utf-8"
    ".txt"  = "text/plain; charset=utf-8"
    ".svg"  = "image/svg+xml"
    ".png"  = "image/png"
    ".jpg"  = "image/jpeg"
    ".jpeg" = "image/jpeg"
    ".ico"  = "image/x-icon"
  }

  frontend_files = [
    for file in fileset("${path.root}/../frontend", "**") : file
    if !endswith(file, "README.md") && file != "assets/js/deploy-config.js"
  ]
}

resource "aws_s3_bucket" "frontend" {
  bucket        = local.frontend_bucket_name
  force_destroy = var.frontend_bucket_force_destroy
}

resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "spendsmart-frontend-oac-${var.environment}"
  description                       = "OAC for SpendSmart frontend static assets"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "SpendSmart static frontend"
  default_root_object = "index.html"
  price_class         = "PriceClass_100"

  origin {
    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "frontend-s3-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  origin {
    domain_name = aws_lb.spendsmart.dns_name
    origin_id   = "api-alb-origin"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "frontend-s3-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
  }

  ordered_cache_behavior {
    path_pattern           = "/api/*"
    target_origin_id       = "api-alb-origin"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS", "PUT", "PATCH", "POST", "DELETE"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    min_ttl                = 0
    default_ttl            = 0
    max_ttl                = 0

    forwarded_values {
      query_string = true
      headers      = ["*"]
      cookies {
        forward = "all"
      }
    }
  }

  custom_error_response {
    error_code            = 403
    response_code         = 404
    response_page_path    = "/errors.html"
    error_caching_min_ttl = 0
  }

  custom_error_response {
    error_code            = 404
    response_code         = 404
    response_page_path    = "/errors.html"
    error_caching_min_ttl = 0
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

data "aws_iam_policy_document" "frontend_cloudfront_read" {
  statement {
    sid     = "AllowCloudFrontReadFrontend"
    effect  = "Allow"
    actions = ["s3:GetObject"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    resources = ["${aws_s3_bucket.frontend.arn}/*"]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.frontend.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend_cloudfront_read" {
  bucket = aws_s3_bucket.frontend.id
  policy = data.aws_iam_policy_document.frontend_cloudfront_read.json

  depends_on = [aws_s3_bucket_public_access_block.frontend, aws_cloudfront_distribution.frontend]
}

resource "aws_s3_object" "frontend_files" {
  for_each = toset(local.frontend_files)

  bucket = aws_s3_bucket.frontend.id
  key    = each.value
  source = "${path.root}/../frontend/${each.value}"
  etag   = filemd5("${path.root}/../frontend/${each.value}")

  content_type = lookup(
    local.frontend_content_types,
    try(regex("\\.[^.]+$", each.value), ""),
    "application/octet-stream"
  )
}

resource "aws_s3_object" "frontend_deploy_config" {
  bucket = aws_s3_bucket.frontend.id
  key    = "assets/js/deploy-config.js"

  content = templatefile("${path.root}/templates/deploy-config.js.tftpl", {
    api_base_url         = local.frontend_api_base_url
    cognito_domain       = "https://${aws_cognito_user_pool_domain.spendsmart.domain}.auth.${var.aws_region}.amazoncognito.com"
    cognito_client_id    = aws_cognito_user_pool_client.spendsmart.id
    cognito_redirect_uri = local.frontend_callback_url
    cognito_logout_uri   = local.frontend_logout_url
  })

  content_type = "application/javascript; charset=utf-8"

  depends_on = [aws_s3_object.frontend_files]
}
