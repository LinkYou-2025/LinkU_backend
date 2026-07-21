output "instance_id" {
  value = aws_instance.this.id
}

output "public_ip" {
  description = "EIP를 만들었으면 고정 IP, 아니면 인스턴스의 퍼블릭 IP"
  value       = var.create_eip ? aws_eip.this[0].public_ip : aws_instance.this.public_ip
}

output "security_group_id" {
  value = var.create_security_group ? aws_security_group.this[0].id : null
}
