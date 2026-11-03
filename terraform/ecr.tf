locals {
  service_names = [
    "expense-service",
    "banking-service",
    "query-service",
    "alerts-service",
    "bank-auth-service",
    "group-service",
    "bank-api-nodejs",
    "budget-service",
  ]
}

resource "aws_ecr_repository" "services" {
  for_each = toset(local.service_names)
  name     = "spendsmart-${each.key}"

  force_delete = true

  tags = { Name = "spendsmart-${each.key}" }
}

# Build each service image locally and push to ECR.
# Requires Docker daemon running during terraform apply.
resource "docker_image" "services" {
  for_each = toset(local.service_names)
  name     = "${aws_ecr_repository.services[each.key].repository_url}:${var.image_tag}"

  build {
    context    = "${path.root}/.."
    dockerfile = "services/${each.key}/Dockerfile"
  }

  triggers = {
    dir_sha = sha1(join("", concat(
      [
        for f in sort(tolist(fileset("${path.root}/../services/${each.key}", "src/**"))) :
        filesha1("${path.root}/../services/${each.key}/${f}")
      ],
      [
        for f in sort(tolist(fileset("${path.root}/../services/${each.key}", "gradle/**"))) :
        filesha1("${path.root}/../services/${each.key}/${f}")
      ],
      [
        for f in [
          "Dockerfile",
          ".dockerignore",
          "build.gradle",
          "settings.gradle",
          "gradlew",
          "gradlew.bat",
        ] :
        filesha1("${path.root}/../services/${each.key}/${f}")
        if fileexists("${path.root}/../services/${each.key}/${f}")
      ],
      [
        for f in sort(tolist(fileset("${path.root}/../common/shared-schema", "src/**"))) :
        filesha1("${path.root}/../common/shared-schema/${f}")
      ],
      [
        for f in sort(tolist(fileset("${path.root}/../common/shared-schema", "gradle/**"))) :
        filesha1("${path.root}/../common/shared-schema/${f}")
      ],
      [
        for f in ["build.gradle", "settings.gradle", "gradlew", "gradlew.bat"] :
        filesha1("${path.root}/../common/shared-schema/${f}")
        if fileexists("${path.root}/../common/shared-schema/${f}")
      ]
    )))
  }
}

resource "docker_registry_image" "services" {
  for_each = toset(local.service_names)
  name     = docker_image.services[each.key].name
}
