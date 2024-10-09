#!/bin/bash
kind delete cluster -n mvd
./gradlew build
./gradlew -Ppersistence=true dockerize
kind create cluster -n mvd --config deployment/kind.config.yaml
kind load docker-image controlplane:latest dataplane:latest identity-hub:latest catalog-server:latest sts:latest -n mvd
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
cd deployment
terraform init
terraform apply -auto-approve
cd ..
./seed-k8s.sh
