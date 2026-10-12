variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "instance_type" {
  type    = string
  default = "t3.micro"
}

variable "ami_id" {
  description = "monitoring EC2에 사용할 AMI ID (예: Ubuntu 24.04 LTS)"
  type        = string
}

variable "key_name" {
  description = "SSH 키페어 이름"
  type        = string
}

variable "subnet_id" {
  description = "dev/prod와 동일한 VPC의 퍼블릭 서브넷 ID"
  type        = string
}

variable "vpc_id" {
  description = "dev/prod와 동일한 VPC ID"
  type        = string
}

variable "root_volume_size" {
  description = "루트 볼륨 크기 (GB)"
  type        = number
  default     = 25
}

variable "dev_security_group_id" {
  description = "dev 앱 서버 보안그룹 ID"
  type        = string
}

variable "prod_security_group_id" {
  description = "prod 앱 서버 보안그룹 ID"
  type        = string
}
