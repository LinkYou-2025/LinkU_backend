output "endpoint" {
  description = "호스트:포트 형식의 접속 엔드포인트"
  value       = aws_db_instance.this.endpoint
}

output "address" {
  description = "호스트 주소만"
  value       = aws_db_instance.this.address
}

output "jdbc_url" {
  description = "앱 DB_URL에 넣을 JDBC URL"
  value       = "jdbc:postgresql://${aws_db_instance.this.endpoint}/${aws_db_instance.this.db_name}"
}
