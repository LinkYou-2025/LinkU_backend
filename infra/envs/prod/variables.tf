variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

variable "ami_id" {
  description = "prod EC2에 사용할 AMI ID (예: Ubuntu 24.04 LTS)"
  type        = string
}

variable "key_name" {
  description = "SSH 키페어 이름"
  type        = string
}

variable "db_password" {
  description = "RDS 마스터 비밀번호 (terraform.tfvars 또는 TF_VAR_db_password 환경변수로 주입)"
  type        = string
  sensitive   = true
}
