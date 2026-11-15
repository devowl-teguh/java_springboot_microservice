#!/usr/bin/env bash
# Deploys the free-tier AWS mode (Lambda + SQS) using the Neon connection in .env.local.
# Usage: scripts/deploy-aws.sh you@example.com
# Secrets are read from .env.local (gitignored, written by `neon init`) and never printed.
set -euo pipefail
cd "$(dirname "$0")/.."

budget_email="${1:?Usage: scripts/deploy-aws.sh <email for the 1 USD budget alert>}"
[[ -f .env.local ]] || { echo "Missing .env.local. Run: neon init" >&2; exit 1; }

# Convert Neon's postgresql://user:pass@host/db?... into JDBC parts.
read -r db_url db_user db_pass < <(python3 - <<'EOF'
import urllib.parse as u
env = {}
for line in open(".env.local"):
    if "=" in line and not line.lstrip().startswith("#"):
        k, v = line.rstrip("\n").split("=", 1)
        env[k] = v.strip().strip('"').strip("'")
p = u.urlparse(env["DATABASE_URL"])  # pooled endpoint: right for many short-lived Lambda connections
jdbc = f"jdbc:postgresql://{p.hostname}{p.path}?sslmode=require"
print(jdbc, u.unquote(p.username), u.unquote(p.password))
EOF
)

region="$(aws configure get region)"
echo "Deploying stack transaction-api to ${region} (database host: ${db_url#jdbc:postgresql://})"
echo "${db_url}" | grep -q "${region}" || echo "Note: the database is in a different region than ${region}; requests will be slower."

mvn -B -q -Plambda package -DskipTests
sam deploy \
  --stack-name transaction-api \
  --region "${region}" \
  --resolve-s3 \
  --capabilities CAPABILITY_IAM \
  --no-confirm-changeset \
  --no-fail-on-empty-changeset \
  --parameter-overrides "DbUrl=${db_url}" "DbUsername=${db_user}" "DbPassword=${db_pass}" "BudgetEmail=${budget_email}"

aws cloudformation describe-stacks --stack-name transaction-api --region "${region}" \
  --query "Stacks[0].Outputs[?OutputKey=='ApiUrl'].OutputValue" --output text
