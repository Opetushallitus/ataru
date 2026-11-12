import { defineConfig, globalIgnores } from 'eslint/config'
import globals from 'globals'
import tseslint from 'typescript-eslint'
import prettierRecommended from 'eslint-plugin-prettier/recommended'
import preferArrow from 'eslint-plugin-prefer-arrow'
import playwright from 'eslint-plugin-playwright'

export default defineConfig([
  globalIgnores([
    'bin/',
    'out/',
    'resources/',
    'cypress/',
    'target/',
    '.clj-kondo/',
  ]),
  {
    files: ['**/*.{ts,js}'],
    extends: [tseslint.configs.recommended, prettierRecommended],
    plugins: {
      'prefer-arrow': preferArrow,
    },
    languageOptions: {
      globals: globals.browser,
    },
    rules: {
      'prettier/prettier': 'error',
      'prefer-arrow/prefer-arrow-functions': 'error',
      'array-callback-return': 'off',
      'prefer-const': 'error',
      'no-var': 'error',
      '@typescript-eslint/no-unused-vars': 'warn',
    },
  },
  {
    files: ['playwright/*.ts', 'playwright/tests/*.ts'],
    extends: [playwright.configs['flat/recommended']],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.json'],
      },
    },
    rules: {
      'playwright/expect-expect': 'off',
      '@typescript-eslint/no-floating-promises': 'error',
    },
  },
])
