#!/bin/bash
set -euo pipefail

PLAYWRIGHT_VERSION=$(node -e "console.log(require('@playwright/test/package.json').version)")
# The corepack bundled with the Playwright image's Node is too old for pnpm >= 11, so install pnpm directly
PNPM_VERSION=$(node -p "require('./package.json').packageManager.split('@')[1].split('+')[0]")

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
  sh -c "cp -r /app-source /app && rm -rf /app/node_modules && cd /app && npm install -g --silent pnpm@${PNPM_VERSION} && pnpm install --ignore-scripts && pnpm exec playwright test ${ARGS[@]}"