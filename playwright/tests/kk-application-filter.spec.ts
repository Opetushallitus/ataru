import { test, expect, Page } from '@playwright/test'

import { kirjauduVirkailijanNakymaan } from '../playwright-ataru-utils'
import { fixtureFromFile } from '../playwright-utils'

let page: Page

test.describe.configure({ mode: 'serial' })

interface ListRequest {
  'states-and-filters': {
    filters: {
      'kk-application-payment': Record<string, boolean>
    }
  }
}

test.describe('KK-hakemusmaksufiltteri', () => {
  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage()

    await page.route(
      '**/lomake-editori/api/valinta-tulos-service/valinnan-tulos/hakemus*',
      (route) => {
        if (route.request().method() === 'POST') {
          return fixtureFromFile('valinnantulokset.json')(route)
        } else if (route.request().url().includes('?hakemusOid=')) {
          return route.fulfill({ json: [] })
        }
      }
    )

    await page.route(
      '**/lomake-editori/api/valintalaskentakoostepalvelu/valintaperusteet/hakukohde/1.2.246.562.20.10000000001/kayttaa-valintalaskentaa',
      fixtureFromFile('kayttaavalintalaskentaa.json')
    )

    await page.route(
      '**/lomake-editori/api/haut*',
      fixtureFromFile('hautkevyt.json')
    )

    await page.route(
      '**/lomake-editori/api/haku*',
      fixtureFromFile('hautkevyt.json')
    )

    await kirjauduVirkailijanNakymaan(page, '1.2.246.562.11.11111111111')
  })

  test('lähetetään oletusarvoilla, kun haku on valittu', async () => {
    await page.route(
      '**/lomake-editori/api/applications/list',
      async (route) => {
        const body = route.request().postDataJSON() as ListRequest
        const defaultKkApplicationPaymentFilter = {
          'not-checked': true,
          'not-required': true,
          awaiting: false,
          'ok-by-proxy': true,
          paid: true,
          overdue: false,
        }
        expect(
          body?.['states-and-filters']?.filters?.['kk-application-payment']
        ).toEqual(defaultKkApplicationPaymentFilter)

        if (route.request().method() === 'POST') {
          return await fixtureFromFile('hakemuksetkevyt.json')(route)
        }
      }
    )

    await page.locator('div.section-link.application > a').click()
    await page.locator('.application__search-control-haku').first().click()
    await page.getByTestId('show-results').click()

    const applicationListItems = page
      .locator('.application-handling__list--expanded')
      .locator('.application-handling__list-row')

    await expect(applicationListItems).toHaveCount(2)
  })

  test('lähetetään kaikki arvot valittuna', async () => {
    await page.route(
      '**/lomake-editori/api/applications/list',
      async (route) => {
        const body = route.request().postDataJSON() as ListRequest
        const defaultKkApplicationPaymentFilter = {
          'not-checked': true,
          'not-required': true,
          awaiting: true,
          'ok-by-proxy': true,
          paid: true,
          overdue: true,
        }
        expect(
          body?.['states-and-filters']?.filters?.['kk-application-payment']
        ).toEqual(defaultKkApplicationPaymentFilter)

        if (route.request().method() === 'POST') {
          return await fixtureFromFile('hakemuksetkevyt.json')(route)
        }
      }
    )

    await page.locator('div.section-link.application > a').click()
    await page
      .locator(
        '.application__search-control-tab-selector-wrapper--search > a.application__search-control-tab-selector-link'
      )
      .click()
    await page
      .locator('input.application__search-control-search-term-input')
      .fill('test')

    const applicationListItems = page
      .locator('.application-handling__list--expanded')
      .locator('.application-handling__list-row')

    await expect(applicationListItems).toHaveCount(2)
  })
})
