#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

mkdir -p .git/hooks
chmod +x scripts/hooks/check-file-tokens.sh
ln -sf ../../scripts/hooks/check-file-tokens.sh .git/hooks/pre-commit

echo "Pre-commit hook installed to .git/hooks/pre-commit"
