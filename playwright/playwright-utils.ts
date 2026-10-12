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

const resolveOptionLabel = async (
  listbox: Locator,
  value: SelectOptionValue
): Promise<string> => {
  if (typeof value === 'object' && 'label' in value) {
    return value.label
  }
  const optionValue = typeof value === 'string' ? value : value.value
  // Listan <li>-vaihtoehdoilla ei ole arvoa omana attribuuttinaan (ks.
  // dropdown-popup dropdown_component.cljs:ssä) — se on koodattu
  // data-test-idn loppuun ("...-option-<arvo>").
  const option = listbox.locator(`[data-test-id$="-option-${optionValue}"]`)
  if ((await option.count()) === 0) {
    // Kutsuja on saattanut antaa jo valmiiksi näkyvän tekstin (esim.
    // kuntakoodiston sijasta suoraan kunnan nimen) eikä arvoa vastaavaa
    // vaihtoehtoa löytynyt — käytetään sitä silloin sellaisenaan
    // hakusanana sen sijaan, että jäätäisiin odottamaan olematonta
    // vaihtoehtoa.
    return optionValue
  }
  const text = await option.textContent()
  return text ?? optionValue
}

const selectFilteredDropdownOption = async (
  input: Locator,
  value: SelectOptionValue
) => {
  const listboxId = await input.getAttribute('aria-controls')
  if (!listboxId) {
    throw new AssertionError({
      message: 'Dropdown combobox has no aria-controls attribute',
    })
  }
  const listbox = input.page().locator(`[id="${listboxId}"]`)
  const label = await resolveOptionLabel(listbox, value)
  await input.click()
  await input.fill(label)
  // .first(): suodatettu lista voi sisältää useita osumia (esim. haku "suo"
  // löytää sekä "Suomi" että "Suomen ..." -alkuiset vaihtoehdot) — annettua
  // label-arvoa vastaava vaihtoehto on näistä ylin.
  await listbox.getByRole('option').first().click()
  // Valinnan klikkaus sulkee popupin (ks. on-option-click/collapse-dropdown
  // dropdown_component.cljs:ssä), mutta sulkeutuminen tapahtuu vasta
  // seuraavassa renderöinnissä eikä välttömästi ehdi valmiiksi ennen kuin
  // tämä funktio palaa. Jos lomakkeella on vain vähän kenttiä home-town-
  // tyyppisen ison koodistopudotusvalikon jälkeen, seuraava klikkaus voi
  // osua vielä auki olevaan/juuri sulkeutuvaan popupiin sen sijaan, että
  // osuisi oikeaan kohteeseensa — odotetaan siis popupin oikeaa piiloutumista.
  await listbox.waitFor({ state: 'hidden' })
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
  let role = await locator.getAttribute('role')
  let tagName = await locator.evaluate((el) => el.tagName.toLowerCase())

  if (role !== 'combobox' && tagName !== 'select') {
    // locator saattaa osoittaa koko kentän wrapperiin (esim.
    // .application__form-field) eikä suoraan combobox-inputiin tai
    // <select>iin — etsitään todellinen kohde sen sisältä.
    const inner = locator.locator('[role="combobox"], select').first()
    if ((await inner.count()) > 0) {
      locator = inner
      role = await inner.getAttribute('role')
      tagName = await inner.evaluate((el) => el.tagName.toLowerCase())
    }
  }

  if (role === 'combobox') {
    await selectFilteredDropdownOption(locator, value)
  } else if (tagName === 'select') {
    await locator.selectOption(value)
  } else {
    throw new AssertionError({
      message: `Cannot select option for element with tag name "${tagName}" and role "${role}"`,
    })
  }

  // Jos lomake täytetään ilman taukoja, lähettäessä jotkin lomakkeen kentät ovat tyhjiä.
  // eslint-disable-next-line playwright/no-wait-for-timeout
  await page.waitForTimeout(50)
}

export const getDropdownOptionValue = async (
  input: Locator
): Promise<string | null> => {
  const listboxId = await input.getAttribute('aria-controls')
  if (!listboxId) {
    throw new AssertionError({
      message: 'Dropdown combobox has no aria-controls attribute',
    })
  }
  const listbox = input.page().locator(`[id="${listboxId}"]`)
  const selected = listbox.locator('[aria-selected="true"]')
  if ((await selected.count()) === 0) {
    return null
  }
  const dataTestId = await selected.getAttribute('data-test-id')
  const match = dataTestId?.match(/-option-(.+)$/)
  return match ? match[1] : null
}
