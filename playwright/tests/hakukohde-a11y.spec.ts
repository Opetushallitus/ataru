import { test, expect, Page } from '@playwright/test'

import {
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
} from '../playwright-ataru-utils'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenTunnisteet: { lomakkeenAvain: string; lomakkeenId: number }

const TEST_HAKUKOHDE_OID_1 = '1.2.246.562.20.00000000001'
const TEST_HAKUKOHDE_OID_2 = '1.2.246.562.20.00000000002'

const testTarjontaHakukohteet = [
  {
    oid: TEST_HAKUKOHDE_OID_1,
    archived: false,
    'can-be-applied-to?': true,
    hakuaika: { on: true },
    'opetuskieli-koodi-urit': ['oppilaitoksenopetuskieli_1'],
    hakukohderyhmat: [],
  },
  {
    oid: TEST_HAKUKOHDE_OID_2,
    archived: false,
    'can-be-applied-to?': true,
    hakuaika: { on: true },
    'opetuskieli-koodi-urit': ['oppilaitoksenopetuskieli_1'],
    hakukohderyhmat: [],
  },
]

const testHakukohdeOptions = [
  {
    value: TEST_HAKUKOHDE_OID_1,
    label: { fi: 'Testikoulutus A' },
    description: { fi: 'Testikoulutus A kuvaus' },
  },
  {
    value: TEST_HAKUKOHDE_OID_2,
    label: { fi: 'Testikoulutus B' },
    description: { fi: 'Testikoulutus B kuvaus' },
  },
]

const openSearch = async () => {
  await page.locator('.application__hakukohde-selection-open-search').click()
  await expect(
    page.locator('.application__hakukohde-selection-search-container')
  ).toBeVisible()
}

const ensureSearchClosed = async () => {
  const container = page.locator(
    '.application__hakukohde-selection-search-container'
  )
  if (await container.isVisible()) {
    await page.keyboard.press('Escape')
    await expect(container).toBeHidden()
  }
}

const fillSearchAndWaitForResults = async (
  query: string,
  expectedCount: number
) => {
  const input = page.locator('.application__form-text-input-in-box')
  await input.fill(query)
  await expect(
    page.locator('.application__search-hit-hakukohde-row')
  ).toHaveCount(expectedCount)
}

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
  page = await browser.newPage()

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes('/lomake-editori/api/forms')
    ),
    kirjauduVirkailijanNakymaan(page),
  ])

  const lomake = await lisaaLomake(page)
  lomakkeenTunnisteet = {
    lomakkeenAvain: unsafeFoldOption(lomake.lomakkeenAvain),
    lomakkeenId: unsafeFoldOption(lomake.lomakkeenId),
  }

  // Intercept form fetch and inject test hakukohteet into both tarjonta data
  // and the hakukohteet field options so search returns results
  const formApiPattern = new RegExp(
    `/hakemus/api/form/${lomakkeenTunnisteet.lomakkeenAvain}\\?role=hakija`
  )
  await page.route(formApiPattern, async (route) => {
    const response = await route.fetch()
    const json = await response.json()

    type FormField = {
      fieldType?: string
      options?: unknown
      [key: string]: unknown
    }

    const contentWithOptions = (json.content as FormField[]).map((field) => {
      if (field.fieldType === 'hakukohteet') {
        return { ...field, options: testHakukohdeOptions }
      }
      return field
    })

    await route.fulfill({
      json: {
        ...json,
        tarjonta: {
          ...(json.tarjonta ?? {}),
          hakukohteet: testTarjontaHakukohteet,
        },
        content: contentWithOptions,
      },
    })
  })

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenTunnisteet.lomakkeenAvain))
    ),
    page.goto(getHakijanNakymanOsoite(lomakkeenTunnisteet.lomakkeenAvain)),
  ])
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenTunnisteet.lomakkeenAvain)
  await page.close()
})

test('Hakukohde search input has correct WAI-ARIA combobox attributes', async () => {
  await openSearch()

  const input = page.locator('.application__form-text-input-in-box')
  await expect(input).toHaveAttribute('role', 'combobox')
  await expect(input).toHaveAttribute('aria-haspopup', 'listbox')
  await expect(input).toHaveAttribute('aria-expanded', 'true')
  await expect(input).toHaveAttribute(
    'aria-controls',
    'hakukohde-search-listbox'
  )
  await expect(input).toHaveAttribute('aria-autocomplete', 'list')
})

