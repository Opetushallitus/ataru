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
let lomakkeenIlmanTyyppiaAvain: string | undefined
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
  for (const formKey of [lomakkeenAvain, lomakkeenIlmanTyyppiaAvain]) {
    if (formKey) {
      await request.delete(getLomakkeenPoistamisenOsoite(), {
        data: { formKey },
      })
    }
  }
  await page.close()
})

const haeLomaketyyppiSelect = (page: Page) =>
  page.getByTestId('lomaketyyppi-input')

test.describe('Lomake-editori lomaketyyppi', () => {
  test('näytetään rekisterinpitäjälle', async () => {
    await expect(haeLomaketyyppiSelect(page)).toBeVisible()
    await expect(haeLomaketyyppiSelect(page)).toBeEnabled()
  })

  test('kaikki lomaketyypit ovat valittavissa', async () => {
    const options = haeLomaketyyppiSelect(page).locator('option')

    await expect(options).toHaveText([
      'Opiskelijavalinta',
      'TUTU',
      'YKI',
      'Muu'
    ])
  })

  test('uuden lomakkeen oletuksena opiskelijavalinta', async () => {
    await expect(haeLomaketyyppiSelect(page)).toHaveValue('opiskelijavalinta')
  })

  test('lomaketyypin voi vaihtaa ja se tallentuu', async () => {
    await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
      await haeLomaketyyppiSelect(page).selectOption('tutu')
    })
    await expect(haeLomaketyyppiSelect(page)).toHaveValue('tutu')

    await page.reload()

    await expect(haeLomaketyyppiSelect(page)).toHaveValue('tutu')
  })

  test('oletuksena opiskelijavalinta, jos lomakkeelle ei ole tallennettu lomaketyyppiä', async () => {
    // Simuloi vanhaa lomaketta: poista lomaketyyppi uuden lomakkeen
    // tallennuspyynnöstä, jolloin sitä ei tallennu lomakkeen ominaisuuksiin.
    const uudenLomakkeenOsoite = '**/lomake-editori/api/forms'
    await page.route(uudenLomakkeenOsoite, async (route) => {
      const request = route.request()
      if (request.method() !== 'POST') {
        return route.continue()
      }
      const body = request.postDataJSON()
      if (body?.properties) {
        delete body.properties.lomaketyyppi
      }
      await route.continue({ postData: JSON.stringify(body) })
    })

    const lomake = await lisaaLomake(page)
    await page.unroute(uudenLomakkeenOsoite)

    lomakkeenIlmanTyyppiaAvain = unsafeFoldOption(lomake.lomakkeenAvain)

    await expect(haeLomaketyyppiSelect(page)).toHaveValue('opiskelijavalinta')

    await page.reload()

    await expect(haeLomaketyyppiSelect(page)).toHaveValue('opiskelijavalinta')
  })
})
