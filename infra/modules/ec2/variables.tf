variable "name" {
  description = "리소스 이름 접두사 (예: linku-dev)"
  type        = string
}

variable "instance_type" {
  description = "EC2 인스턴스 타입"
  type        = string
  default     = "t3.small"
}

variable "ami_id" {
  description = "AMI ID (기존 인스턴스 import 시 콘솔에서 확인한 값)"
  type        = string
}

variable "key_name" {
  description = "SSH 키페어 이름"
  type        = string
  default     = null
}

variable "subnet_id" {
  description = "서브넷 ID (null이면 기본 서브넷)"
  type        = string
  default     = null
}

variable "vpc_id" {
  description = "보안그룹을 생성할 VPC ID (create_security_group = true일 때 필요)"
  type        = string
  default     = null
}

variable "create_security_group" {
  description = "true면 보안그룹을 새로 생성, false면 security_group_ids 사용"
  type        = bool
  default     = true
}

variable "security_group_ids" {
  description = "기존 보안그룹 ID 목록 (create_security_group = false일 때 사용)"
  type        = list(string)
  default     = []
}

variable "ingress_ports" {
  description = "보안그룹 생성 시 열어줄 포트 목록"
  type        = list(number)
  default     = [22, 8080]
}

variable "create_eip" {
  description = "true면 Elastic IP를 생성해서 인스턴스에 연결 (고정 IP)"
  type        = bool
  default     = false
}

variable "root_volume_size" {
  description = "루트 볼륨 크기 (GB)"
  type        = number
  default     = 20
}

variable "root_volume_encrypted" {
  description = "루트 볼륨 암호화 여부 (기존 비암호화 인스턴스는 true로 바꾸면 재생성됨)"
  type        = bool
  default     = true
}

variable "user_data" {
  description = "최초 부팅 시 실행할 user_data 스크립트 (null이면 미사용). 내용이 바뀌면 인스턴스가 재생성됨"
  type        = string
  default     = null
}
