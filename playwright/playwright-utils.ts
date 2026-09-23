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

// Jos pelkkä string, voi olla value tai label
type SelectOption = string | { label: string } | { value: string }

const resolveOptionLabel = async (
  listbox: Locator,
  option: SelectOption
): Promise<string> => {
  if (typeof option === 'object' && 'label' in option) {
    return option.label
  }
  const optionValue = typeof option === 'string' ? option : option.value

  // Koodistopohjaiset (esim. maa/kansalaisuus/kotikunta) listat ladataan
  // asynkronisesti eivätkä siis välttämättä ole vielä renderöityneet heti
  // popupin avautuessa
  await listbox
    .getByRole('option')
    .first()
    .waitFor({ state: 'attached', timeout: 5000 })

  // string-tyyppinen option voi olla joko value tai label
  let optionEl = listbox.locator(`[data-value="${optionValue}"]`)
  if (typeof option === 'string' && (await optionEl.count()) === 0) {
    optionEl = listbox.getByRole('option', { name: option })
  }

  const text = await optionEl.textContent()
  return text ?? optionValue
}

const selectFilteredDropdownOption = async (
  input: Locator,
  value: SelectOption
) => {
  const listboxId = await input.getAttribute('aria-controls')
  if (!listboxId) {
    throw new AssertionError({
      message: 'Dropdown combobox has no aria-controls attribute',
    })
  }
  const listbox = input.page().locator(`[id="${listboxId}"]`)
  // Koodistopohjaiset (esim. maa/kansalaisuus/kotikunta) listat ladataan
  // vasta kentän avaamisen yhteydessä eivätkä siis ole vielä olemassa
  // DOM:issa ollenkaan ennen tätä klikkausta
  await input.click()
  const label = await resolveOptionLabel(listbox, value)
  await input.fill(label)
  await listbox.getByRole('option', { name: label }).first().click()
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

const getDropdown = async (
  locator: Locator
): Promise<{
  type: 'filtered' | 'native'
  locator: Locator
}> => {
  let role = await locator.getAttribute('role')
  let tagName = await locator.evaluate((el) => el.tagName.toLowerCase())
  let input = locator

  if (role !== 'combobox' && tagName !== 'select') {
    // locator saattaa osoittaa koko kentän wrapperiin (esim.
    // .application__form-field)
    input = locator.locator('[role="combobox"], select').first()
    if ((await input.count()) > 0) {
      role = await input.getAttribute('role')
      tagName = await input.evaluate((el) => el.tagName.toLowerCase())
    }
  }

  if (role === 'combobox') {
    return { type: 'filtered', locator: input }
  } else if (tagName === 'select') {
    return { type: 'native', locator: input }
  } else {
    throw new AssertionError({
      message: `Cannot determine dropdown type for element with tag name "${tagName}" and role "${role}"`,
    })
  }
}

export const selectOption = async (
  page: Page,
  locator: Locator,
  value: SelectOption
) => {
  const { type: dropdownType, locator: dropdownLocator } =
    await getDropdown(locator)

  switch (dropdownType) {
    case 'filtered':
      return selectFilteredDropdownOption(dropdownLocator, value)
    case 'native':
      return dropdownLocator.selectOption(value)
    default:
      throw new AssertionError({
        message: `Cannot select option for element with unknown dropdown type "${dropdownType}"`,
      })
  }
}

export const getDropdownOptionValue = async (
  input: Locator
): Promise<string | null> => {
  return input.getAttribute('data-selected-option-value')
}
