variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

# 아래 값들은 기존 인스턴스를 import하기 위해 콘솔에서 확인한 실제 값을 넣어야 함
variable "ami_id" {
  description = "기존 인스턴스의 AMI ID"
  type        = string
}

variable "key_name" {
  description = "기존 인스턴스의 키페어 이름"
  type        = string
}

variable "subnet_id" {
  description = "기존 인스턴스의 서브넷 ID"
  type        = string
}

variable "security_group_ids" {
  description = "기존 인스턴스에 붙어 있는 보안그룹 ID 목록"
  type        = list(string)
}

variable "root_volume_size" {
  description = "기존 인스턴스의 루트 볼륨 크기 (GB)"
  type        = number
  default     = 20
}
