# dev EC2 인스턴스 상태 조회/시작/중지용 API Gateway + Lambda.

data "archive_file" "server_switch" {
  type        = "zip"
  source_file = "${path.module}/lambda/index.py"
  output_path = "${path.module}/lambda/server_switch.zip"
}

resource "aws_lambda_function" "server_switch" {
  function_name    = "linku-dev-server-switch"
  runtime          = "python3.12"
  handler          = "index.lambda_handler"
  filename         = data.archive_file.server_switch.output_path
  source_code_hash = data.archive_file.server_switch.output_base64sha256
  role             = aws_iam_role.server_switch.arn
  timeout          = 10

  environment {
    variables = {
      INSTANCE_ID   = module.app.instance_id
      SWITCH_SECRET = var.switch_secret
    }
  }
}

resource "aws_iam_role" "server_switch" {
  name = "linku-dev-server-switch-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "server_switch" {
  name = "linku-dev-server-switch-policy"
  role = aws_iam_role.server_switch.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ec2:StartInstances", "ec2:StopInstances"]
        Resource = "arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${module.app.instance_id}"
      },
      {
        # ec2:DescribeInstances는 리소스 수준 권한을 지원하지 않아 Resource를 "*"로 지정
        Effect   = "Allow"
        Action   = ["ec2:DescribeInstances"]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"
      }
    ]
  })
}

data "aws_caller_identity" "current" {}

resource "aws_apigatewayv2_api" "server_switch" {
  name          = "linku-dev-server-switch-api"
  protocol_type = "HTTP"
}

resource "aws_apigatewayv2_integration" "server_switch" {
  api_id                 = aws_apigatewayv2_api.server_switch.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.server_switch.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "server_switch_status" {
  api_id    = aws_apigatewayv2_api.server_switch.id
  route_key = "GET /status"
  target    = "integrations/${aws_apigatewayv2_integration.server_switch.id}"
}

resource "aws_apigatewayv2_route" "server_switch_start" {
  api_id    = aws_apigatewayv2_api.server_switch.id
  route_key = "POST /start"
  target    = "integrations/${aws_apigatewayv2_integration.server_switch.id}"
}

resource "aws_apigatewayv2_route" "server_switch_stop" {
  api_id    = aws_apigatewayv2_api.server_switch.id
  route_key = "POST /stop"
  target    = "integrations/${aws_apigatewayv2_integration.server_switch.id}"
}

resource "aws_apigatewayv2_stage" "server_switch" {
  api_id      = aws_apigatewayv2_api.server_switch.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "server_switch_apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.server_switch.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.server_switch.execution_arn}/*/*"
}

output "server_switch_url" {
  description = "base URL. 사용 시 /status(GET), /start(POST), /stop(POST)를 붙이고 쿼리파라미터로 ?secret=...를 추가"
  value       = aws_apigatewayv2_api.server_switch.api_endpoint
}
