# Ansible: Minimal Kubernetes + Istio + PostgreSQL

This playbook provisions:

- a minimal single-node Kubernetes cluster using `k3s`
- `kubectl`
- Kubernetes Gateway API CRDs
- Istio control plane in ambient mode (via `istioctl`)
- PostgreSQL in Kubernetes with:
  - ambient dataplane label on the PostgreSQL namespace
  - host directory backed storage
  - a 1Gi PV/PVC
  - host-only exposure via NodePort restricted to loopback

## Files

- `playbooks/k3s-istio-postgres.yml`
- `templates/k3s-config.yaml.j2`
- `templates/postgres-k8s.yaml.j2`

## Example inventory

```ini
[kube_hosts]
localhost ansible_connection=local
```

## Run

```bash
ansible-playbook -i inventory.ini ops/ansible/playbooks/k3s-istio-postgres.yml
```

## Common overrides

```bash
ansible-playbook \
  -i inventory.ini \
  ops/ansible/playbooks/k3s-istio-postgres.yml \
  -e istio_version=1.29.1 \
  -e gateway_api_crds_version=v1.3.0 \
  -e postgres_db_name=govaryn \
  -e postgres_db_user=govaryn \
  -e postgres_db_password=change-me \
  -e postgres_host_data_dir=/srv/postgres-dev \
  -e postgres_node_port=30432
```

## Connection details

- PostgreSQL service is exposed on `127.0.0.1:<postgres_node_port>` on cluster hosts.
- Default port is `127.0.0.1:30432`.

## Istio command reference

Manual equivalent used by the playbook:

```bash
curl -L https://istio.io/downloadIstio | sh -
cd istio-1.29.1
export PATH=$PWD/bin:$PATH
istioctl install --set profile=ambient --skip-confirmation
```
