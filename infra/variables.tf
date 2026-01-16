variable "region" {
  type    = string
  default = "ap-northeast-2"
}

variable "prefix" {
  type    = string
  default = "uppick"
}

variable "key_name" {
  type = string
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

variable "allowed_ssh_cidr" {
  type        = string
  description = "Your public IP CIDR for SSH (e.g. 1.2.3.4/32)"
}

variable "ssm_env_path" {
  type        = string
  description = "SSM Parameter path for env vars (SecureString). Example: /uppick/prod/env"
  default     = "/uppick/prod/env"
}

# ECR Repository name
variable "ecr_repo_name" {
  type    = string
  default = "uppick-api"
}

# NPM 초기 관리자 (초기 1회)
variable "npm_admin_email" {
  type    = string
  default = "admin@example.com"
}