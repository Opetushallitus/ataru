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

  await kirjauduVirkailijanNakymaan(page)

  await page.route('/lomake-editori/api/tarjonta/haku**', async (route) => {
    await route.fulfill({
      json: { oid: '1.2.246.562.29.00000000000000009710', yhteishaku: true },
    })
  })

  await page.route(
    '/lomake-editori/api/tarjonta/haku/1.2.246.562.29.00000000000000009710',
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
    // FIXME: Jos lomakkeen nimen syöttää liian aikaisin, automaattitallennus ei triggeröidy! https://jira.eduuni.fi/browse/OY-4642
    await page.waitForTimeout(800)
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

const haeOtsikko = (page: Page) => page.getByTestId('properties-header')
const haeDemoAlkaa = (page: Page) => page.getByTestId('demo-validity-start')
const haeDemoPaattyy = (page: Page) => page.getByTestId('demo-validity-end')
const haeLinkkiDemoon = (page: Page) => page.getByTestId('demo-link')

test.describe('Lomake-editori Yleiset asetukset -osio', () => {
  test('Näyttää Yleiset asetukset', async () => {
    const otsikko = haeOtsikko(page)
    await expect(otsikko).toBeVisible()
    await expect(otsikko).toHaveText('Yleiset asetukset')
  })

  test('Näyttää demon alkamisajankohdan valinnan', async () => {
    const demoAlkaa = haeDemoAlkaa(page)
    await expect(demoAlkaa).toBeVisible()
  })

  test('Näyttää demon päättymisajankohdan valinnan', async () => {
    const demoPaattyy = haeDemoPaattyy(page)
    await expect(demoPaattyy).toBeVisible()
  })

  test('Demolinkkiä ei näytetä', async () => {
    const linkkiDemoon = haeLinkkiDemoon(page)
    await expect(linkkiDemoon).not.toBeVisible()
  })

  test('Demon aikavälin asettaminen toimii', async () => {
    const demoAlkaa = haeDemoAlkaa(page)
    const demoPaattyy = haeDemoPaattyy(page)

    await demoAlkaa.fill('2021-01-01')
    await demoPaattyy.fill('2021-12-31')
  })
})
