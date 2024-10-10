#
#  Copyright (c) 2024 Metaform Systems, Inc.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Metaform Systems, Inc. - initial API and implementation
#


# This file deploys all the components needed for the provider side of the scenario,
# i.e. a catalog server ("bob"), two connectors ("ted" and "carol") as well as one identityhub and one vault

# first provider connector "provider-qna"
module "provider-qna-connector" {
  source            = "./modules/connector"
  humanReadableName = "provider-qna"
  participantId     = var.provider-did
  database = {
    user     = "qna"
    password = "provider-qna"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/provider_qna"
  }
  namespace     = kubernetes_namespace.ns.metadata.0.name
  vault-url     = "http://provider-vault:8200"
  sts-token-url = "${module.provider-sts.sts-token-url}/token"
}

# Second provider connector "provider-manufacturing"
module "provider-manufacturing-connector" {
  source            = "./modules/connector"
  humanReadableName = "provider-manufacturing"
  participantId     = var.provider-did
  database = {
    user     = "manufacturing"
    password = "provider-manufacturing"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/provider_manufacturing"
  }
  namespace     = kubernetes_namespace.ns.metadata.0.name
  vault-url     = "http://provider-vault:8200"
  sts-token-url = "${module.provider-sts.sts-token-url}/token"
}

module "provider-identityhub" {
  depends_on        = [module.provider-vault]
  source            = "./modules/identity-hub"
  credentials-dir   = dirname("./assets/credentials/k8s/provider/")
  humanReadableName = "provider-identityhub" # must be named "provider-identityhub" until we regenerate DIDs and credentials
  participantId     = var.provider-did
  vault-url         = "http://provider-vault:8200"
  service-name      = "provider"
  namespace         = kubernetes_namespace.ns.metadata.0.name

  database = {
    user     = "identityhub"
    password = "identityhub"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/identityhub"
  }
  sts-accounts-api-url = module.provider-sts.sts-accounts-url
}

# provider standalone STS
module "provider-sts" {
  depends_on        = [module.provider-vault]
  source            = "./modules/sts"
  humanReadableName = "provider-sts"
  namespace         = kubernetes_namespace.ns.metadata.0.name
  database = {
    user     = "sts"
    password = "sts"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/sts"
  }
  vault-url = "http://provider-vault:8200"
}

# Catalog server runtime
module "provider-catalog-server" {
  source            = "./modules/catalog-server"
  humanReadableName = "provider-catalog-server"
  participantId     = var.provider-did
  namespace         = kubernetes_namespace.ns.metadata.0.name
  vault-url         = "http://provider-vault:8200"
  sts-token-url     = "${module.provider-sts.sts-token-url}/token"

  database = {
    user     = "catalog_server"
    password = "catalog_server"
    url      = "jdbc:postgresql://${module.provider-postgres.database-url}/catalog_server"
  }
}

module "provider-vault" {
  source            = "./modules/vault"
  humanReadableName = "provider-vault"
  namespace         = kubernetes_namespace.ns.metadata.0.name
}

# Postgres database for the consumer
module "provider-postgres" {
  depends_on    = [kubernetes_config_map.postgres-initdb-config-cs]
  source        = "./modules/postgres"
  instance-name = "provider"
  init-sql-configs = [
    kubernetes_config_map.postgres-initdb-config-cs.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-pqna.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-pm.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-ih.metadata[0].name,
    kubernetes_config_map.postgres-initdb-config-sts.metadata[0].name
  ]
  namespace = kubernetes_namespace.ns.metadata.0.name
}

resource "kubernetes_config_map" "postgres-initdb-config-cs" {
  metadata {
    name      = "cs-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "cs-initdb-config.sql" = <<-EOT
        CREATE USER catalog_server WITH ENCRYPTED PASSWORD 'catalog_server' SUPERUSER;
        CREATE DATABASE catalog_server;
        \c catalog_server

      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-pqna" {
  metadata {
    name      = "provider-qna-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "provider-qna-initdb-config.sql" = <<-EOT
        CREATE USER qna WITH ENCRYPTED PASSWORD 'provider-qna' SUPERUSER;
        CREATE DATABASE provider_qna;
        \c provider_qna

      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-pm" {
  metadata {
    name      = "provider-manufacturing-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "provider-manufacturing-initdb-config.sql" = <<-EOT
        CREATE USER manufacturing WITH ENCRYPTED PASSWORD 'provider-manufacturing' SUPERUSER;
        CREATE DATABASE provider_manufacturing;
        \c provider_manufacturing

      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-ih" {
  metadata {
    name      = "ih-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "ih-initdb-config.sql" = <<-EOT
        CREATE USER identityhub WITH ENCRYPTED PASSWORD 'identityhub' SUPERUSER;
        CREATE DATABASE identityhub;
        \c identityhub
      EOT
  }
}

resource "kubernetes_config_map" "postgres-initdb-config-sts" {
  metadata {
    name      = "sts-initdb-config"
    namespace = kubernetes_namespace.ns.metadata.0.name
  }
  data = {
    "sts-initdb-config.sql" = <<-EOT
        CREATE USER sts WITH ENCRYPTED PASSWORD 'sts' SUPERUSER;
        CREATE DATABASE sts;
        \c sts
      EOT
  }
}