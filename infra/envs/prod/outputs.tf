output "instance_id" {
  value = module.app.instance_id
}

output "public_ip" {
  value = module.app.public_ip
}

output "rds_endpoint" {
  value = module.db.endpoint
}

output "app_db_url" {
  description = "prod docker-compose의 DB_URL 환경변수에 넣을 값"
  value       = module.db.jdbc_url
}
