#!/bin/bash
set -euo pipefail

PLAYWRIGHT_VERSION=$(node -e "console.log(require('@playwright/test/package.json').version)")

echo "Running Playwright tests in Docker image mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}"

# Normalize args: strip a leading standalone "--" (pnpm run ... -- --project=foo)
ARGS=("$@")
if [[ "${ARGS[0]:-}" == "--" ]]; then
  ARGS=("${ARGS[@]:1}")
fi

docker run \
  -e CI \
  --mount type=bind,source="$PWD",target=/app-source,readonly \
  --ipc=host \
  --net=host \
  mcr.microsoft.com/playwright:v"$PLAYWRIGHT_VERSION" \
  sh -c "cp -r /app-source /app && cd /app && corepack pnpm install && corepack pnpm exec playwright test ${ARGS[@]}"