test('Hakukohde search results container has listbox role, matching id, and visible label via aria-labelledby', async () => {
  const results = page.locator(
    '.application__hakukohde-selection-search-results'
  )
  await expect(results).toHaveAttribute('role', 'listbox')
  await expect(results).toHaveAttribute('id', 'hakukohde-search-listbox')
  await expect(results).toHaveAttribute(
    'aria-labelledby',
    'hakukohde-search-results-label'
  )

  const label = page.locator('#hakukohde-search-results-label')
  await expect(label).toBeVisible()
  await expect(label).toHaveText('Hakutulokset')
})

test('No-results message has role=status when search returns no hits', async () => {
  const input = page.locator('.application__form-text-input-in-box')
  await input.fill('zzzzzzzzz')

  const noHits = page.locator(
    '.application__hakukohde-selection-search-no-hits'
  )
  await expect(noHits).toBeVisible()
  await expect(noHits).toHaveAttribute('role', 'status')

  // Restore blank query for subsequent tests
  await input.fill('')
})

test('Hakukohde search result rows have option role, aria-selected, and boolean aria-disabled', async () => {
  await fillSearchAndWaitForResults('Te', 2)

  const rows = page.locator('.application__search-hit-hakukohde-row')
  const firstRow = rows.first()

  await expect(firstRow).toHaveAttribute('role', 'option')
  await expect(firstRow).toHaveAttribute('aria-selected', 'false')

  const addButton = page
    .locator('.application__search-hit-hakukohde-row--select-button')
    .first()
  const ariaDisabled = await addButton.getAttribute('aria-disabled')
  expect(ariaDisabled).toMatch(/^(true|false)$/)
})

test('ArrowDown from search input moves focus to first result', async () => {
  // Search input already has 'Te' from previous test with 2 results visible
  const input = page.locator('.application__form-text-input-in-box')
  await input.click()
  await input.press('ArrowDown')

  await expect(
    page
      .locator('.application__search-hit-hakukohde-row--select-button')
      .first()
  ).toBeFocused()
})

test('ArrowDown/Up keyboard navigation moves between result rows and back to input', async () => {
  // Focus is on first result Lisää button from previous test
  const firstButton = page
    .locator('.application__search-hit-hakukohde-row--select-button')
    .first()
  const secondButton = page
    .locator('.application__search-hit-hakukohde-row--select-button')
    .nth(1)
  const input = page.locator('.application__form-text-input-in-box')

  await page.keyboard.press('ArrowDown')
  await expect(secondButton).toBeFocused()

  await page.keyboard.press('ArrowUp')
  await expect(firstButton).toBeFocused()

  await page.keyboard.press('ArrowUp')
  await expect(input).toBeFocused()
})

test('Escape key closes search panel and returns focus to open-search button', async () => {
  // Input is focused from previous test
  await page.keyboard.press('Escape')

  await expect(
    page.locator('.application__hakukohde-selection-search-container')
  ).toBeHidden()
  await expect(
    page.locator('.application__hakukohde-selection-open-search')
  ).toBeFocused()
})

test('Clicking Lisää moves focus to selected row; selected row has aria-selected=true; checkmark icon has aria-hidden=true', async () => {
  await openSearch()
  await fillSearchAndWaitForResults('Te', 2)

  // Click Lisää on first result
  await page
    .locator('.application__search-hit-hakukohde-row--select-button')
    .first()
    .click()

  // After re-render, first row is selected (aria-selected=true) and gets focus
  const selectedRow = page.locator(
    '.application__search-hit-hakukohde-row[aria-selected="true"]'
  )
  await expect(selectedRow).toBeFocused()
  await expect(selectedRow).toHaveAttribute('aria-selected', 'true')

  // Checkmark icon should be hidden from assistive technologies
  const checkIcon = page.locator(
    '.application__search-hit-hakukohde-row--selected-check'
  )
  await expect(checkIcon).toHaveAttribute('aria-hidden', 'true')
})

test('Arrow navigation includes selected hakukohde rows in its sequence', async () => {
  // First hakukohde is selected; focus is on its row div from previous test.
  // nav-sel matches: selected row div (aria-selected=true) AND second Lisää button.
  // ArrowDown from input should reach selected row first.
  const input = page.locator('.application__form-text-input-in-box')
  await input.click()
  await input.press('ArrowDown')

  // First nav-sel match is the selected row (aria-selected=true)
  await expect(
    page.locator('.application__search-hit-hakukohde-row[aria-selected="true"]')
  ).toBeFocused()

  // ArrowDown from selected row reaches the only remaining Lisää button (first hakukohde is selected, so its button is replaced by a checkmark; .first() points to the second hakukohde's button)
  await page.keyboard.press('ArrowDown')
  await expect(
    page
      .locator('.application__search-hit-hakukohde-row--select-button')
      .first()
  ).toBeFocused()

  await ensureSearchClosed()
})
