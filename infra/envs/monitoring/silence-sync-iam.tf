# monitoring EC2가 SSM Run Command(EventBridge 트리거)를 받을 수 있도록 IAM 인스턴스 프로필 부여

data "aws_caller_identity" "current" {}

resource "aws_iam_role" "monitoring_instance" {
  name = "linku-monitoring-instance-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "monitoring_instance_ssm" {
  role       = aws_iam_role.monitoring_instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "monitoring_instance" {
  name = "linku-monitoring-instance-profile"
  role = aws_iam_role.monitoring_instance.name
}
