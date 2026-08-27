#!/usr/bin/env bash
#
# Exports the running API's OpenAPI document as a Postman collection.
#
# Requirements:
#   - the backend running locally (default http://localhost:8080)
#   - Node.js 18+ (npx downloads openapi-to-postmanv2 on first use)
#
# Usage:
#   scripts/export-postman.sh                 # uses http://localhost:8080
#   API_BASE_URL=https://api.example.com scripts/export-postman.sh
#
# Output: docs/postman/slotify.postman_collection.json (import it into Postman).
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
OPENAPI_URL="${API_BASE_URL}/v3/api-docs"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT="${REPO_ROOT}/docs/postman/slotify.postman_collection.json"

command -v npx >/dev/null 2>&1 || {
  echo "Node.js (npx) is required: https://nodejs.org" >&2
  exit 1
}

mkdir -p "$(dirname "${OUTPUT}")"
echo "Fetching ${OPENAPI_URL} ..."
# -p pretty-prints the JSON so diffs stay readable in git.
curl -fsS "${OPENAPI_URL}" | npx -y openapi-to-postmanv2 -s /dev/stdin -o "${OUTPUT}" -p
echo "Collection written to ${OUTPUT}"
