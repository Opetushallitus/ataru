#!/bin/bash
set -euo pipefail

PLAYWRIGHT_VERSION=$(node -e "console.log(require('@playwright/test/package.json').version)")

docker pull mcr.microsoft.com/playwright:v"$PLAYWRIGHT_VERSION"