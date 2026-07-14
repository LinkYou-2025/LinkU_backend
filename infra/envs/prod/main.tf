# 기본 VPC와 그 서브넷을 사용. 별도 VPC를 쓰게 되면 변수로 교체할 것.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

module "app" {
  source = "../../modules/ec2"

  name          = "linku-prod"
  instance_type = var.instance_type
  ami_id        = var.ami_id
  key_name      = var.key_name

  create_security_group = true
  vpc_id                = data.aws_vpc.default.id
  # nginx가 80/443으로 받고 내부에서 8080으로 프록시 (8080은 외부 미노출)
  ingress_ports = [22, 80, 443]

  # 고정 IP (Elastic IP)
  create_eip = true
}

module "db" {
  source = "../../modules/rds"

  name       = "linku-prod"
  vpc_id     = data.aws_vpc.default.id
  subnet_ids = data.aws_subnets.default.ids

  # 앱 EC2 보안그룹에서만 5432 접근 허용
  allowed_security_group_id = module.app.security_group_id

  db_name     = "linkUDB"
  db_username = "linkU"
  db_password = var.db_password

  skip_final_snapshot = false
  deletion_protection = true
}
