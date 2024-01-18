import { Locator, Page } from '@playwright/test'

export const kirjauduVirkailijanNakymaan = async (
  page: Page,
  ticket?: string
): Promise<void> => {
  await page.goto(`/lomake-editori/auth/cas?ticket=${ticket || 'DEVELOPER'}`)
}

export const getSensitiveAnswer = (page: Page): Locator =>
  page.getByTestId('checkbox-sensitive-answer')

type HttpMethod = 'PUT' | 'POST' | 'GET' | 'DELETE'

export const waitForResponse = (
  page: Page,
  method: HttpMethod,
  urlMatcher: (url: string) => boolean
) =>
  page.waitForResponse((response) => {
    return response.request().method() === method && urlMatcher(response.url())
  })
