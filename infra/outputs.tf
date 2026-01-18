output "instance_id" {
  value = aws_instance.this.id
}

output "public_ip" {
  value = aws_eip.this.public_ip
}

output "ecr_repo_url" {
  value = aws_ecr_repository.app.repository_url
}

output "gabia_dns_hint" {
  value = {
    api        = "A record: api.uppick.net  -> ${aws_eip.this.public_ip}"
    grafana    = "A record: grafana.uppick.net -> ${aws_eip.this.public_ip}"
    prometheus = "A record: prometheus.uppick.net -> ${aws_eip.this.public_ip}"
  }
}