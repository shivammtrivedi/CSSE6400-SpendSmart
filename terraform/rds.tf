resource "aws_db_instance" "spendsmart" {
  identifier        = "spendsmart"
  engine            = "postgres"
  engine_version    = "17"
  instance_class    = "db.t3.micro"
  allocated_storage = 20

  db_name  = "spendsmart"
  username = var.db_username
  password = var.db_password

  parameter_group_name = "default.postgres17"
  multi_az             = true
  skip_final_snapshot  = true
  publicly_accessible  = true

  vpc_security_group_ids = [aws_security_group.rds.id]

  tags = { Name = "spendsmart" }
}
