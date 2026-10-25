# dev EC2 상태 변화를 감지해 monitoring EC2에서 silence-sync.sh를 실행

resource "aws_cloudwatch_event_rule" "dev_instance_state_change" {
  name = "linku-dev-instance-state-change"
  event_pattern = jsonencode({
    source        = ["aws.ec2"]
    "detail-type" = ["EC2 Instance State-change Notification"]
    detail = {
      "instance-id" = [var.dev_instance_id]
      state         = ["stopped", "running"]
    }
  })
}

resource "aws_iam_role" "eventbridge_ssm" {
  name = "linku-eventbridge-ssm-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "events.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "eventbridge_ssm" {
  name = "linku-eventbridge-ssm-policy"
  role = aws_iam_role.eventbridge_ssm.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["ssm:SendCommand"]
      Resource = [
        "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript",
        "arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${module.monitoring.instance_id}"
      ]
    }]
  })
}

resource "aws_cloudwatch_event_target" "silence_sync" {
  rule     = aws_cloudwatch_event_rule.dev_instance_state_change.name
  arn      = "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript"
  role_arn = aws_iam_role.eventbridge_ssm.arn

  run_command_targets {
    key    = "InstanceIds"
    values = [module.monitoring.instance_id]
  }

  input_transformer {
    input_paths = {
      state = "$.detail.state"
    }
    input_template = "{\"commands\":[\"/home/ubuntu/linku-monitoring/silence-sync.sh <state>\"]}"
  }
}
