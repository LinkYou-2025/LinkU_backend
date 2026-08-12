# 모니터링 서버가 dev/prod 앱 서버의 /actuator/prometheus, node-exporter를 크레이프할 수 있도록 인바운드 규칙 추가

resource "aws_vpc_security_group_ingress_rule" "dev_app_metrics" {
  security_group_id            = var.dev_security_group_id
  referenced_security_group_id = module.monitoring.security_group_id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
  description                  = "linku-monitoring: actuator/prometheus scrape"
}

resource "aws_vpc_security_group_ingress_rule" "dev_node_exporter" {
  security_group_id            = var.dev_security_group_id
  referenced_security_group_id = module.monitoring.security_group_id
  from_port                    = 9100
  to_port                      = 9100
  ip_protocol                  = "tcp"
  description                  = "linku-monitoring: node-exporter scrape"
}

resource "aws_vpc_security_group_ingress_rule" "prod_app_metrics" {
  security_group_id            = var.prod_security_group_id
  referenced_security_group_id = module.monitoring.security_group_id
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
  description                  = "linku-monitoring: actuator/prometheus scrape"
}

resource "aws_vpc_security_group_ingress_rule" "prod_node_exporter" {
  security_group_id            = var.prod_security_group_id
  referenced_security_group_id = module.monitoring.security_group_id
  from_port                    = 9100
  to_port                      = 9100
  ip_protocol                  = "tcp"
  description                  = "linku-monitoring: node-exporter scrape"
}
