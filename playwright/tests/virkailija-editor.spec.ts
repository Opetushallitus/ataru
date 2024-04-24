import { test, expect, Page, Locator } from '@playwright/test'
import {
  getLomakkeenPoistamisenOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let formCount: number
let lomakkeenId: number
let lomakkeenAvain: string
let lomakkeen2Avain: string

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()
  await page.route(
    '**/lomake-editori/api/forms**',
    async (route) => {
      const response = await route.fetch()
      const json = await response.json()
      formCount = json['forms'].length
      await route.fulfill({ response })
    },
    { times: 1 }
  )

  await kirjauduVirkailijanNakymaan(page)
  await waitForResponse(page, 'GET', (url) =>
    url.includes('/lomake-editori/api/forms')
  )

  const lomake = await lisaaLomake(page)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    const nameInput = page.getByTestId('form-name-input')
    await nameInput.fill('Tyhjä lomake')
  })
})

test.afterAll(async ({ request }) => {
  await request.delete(getLomakkeenPoistamisenOsoite(), {
    data: {
      formKey: lomakkeenAvain,
    },
  })
  if (lomakkeen2Avain) {
    await request.delete(getLomakkeenPoistamisenOsoite(), {
      data: {
        formKey: lomakkeen2Avain,
      },
    })
  }
  await page.close()
})

const formListItems = (page: Page) => {
  return page.locator('.editor-form__list').locator('a')
}

const formComponents = (page: Page) => {
  return page.locator('.editor-form__component-wrapper')
}

const componentToolbar = (page: Page | Locator) => {
  return page.getByTestId('component-toolbar')
}

const componentSubformToolbar = (page: Page | Locator) => {
  return page.getByTestId('component-subform-toolbar')
}

const clickComponentToolbar = async (
  loc: Page | Locator,
  component: string
) => {
  const toolbar = componentToolbar(loc)
  await toolbar.hover()
  await toolbar.getByTestId('component-toolbar-' + component).click()
}

const clickSubComponentToolbar = async (
  loc: Page | Locator,
  component: string
) => {
  const toolbar = componentSubformToolbar(loc)
  await toolbar.hover()
  await toolbar.getByTestId('component-toolbar-' + component).click()
}

const clickRemoveAndConfirm = async (component: Locator) => {
  await component.locator('.editor-form__component-button').nth(2).click()
  await component.locator('.editor-form__component-button--confirm').click()
}

const getInputs = (page: Page, metaClass: string = '') => {
  return page.locator(
    '.editor-form__panel-container input:not(#editor-form__copy-question-id-container)' +
      metaClass
  )
}

const getComponentButtons = (page: Page, metaClass: string = '') => {
  return page.locator(
    '.editor-form__panel-container .editor-form__component-button' + metaClass
  )
}

