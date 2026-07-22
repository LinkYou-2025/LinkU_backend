terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # 상태 파일을 S3로 옮기고 싶을 때 주석 해제
  # backend "s3" {
  #   bucket = "linku-terraform-state"
  #   key    = "dev/terraform.tfstate"
  #   region = "ap-northeast-2"
  # }
}

provider "aws" {
  region  = var.aws_region
  profile = "linku"

  # LinkU 계정이 아니면 plan/apply 자체가 거부됨 (다른 계정 오염 방지)
  allowed_account_ids = ["236058984744"]
}
