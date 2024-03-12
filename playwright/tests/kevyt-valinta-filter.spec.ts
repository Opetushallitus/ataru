import { test, expect, Page } from '@playwright/test'

import { kirjauduVirkailijanNakymaan } from '../playwright-ataru-utils'
import { fixtureFromFile } from '../playwright-utils'

let page: Page

test.describe.configure({ mode: 'serial' })

test.describe('Hakemusten filtteröinti kevyt-valinnan tiedoilla', async () => {
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

    await page.route('**/lomake-editori/api/applications/list', (route) => {
      if (route.request().method() === 'POST') {
        return fixtureFromFile('hakemuksetkevyt.json')(route)
      }
    })

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

  test('Hakemuksia voi filtteröidä vastaanoton tilalla', async () => {
    await page.locator('div.section-link.application > a').click()
    await page.locator('.application__search-control-haku').first().click()
    await page.getByTestId('show-results').click()

    const applicationListItems = page
      .locator('.application-handling__list--expanded')
      .locator('.application-handling__list-row')

    await expect(applicationListItems).toHaveCount(2)

    await page.locator('.application-handling__list-row--vastaanotto').click()

    await page
      .locator(
        '.application-handling__filter-state-selection-row--all > label > input'
      )
      .click()

    await expect(applicationListItems).toHaveCount(0)

    const filterStateSelection = page
      .locator('.application-handling__filter-state-selection')
      .locator('span')

    await expect(filterStateSelection).toHaveCount(7)

    await expect(
      filterStateSelection.filter({ hasText: 'Vastaanottanut (1)' })
    ).toHaveCount(1)

    await expect(
      filterStateSelection.filter({ hasText: 'Kesken (1)' })
    ).toHaveCount(1)

    await filterStateSelection
      .filter({ hasText: 'Vastaanottanut (1)' })
      .first()
      .click()

    await expect(applicationListItems).toHaveCount(1)
  })
})
