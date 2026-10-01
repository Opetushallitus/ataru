import path from 'node:path'
import { AssertionError } from 'node:assert'

import { Locator, Page, Response, Route } from '@playwright/test'
import * as Record from 'fp-ts/lib/Record'
import * as Option from 'fp-ts/lib/Option'

type HttpMethod = 'PUT' | 'POST' | 'GET' | 'DELETE'

export const waitForResponse = (
  page: Page,
  method: HttpMethod,
  urlMatcher: (url: string) => boolean
) =>
  page.waitForResponse((response) => {
    return response.request().method() === method && urlMatcher(response.url())
  })

export const getJsonResponseKey = async <T>(res: Response, key: string) => {
  const body = await res.json()
  return Record.has(key, body) ? Option.some(body[key] as T) : Option.none
}

export const unsafeFoldOption = <T>(o: Option.Option<T>): T => {
  return Option.fold<T, T>(
    () => {
      throw new AssertionError({ message: 'Option was None' })
    },
    (val) => val
  )(o)
}

const FIXTURES_PATH = path.resolve(__dirname, '../cypress/fixtures')

export const getFixturePath = (fileName: string) =>
  path.resolve(FIXTURES_PATH, fileName)

export const fixtureFromFile = (fileName: string) => (route: Route) =>
  route.fulfill({ path: getFixturePath(fileName) })

export const fillField = async (
  page: Page,
  locator: Locator,
  value: string
) => {
  await locator.click()
  await locator.fill(value)
  // Jos lomake täytetään ilman taukoja, lähettäessä jotkin lomakkeen kentät ovat tyhjiä.
  // eslint-disable-next-line playwright/no-wait-for-timeout
  await page.waitForTimeout(50)
}

type SelectOptionValue = string | { label: string } | { value: string }

// ataru.application-common.components.dropdown-component renderöi haetulla
// desktop-leveydellä pudotusvalikon nappi+haku+lista-käyttöliittymänä ja piilottaa
// natiivin <select>-elementin CSS:llä (se näytetään vain mobiilileveyksillä). Tällöin
// selectOption ei toimi suoraan natiiviin elementtiin, vaan valinta pitää tehdä
// näkyvän käyttöliittymän kautta.
const isHiddenNativeDropdownFallback = async (locator: Locator) =>
  (await locator.evaluate((el) => el.tagName)) === 'SELECT' &&
  !(await locator.isVisible())

const dropdownFieldLocator = (nativeSelect: Locator): Locator =>
  nativeSelect
    .locator(
      'xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " a-dropdown ")]'
    )
    .locator('.a-dropdown-field')

// Kentän id osuu aina pudotusvalikon natiiviin <select>-elementtiin (ks.
// yllä), joka on työpöytäleveydellä aina CSS:llä piilotettu riippumatta
// siitä, onko koko kenttä käyttäjälle näkyvissä. Näkyvyysväitteissä pitää
// siis tarkistaa sen sijaan komponentin oikeasti näkyvä kenttä.
export const getVisibleFieldLocator = async (
  locator: Locator
): Promise<Locator> => {
  // Kenttä voi ilmestyä DOM:iin viiveellä (esim. sääntöjen paljastamana),
  // joten odotetaan sen kiinnittymistä ennen tag-nimen tarkistusta — muuten
  // tarkistus voisi osua hetkeen, jolloin elementtiä ei vielä ole, ja
  // palauttaisi virheellisesti alkuperäisen (ohjaamattoman) locatorin.
  try {
    await locator.waitFor({ state: 'attached' })
  } catch {
    return locator
  }
  const tagName = await locator.evaluate((el) => el.tagName)
  return tagName === 'SELECT' ? dropdownFieldLocator(locator) : locator
}

const resolveOptionLabel = async (
  nativeSelect: Locator,
  value: SelectOptionValue
): Promise<string> => {
  if (typeof value === 'object' && 'label' in value) {
    return value.label
  }
  const optionValue = typeof value === 'string' ? value : value.value
  const option = nativeSelect.locator(`option[value="${optionValue}"]`)
  if ((await option.count()) === 0) {
    // Kutsuja on saattanut antaa jo valmiiksi näkyvän tekstin (esim.
    // kuntakoodiston sijasta suoraan kunnan nimen) eikä option-elementin
    // value-attribuuttia — käytetään sitä silloin sellaisenaan hakusanana
    // sen sijaan, että jäätäisiin odottamaan olematonta option-elementtiä.
    return optionValue
  }
  const text = await option.textContent()
  return text ?? optionValue
}

const selectFilteredDropdownOption = async (
  nativeSelect: Locator,
  value: SelectOptionValue
) => {
  const label = await resolveOptionLabel(nativeSelect, value)
  const dropdown = nativeSelect.locator(
    'xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " a-dropdown ")]'
  )
  const input = dropdownFieldLocator(nativeSelect).locator('.a-dropdown-input')
  await input.click()
  await input.fill(label)
  await dropdown
    .locator('.a-dropdown-list .a-dropdown-list__option')
    .first()
    .click()
}

// Pudotusvalikon natiivi <select> on aria-hidden, joten getByRole('combobox', ...)
// ei löydä sitä sen label-tekstin perusteella. Etsitään sen sijaan kenttä <label
// for="...">-yhteyden kautta.
export const getFieldByLabel = async (
  scope: Page | Locator,
  text: string | RegExp
): Promise<Locator> => {
  const label = scope.locator('label', { hasText: text }).first()
  const forId = await label.getAttribute('for')
  if (!forId) {
    throw new AssertionError({
      message: `Label matching ${text} has no "for" attribute`,
    })
  }
  return scope.locator(`[id="${forId}"]`)
}

export const selectOption = async (
  page: Page,
  locator: Locator,
  value: SelectOptionValue
) => {
  if (await isHiddenNativeDropdownFallback(locator)) {
    await selectFilteredDropdownOption(locator, value)
  } else {
    await locator.selectOption(value)
  }
  // Jos lomake täytetään ilman taukoja, lähettäessä jotkin lomakkeen kentät ovat tyhjiä.
  // eslint-disable-next-line playwright/no-wait-for-timeout
  await page.waitForTimeout(50)
}