test.describe('Editori', () => {
  test('näyttää lomakelistan', async () => {
    const itemCount = await formListItems(page).count()
    expect(itemCount).toBeGreaterThanOrEqual(formCount)
  })

  test('lomakkeen luonti luo lomakkeen oletus kentillä', async () => {
    const lomakkeet = formListItems(page)
    const ensimmainenLomake = lomakkeet.nth(0)
    await expect(
      ensimmainenLomake.locator('.editor-form__list-form-name')
    ).toHaveText('Tyhjä lomake')

    const lomakeKomponentit = formComponents(page)

    const yleisetasetukset = lomakeKomponentit.nth(0)
    await expect(
      yleisetasetukset.locator('.editor-form__component-header')
    ).toHaveText('Yleiset asetukset')

    const hakukohteet = lomakeKomponentit.nth(1)
    await expect(
      hakukohteet.locator('.editor-form__component-header')
    ).toHaveText('Hakukohteet')

    const henkilotiedot = lomakeKomponentit.nth(2)
    await expect(
      henkilotiedot.locator('.editor-form__component-header')
    ).toHaveText('Henkilötiedot')
  })

  test('lisää tekstikenttä', async () => {
    await clickComponentToolbar(page, 'tekstikenttä')
    const textfield = page.getByTestId(
      'editor-form__text-field-component-wrapper'
    )
    await textfield
      .getByTestId('tekstikenttä-kysymys')
      .fill('Tekstikenttä kysymys')
    await textfield.locator('.editor-form__info-addon-checkbox label').click()
    await textfield
      .locator('.editor-form__info-addon-inputs textarea')
      .fill('Tekstikenttä kysymyksen ohjeteksti')
    await expect(textfield.getByTestId('tekstikenttä-kysymys')).toHaveValue(
      'Tekstikenttä kysymys'
    )
    await expect(
      textfield.getByLabel('Kysymys sisältää ohjetekstin')
    ).toBeChecked()
    await expect(
      textfield.locator('.editor-form__info-addon-inputs textarea')
    ).toHaveValue('Tekstikenttä kysymyksen ohjeteksti')
    await expect(
      textfield.locator('.editor-form__button-group input:checked')
    ).toHaveValue('M')
    await expect(textfield.getByLabel('Pakollinen tieto')).not.toBeChecked()
    await expect(
      textfield.getByTestId('tekstikenttä-valinta-voi-lisätä-useita')
    ).not.toBeChecked()
    await expect(
      textfield.getByTestId('tekstikenttä-valinta-kenttään-vain-numeroita')
    ).not.toBeChecked()

    await clickRemoveAndConfirm(textfield)
    await expect(textfield).toBeHidden()
  })

  test('lisää tekstialue', async () => {
    await clickComponentToolbar(page, 'tekstialue')
    const textarea = page.getByTestId(
      'editor-form__text-area-component-wrapper'
    )
    await textarea
      .getByTestId('tekstikenttä-kysymys')
      .fill('Tekstialue kysymys')
    await textarea.locator('.editor-form__info-addon-checkbox label').click()
    await textarea
      .locator('.editor-form__info-addon-inputs textarea')
      .fill('Tekstialue kysymyksen ohjeteksti')
    await textarea
      .locator('.editor-form__button-group div')
      .nth(2)
      .locator('label')
      .click()
    await textarea
      .locator('.editor-form__checkbox-wrapper')
      .getByLabel('Pakollinen')
      .click()
    await textarea.getByTestId('tekstialue-max-merkkimaara').fill('2000')

    await expect(textarea.getByTestId('tekstikenttä-kysymys')).toHaveValue(
      'Tekstialue kysymys'
    )
    await expect(
      textarea.getByLabel('Kysymys sisältää ohjetekstin')
    ).toBeChecked()
    await expect(
      textarea.locator('.editor-form__info-addon-inputs textarea')
    ).toHaveValue('Tekstialue kysymyksen ohjeteksti')
    await expect(
      textarea.locator('.editor-form__button-group input:checked')
    ).toHaveValue('L')
    await expect(textarea.getByLabel('Pakollinen tieto')).toBeChecked()
    await expect(
      textarea.getByTestId('tekstialue-max-merkkimaara')
    ).toHaveValue('2000')

    await clickRemoveAndConfirm(textarea)
    await expect(textarea).toBeHidden()
  })

  test('lisää pudotusvalikko', async () => {
    await clickComponentToolbar(page, 'dropdown')
    const dropdown = page.getByTestId('editor-form__dropdown-component-wrapper')
    await dropdown
      .getByTestId('editor-form__dropdown-label')
      .fill('Pudotusvalikko kysymys')
    const options = dropdown
      .getByTestId('editor-form__multi-options-container')
      .locator('.editor-form__multi-options-wrapper-outer')
    await options.nth(1).locator('input').fill('Ensimmäinen vaihtoehto')
    await options
      .nth(1)
      .locator('.editor-form__multi-options-arrow--up')
      .click()
    await options.nth(1).locator('input').fill('Toinen vaihtoehto')
    await dropdown.locator('.editor-form__add-dropdown-item a').click()
    await options.nth(2).locator('input').fill('Kolmas vaihtoehto')
    await dropdown.locator('.editor-form__info-addon-checkbox label').click()
    await dropdown
      .locator('.editor-form__info-addon-inputs textarea')
      .fill('Pudotusvalikko kysymyksen ohjeteksti')
    await dropdown.locator('.editor-form__add-dropdown-item a').click()
    await options.nth(2).getByTestId('followup-question-followups').click()
    await clickComponentToolbar(dropdown, 'tekstikenttä')
    await dropdown.getByTestId('tekstikenttä-kysymys').fill('Jatkokysymys')

    await expect(
      dropdown.getByTestId('editor-form__dropdown-label')
    ).toHaveValue('Pudotusvalikko kysymys')
    await expect(
      dropdown.locator('.editor-form__checkbox-container input').first()
    ).not.toBeChecked()
    await expect(options).toHaveCount(4)
    await expect(options.nth(0).locator('input')).toHaveValue(
      'Ensimmäinen vaihtoehto'
    )
    await expect(options.nth(1).locator('input')).toHaveValue(
      'Toinen vaihtoehto'
    )
    await expect(options.nth(2).locator('input')).toHaveValue(
      'Kolmas vaihtoehto'
    )
    await expect(options.nth(3).locator('input')).toHaveValue('')
    await expect(
      dropdown.getByLabel('Kysymys sisältää ohjetekstin').first()
    ).toBeChecked()
    await expect(
      dropdown.locator('.editor-form__info-addon-inputs textarea')
    ).toHaveValue('Pudotusvalikko kysymyksen ohjeteksti')
    await expect(dropdown.getByTestId('tekstikenttä-kysymys')).toHaveValue(
      'Jatkokysymys'
    )

    await clickRemoveAndConfirm(dropdown)
    await expect(dropdown).toBeHidden()
  })

  test('lisää pudotusvalikko koodisto', async () => {
    await clickComponentToolbar(page, 'dropdown-koodisto')
    const dropdown = page.getByTestId('editor-form__dropdown-component-wrapper')
    await dropdown
      .getByTestId('editor-form__select-koodisto-dropdown')
      .selectOption('pohjakoulutuseditori')

    await expect(
      dropdown.getByTestId('editor-form__select-koodisto-dropdown')
    ).toHaveValue('pohjakoulutuseditori')

    await clickRemoveAndConfirm(dropdown)
    await expect(dropdown).toBeHidden()
  })

  test('lisää monivalinta', async () => {
    await clickComponentToolbar(page, 'multiple-choice')
    const multipleChoice = page.getByTestId(
      'editor-form__multipleChoice-component-wrapper'
    )
    const options = multipleChoice
      .getByTestId('editor-form__multi-options-container')
      .locator('.editor-form__multi-options-wrapper-outer')
    await multipleChoice.locator('.editor-form__add-dropdown-item a').click()
    await options.nth(0).locator('input').fill('Vaihtoehto 1')
    await multipleChoice.locator('.editor-form__add-dropdown-item a').click()
    await options.nth(1).locator('input').fill('Vaihtoehto 2')
    await multipleChoice.locator('.editor-form__add-dropdown-item a').click()
    await options.nth(2).locator('input').fill('Vaihtoehto 3')
    await multipleChoice.locator('.editor-form__add-dropdown-item a').click()
    await options.nth(1).getByTestId('followup-question-followups').click()
    await clickComponentToolbar(multipleChoice, 'painikkeet-yksi-valittavissa')
    const singleChoice = multipleChoice.getByTestId(
      'editor-form__singleChoice-component-wrapper'
    )
    const singleChoiceOptions = singleChoice
      .getByTestId('editor-form__multi-options-container')
      .locator('.editor-form__multi-options-wrapper-outer')
    await singleChoice
      .getByTestId('editor-form__singleChoice-label')
      .fill('Oletko punavihervärisokea?')
    await singleChoice.locator('.editor-form__add-dropdown-item a').click()
    await singleChoiceOptions
      .nth(0)
      .locator('.editor-form__text-field')
      .fill('Kyllä')
    await singleChoice.locator('.editor-form__add-dropdown-item a').click()
    await singleChoiceOptions
      .nth(1)
      .locator('.editor-form__text-field')
      .fill('En')
    await singleChoice.getByTestId('followup-question-followups').last().click()
    await clickComponentToolbar(singleChoice, 'adjacent-fieldset')
    const adjacentFieldset = singleChoice.getByTestId(
      'editor-form__adjacent-fieldset-component-wrapper'
    )
    await componentToolbar(adjacentFieldset).hover()
    await componentToolbar(adjacentFieldset).locator('li').click()
    await componentToolbar(adjacentFieldset).hover()
    await componentToolbar(adjacentFieldset).locator('li').click()
    await componentToolbar(adjacentFieldset).hover()
    await componentToolbar(adjacentFieldset).locator('li').click()
    await adjacentFieldset
      .locator('.editor-form__adjacent-fieldset-container')
      .locator('.editor-form__text-field')
      .nth(0)
      .fill('Jatkokysymys A')
    await adjacentFieldset
      .locator('.editor-form__adjacent-fieldset-container')
      .locator('.editor-form__text-field')
      .nth(1)
      .fill('Jatkokysymys B')
    await adjacentFieldset
      .locator('.editor-form__adjacent-fieldset-container')
      .locator('.editor-form__text-field')
      .nth(2)
      .fill('Jatkokysymys C')

    await expect(options.nth(0).locator('input')).toHaveValue('Vaihtoehto 1')
    await expect(options.nth(1).locator('input')).toHaveValue('Vaihtoehto 2')
    await expect(options.nth(4).locator('input')).toHaveValue('Vaihtoehto 3')
    await expect(options.nth(5).locator('input')).toHaveValue('')
    await expect(
      singleChoice.getByTestId('editor-form__singleChoice-label')
    ).toHaveValue('Oletko punavihervärisokea?')
    await expect(
      singleChoiceOptions.nth(0).locator('.editor-form__text-field')
    ).toHaveValue('Kyllä')
    await expect(
      singleChoiceOptions.nth(1).locator('.editor-form__text-field')
    ).toHaveValue('En')
    await expect(
      adjacentFieldset
        .locator('.editor-form__adjacent-fieldset-container')
        .locator('.editor-form__text-field')
        .nth(0)
    ).toHaveValue('Jatkokysymys A')
    await expect(
      adjacentFieldset
        .locator('.editor-form__adjacent-fieldset-container')
        .locator('.editor-form__text-field')
        .nth(1)
    ).toHaveValue('Jatkokysymys B')
    await expect(
      adjacentFieldset
        .locator('.editor-form__adjacent-fieldset-container')
        .locator('.editor-form__text-field')
        .nth(2)
    ).toHaveValue('Jatkokysymys C')

    await clickRemoveAndConfirm(multipleChoice)
    await expect(multipleChoice).toBeHidden()
  })

  test('lisää monivalinta koodisto', async () => {
    await clickComponentToolbar(page, 'multiple-choice-koodisto')
    const multipleChoiceKoodisto = page.getByTestId(
      'editor-form__multipleChoice-component-wrapper'
    )
    await multipleChoiceKoodisto
      .getByTestId('editor-form__select-koodisto-dropdown')
      .selectOption('tutkinto')

    await expect(
      multipleChoiceKoodisto.getByTestId(
        'editor-form__select-koodisto-dropdown'
      )
    ).toHaveValue('tutkinto')

    await clickRemoveAndConfirm(multipleChoiceKoodisto)
    await expect(multipleChoiceKoodisto).toBeHidden()
  })

  test('lisää lomakeosio', async () => {
    await clickComponentToolbar(page, 'lomakeosio')
    const lomakeosio = page.getByTestId(
      'editor-form__wrapperElement-component-wrapper'
    )
    await lomakeosio.locator('.editor-form__text-field').fill('Testiosio')

    await clickSubComponentToolbar(lomakeosio, 'tekstialue')

    const tekstialue = lomakeosio.getByTestId(
      'editor-form__text-area-component-wrapper'
    )
    await tekstialue.getByTestId('tekstikenttä-kysymys').fill('Osiokysymys')
    await tekstialue
      .locator('.editor-form__checkbox-wrapper')
      .getByLabel('Pakollinen')
      .click()

    await expect(
      lomakeosio.locator('.editor-form__text-field').nth(0)
    ).toHaveValue('Testiosio')
    await expect(tekstialue.getByTestId('tekstikenttä-kysymys')).toHaveValue(
      'Osiokysymys'
    )
    await expect(
      tekstialue
        .locator('.editor-form__checkbox-wrapper')
        .getByLabel('Pakollinen')
    ).toBeChecked()

    await clickRemoveAndConfirm(lomakeosio)
    await expect(lomakeosio).toBeHidden()
  })

  test('lisää numeerinen tekstikenttä', async () => {
    await clickComponentToolbar(page, 'tekstikenttä')
    const textfield = page.getByTestId(
      'editor-form__text-field-component-wrapper'
    )
    await textfield
      .getByTestId('tekstikenttä-valinta-kenttään-vain-numeroita')
      .click()
    await textfield
      .locator('.editor-form__decimal-places-selector')
      .selectOption('2')
    await textfield.locator('.editor-form__range-input').nth(0).fill('1')
    await textfield.locator('.editor-form__range-input').nth(1).fill('10')

    await expect(
      textfield.getByTestId('tekstikenttä-valinta-kenttään-vain-numeroita')
    ).toBeChecked()
    await expect(
      textfield.locator('.editor-form__decimal-places-selector')
    ).toHaveValue('2')
    await expect(
      textfield.locator('.editor-form__range-input').nth(0)
    ).toHaveValue('1')
    await expect(
      textfield.locator('.editor-form__range-input').nth(1)
    ).toHaveValue('10')

    await clickRemoveAndConfirm(textfield)
    await expect(textfield).toBeHidden()
  })

  test('pudotusvalikko koodisto päättyneillä koodeilla', async () => {
    await clickComponentToolbar(page, 'dropdown-koodisto')
    const dropdownKoodisto = page.getByTestId(
      'editor-form__dropdown-component-wrapper'
    )

    await dropdownKoodisto
      .getByTestId('editor-form__select-koodisto-dropdown')
      .selectOption('maatjavaltiot2')

    await dropdownKoodisto.getByLabel('Sisällytä päättyneet koodit').click()
    await dropdownKoodisto
      .locator('.editor-form__show-koodisto-values a')
      .click()

    await expect(
      dropdownKoodisto.locator('[title="Entinen Neuvostoliitto"]')
    ).toBeVisible()

    await clickRemoveAndConfirm(dropdownKoodisto)
    await expect(dropdownKoodisto).toBeHidden()
  })

  test('lomakkeen lukitseminen ja lukituksen avaaminen', async () => {
    await page.locator('#lock-form').click()

    await expect(getInputs(page, ':enabled')).toHaveCount(0)
    await expect(getInputs(page, ':disabled')).toHaveCount(
      await getInputs(page).count()
    )

    await expect(getComponentButtons(page, ':enabled')).toHaveCount(0)
    await expect(getComponentButtons(page, ':disabled')).toHaveCount(
      await getComponentButtons(page).count()
    )

    await page.locator('#lock-form').click()

    await expect(getInputs(page, ':disabled')).toHaveCount(1)
    await expect(getInputs(page, ':enabled')).toHaveCount(
      (await getInputs(page).count()) - 1
    )

    await expect(getComponentButtons(page, ':disabled')).toHaveCount(0)
    await expect(getComponentButtons(page, ':enabled')).toHaveCount(
      await getComponentButtons(page).count()
    )
  })

  test('kopioi kysymys toiseen lomakkeeseen', async () => {
    await clickComponentToolbar(page, 'lomakeosio')
    const lomakeosio = page.getByTestId(
      'editor-form__wrapperElement-component-wrapper'
    )
    await lomakeosio
      .locator('.editor-form__text-field')
      .fill('Kopioitava testiosio')
    await lomakeosio.locator('.editor-form__component-button').nth(1).click()
    await page.locator('.close-details-button').click()

    const lomake2 = await lisaaLomake(page)
    lomakkeen2Avain = unsafeFoldOption(lomake2.lomakkeenAvain)

    await page
      .locator('.editor-form__drag_n_drop_spacer_container_for_component')
      .last()
      .hover()
    await page
      .locator('.editor-form__drag_n_drop_spacer_container_for_component')
      .locator('.editor-form__component-button')
      .click()

    await expect(lomakeosio.locator('.editor-form__text-field')).toHaveValue(
      'Kopioitava testiosio'
    )
  })
})
