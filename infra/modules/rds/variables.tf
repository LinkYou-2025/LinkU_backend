variable "name" {
  description = "리소스 이름 접두사 (예: linku-prod)"
  type        = string
}

variable "vpc_id" {
  description = "RDS가 위치할 VPC ID"
  type        = string
}

variable "subnet_ids" {
  description = "DB 서브넷 그룹에 넣을 서브넷 ID 목록 (서로 다른 AZ 2개 이상)"
  type        = list(string)
}

variable "allowed_security_group_id" {
  description = "DB 접속을 허용할 보안그룹 ID (앱 EC2의 보안그룹)"
  type        = string
}

variable "instance_class" {
  description = "RDS 인스턴스 클래스"
  type        = string
  default     = "db.t4g.micro"
}

variable "allocated_storage" {
  description = "스토리지 크기 (GB)"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "생성할 데이터베이스 이름"
  type        = string
  default     = "linkUDB"
}

variable "db_username" {
  description = "마스터 사용자 이름"
  type        = string
  default     = "linkU"
}

variable "db_password" {
  description = "마스터 비밀번호"
  type        = string
  sensitive   = true
}

variable "skip_final_snapshot" {
  description = "삭제 시 최종 스냅샷 생략 여부"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "삭제 보호 활성화 여부"
  type        = bool
  default     = false
}
