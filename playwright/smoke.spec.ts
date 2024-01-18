import { test, expect, Page } from '@playwright/test'

const kirjauduVirkailijanNakymaan = async (page: Page, ticket?: string) => {
  await page.goto(`/lomake-editori/auth/cas?ticket=${ticket || 'DEVELOPER'}`)
}

test('Smoke', async ({ page }) => {
  await kirjauduVirkailijanNakymaan(page)
  await page.goto('/lomake-editori')
  await expect(page.getByRole('heading', { name: 'Lomakkeet' })).toBeVisible()
})
