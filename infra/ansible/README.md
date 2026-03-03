# Ansible (SaaS / IaC)

This folder contains Ansible playbooks and roles to provision and configure Govaryn SaaS infrastructure.
Terraform is intentionally not used here.

Typical responsibilities:
- baseline OS hardening
- docker/container runtime setup
- k8s node prep / addons
- secrets distribution (use Vault/KMS in real life)
- monitoring/agents
