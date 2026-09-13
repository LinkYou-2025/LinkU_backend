module "monitoring" {
  source = "../../modules/ec2"

  name          = "linku-monitoring"
  instance_type = var.instance_type
  ami_id        = var.ami_id
  key_name      = var.key_name
  subnet_id     = var.subnet_id
  vpc_id        = var.vpc_id

  create_security_group = true
  ingress_ports         = [22] # SSH만

  create_eip = true

  root_volume_size = var.root_volume_size

  user_data = file("${path.module}/user-data.sh")
}
