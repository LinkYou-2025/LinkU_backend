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
      INSTANCE_ID          = module.app.instance_id
      GITHUB_ORG           = var.github_org
      GITHUB_CLIENT_ID     = var.github_client_id
      GITHUB_CLIENT_SECRET = var.github_client_secret
      JWT_SECRET           = var.jwt_secret
      APP_HEALTH_URL       = var.app_health_url
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

  cors_configuration {
    allow_origins = [var.backoffice_origin]
    allow_methods = ["GET", "POST"]
    allow_headers = ["authorization", "content-type"]
    max_age       = 3600
  }
}

resource "aws_apigatewayv2_integration" "server_switch" {
  api_id                 = aws_apigatewayv2_api.server_switch.id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.server_switch.invoke_arn
  payload_format_version = "2.0"
}

resource "aws_apigatewayv2_route" "server_switch_auth_github" {
  api_id    = aws_apigatewayv2_api.server_switch.id
  route_key = "POST /auth/github"
  target    = "integrations/${aws_apigatewayv2_integration.server_switch.id}"
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

  # BackOffice는 탭당 /status 4초, /history 15초 주기로 폴링
  default_route_settings {
    throttling_burst_limit = 10
    throttling_rate_limit  = 5
  }

  # 인증 없이 열려 있는 로그인 경로
  route_settings {
    route_key              = aws_apigatewayv2_route.server_switch_auth_github.route_key
    throttling_burst_limit = 3
    throttling_rate_limit  = 1
  }
}

resource "aws_lambda_permission" "server_switch_apigw" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.server_switch.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.server_switch.execution_arn}/*/*"
}

output "server_switch_url" {
  description = "base URL. 사용 시 /status(GET), /start(POST), /stop(POST)를 붙이고 헤더에 Authorization: Bearer <세션 JWT>를 추가. 세션 JWT는 POST /auth/github {code}로 발급"
  value       = aws_apigatewayv2_api.server_switch.api_endpoint
}
