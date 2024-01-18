import { test, expect } from '@playwright/test'
import { kirjauduVirkailijanNakymaan } from './playwright-utils'

test('Smoke', async ({ page }) => {
  await kirjauduVirkailijanNakymaan(page)
  await page.goto('/lomake-editori')
  await expect(page.getByRole('heading', { name: 'Lomakkeet' })).toBeVisible()
})
