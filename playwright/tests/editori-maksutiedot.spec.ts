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

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page, 'SUPERUSER')

  await page.route('**/lomake-editori/api/tarjonta/haku**', async (route) => {
    await route.fulfill({
      json: [{ oid: '1.2.246.562.29.00000000000000009710', yhteishaku: true }],
    })
  })

  await page.route(
    '**/lomake-editori/api/tarjonta/haku/1.2.246.562.29.00000000000000009710',
    async (route) => {
      await route.fulfill({
        json: { yhteishaku: true },
      })
    }
  )

  const lomake = await lisaaLomake(page)

  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    const nameInput = page.getByTestId('form-name-input')
    await nameInput.fill('Testilomake')
  })
})

test.afterAll(async ({ request }) => {
  await request.delete(getLomakkeenPoistamisenOsoite(), {
    data: {
      formKey: lomakkeenAvain,
    },
  })
  await page.close()
})
const haeTogglePayment = (page: Page) =>
  page.getByTestId('toggle-maksutoiminto')
const haeTutuPaymentRadio = (page: Page) =>
  page.getByTestId('maksutyyppi-tutu-radio')
const haeAstuPaymentRadio = (page: Page) =>
  page.getByTestId('maksutyyppi-astu-radio')

test.describe('Lomake-editori maksutiedot', () => {
  test('oletuksena tutu-maksu', async () => {
    const toggle = haeTogglePayment(page)
    const tutuRadio = haeTutuPaymentRadio(page)

    await toggle.click()
    await expect(tutuRadio).toBeChecked()
  })

  test('maksutyypin voi vaihtaa', async () => {
    const astu = haeAstuPaymentRadio(page)

    await astu.click()
    await expect(astu).toBeChecked()
  })
})
