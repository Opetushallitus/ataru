#!/bin/bash
set -euo pipefail

PLAYWRIGHT_VERSION=$(node -e "console.log(require('@playwright/test/package.json').version)")

# Ensure pnpm store is available inside the container (pnpm node_modules symlink to it)
STORE_PATH=$(pnpm store path 2>/dev/null || true)
STORE_MOUNT=()
if [[ -n "${STORE_PATH}" && -d "${STORE_PATH}" ]]; then
  echo "Mounting pnpm store from ${STORE_PATH}"
  STORE_MOUNT=(--volume "${STORE_PATH}:${STORE_PATH}:ro")
fi

echo "Running Playwright tests in Docker image mcr.microsoft.com/playwright:v${PLAYWRIGHT_VERSION}"

# Normalize args: strip a leading standalone "--" (pnpm run ... -- --project=foo)
ARGS=("$@")
if [[ "${ARGS[0]:-}" == "--" ]]; then
  ARGS=("${ARGS[@]:1}")
fi

docker run \
  -e CI \
  --mount type=bind,source="$PWD",target=/app \
  "${STORE_MOUNT[@]}" \
  --user "$(id -u):$(id -g)" \
  -w /app \
  --ipc=host \
  --net=host \
  mcr.microsoft.com/playwright:v"$PLAYWRIGHT_VERSION" \
  corepack pnpm exec playwright test "${ARGS[@]}"