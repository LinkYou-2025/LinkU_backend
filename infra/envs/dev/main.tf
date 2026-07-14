module "app" {
  source = "../../modules/ec2"

  name          = "linku-dev"
  instance_type = var.instance_type
  ami_id        = var.ami_id
  key_name      = var.key_name
  subnet_id     = var.subnet_id

  # 기존 인스턴스의 보안그룹을 그대로 사용 (새로 만들지 않음)
  create_security_group = false
  security_group_ids    = var.security_group_ids

  root_volume_size = var.root_volume_size
}
