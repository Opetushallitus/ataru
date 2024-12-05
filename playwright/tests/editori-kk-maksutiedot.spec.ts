import { test, expect, Page } from '@playwright/test'
import { unsafeFoldOption } from '../playwright-utils'
import {
  getLomakkeenPoistamisenOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let lomakkeenAvain: string
let lomakkeenId: number
let page: Page

test.beforeAll(async ({ browser, request }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page, 'SUPERUSER')

  const lomake = await lisaaLomake(page)

  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    const nameInput = page.getByTestId('form-name-input')
    await nameInput.fill('KKHakemusMaksuTestilomake')
  })

  await page.request.put(
    `lomake-editori/api/forms/${lomakkeenAvain}/upsert-kk-application-payment-module`
  )
  await page.goto(`lomake-editori/editor/${lomakkeenAvain}`)
})

test.afterAll(async ({ request }) => {
  await request.delete(getLomakkeenPoistamisenOsoite(), {
    data: {
      formKey: lomakkeenAvain,
    },
  })
  await page.close()
})

test.describe('Lomake-editori kk-hakemusmaksumoduuli', () => {
  test('sisältää maksumoduulin', async () => {
    await expect(page.getByText('Hakemusmaksu ja lukuvuosimaksu')).toBeVisible()
  })
})
