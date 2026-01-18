# --------------------------------
# GitHub Actions OIDC Provider
# --------------------------------
data "tls_certificate" "github_actions" {
  url = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = [
    "sts.amazonaws.com"
  ]

  thumbprint_list = [
    data.tls_certificate.github_actions.certificates[0].sha1_fingerprint
  ]
}

# --------------------------------
# GitHub Actions Role
#  - repo: Yoepee/pt-ko-251229
#  - branch: main
# --------------------------------
resource "aws_iam_role" "gha_deploy" {
  name = "${var.prefix}-gha-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github_actions.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        StringLike = {
          # main 브랜치만 허용
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_owner}/${var.github_repo}:ref:refs/heads/${var.github_branch}"
        }
      }
    }]
  })
}

# --------------------------------
# Policy: ECR push + SSM put-parameter + SSM send-command
# --------------------------------
resource "aws_iam_role_policy" "gha_deploy" {
  name = "${var.prefix}-gha-deploy-policy"
  role = aws_iam_role.gha_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      # ECR 로그인 토큰
      {
        Effect   = "Allow",
        Action   = ["ecr:GetAuthorizationToken"],
        Resource = "*"
      },

      # ECR 푸시(해당 repo만)
      {
        Effect = "Allow",
        Action = [
          "ecr:BatchCheckLayerAvailability",
          "ecr:CompleteLayerUpload",
          "ecr:InitiateLayerUpload",
          "ecr:PutImage",
          "ecr:UploadLayerPart",
          "ecr:BatchGetImage"
        ],
        Resource = aws_ecr_repository.app.arn
      },

      # SSM Parameter Store: APP_IMAGE 업데이트 (네 path 기준)
      {
        Effect = "Allow",
        Action = [
          "ssm:PutParameter"
        ],
        Resource = "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter${var.ssm_env_path}/APP_IMAGE"
      },

      # SSM RunCommand 보내기
      # - document + instance 둘 다 Resource에 포함하는게 안전
      {
        Effect = "Allow",
        Action = [
          "ssm:SendCommand"
        ],
        Resource = [
          "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:document/AWS-RunShellScript",
          "arn:aws:ec2:${var.region}:${data.aws_caller_identity.current.account_id}:instance/${var.deploy_instance_id}"
        ]
      },

      # (옵션) 배포 결과 확인용 - workflow에서 status 조회할 때 필요
      {
        Effect = "Allow",
        Action = [
          "ssm:GetCommandInvocation",
          "ssm:ListCommandInvocations"
        ],
        Resource = "*"
      }
    ]
  })
}

output "aws_gha_role_arn" {
  value = aws_iam_role.gha_deploy.arn
}