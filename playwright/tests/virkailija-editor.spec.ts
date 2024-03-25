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

const clickComponentToolbar = async (
  loc: Page | Locator,
  component: string
) => {
  const toolbar = componentToolbar(loc)
  await toolbar.hover()
  await page.getByTestId('component-toolbar-' + component).click()
}

const clickRemoveAndConfirm = async (component: Locator) => {
  await component.locator('.editor-form__component-button').nth(2).click()
  await component.locator('.editor-form__component-button--confirm').click()
}

test.describe('Editori', () => {
  test('näyttää lomakelistan', async () => {
    const items = formListItems(page)
    await expect(items).toHaveCount(formCount + 1)
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
    await expect(textfield).not.toBeVisible()
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
    await expect(textarea).not.toBeVisible()
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
    await expect(dropdown).not.toBeVisible()
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
    await options.last().locator('input').fill('Vaihtoehto 1')
    await multipleChoice.locator('.editor-form__add-dropdown-item a').click()
    await options.last().locator('input').fill('Vaihtoehto 2')
    await multipleChoice.locator('.editor-form__add-dropdown-item a').click()
    await options.last().locator('input').fill('Vaihtoehto 3')
    await options.nth(1).getByTestId('followup-question-followups').click()
    await clickComponentToolbar(multipleChoice, 'painikkeet-yksi-valittavissa')
    const singleChoice = multipleChoice.getByTestId(
      'editor-form__singleChoice-component-wrapper'
    )
    await singleChoice
      .getByTestId('editor-form__singleChoice-label')
      .fill('Oletko punavihervärisokea?')
    await singleChoice.locator('.editor-form__add-dropdown-item a').click()
    await singleChoice.locator('.editor-form__text-field').last().fill('Kyllä')
    await singleChoice.locator('.editor-form__add-dropdown-item a').click()
    await singleChoice.locator('.editor-form__text-field').last().fill('En')
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
      .locator('.editor-form__text-field')
      .nth(0)
      .fill('Jatkokysymys A')
    await adjacentFieldset
      .locator('.editor-form__text-field')
      .nth(1)
      .fill('Jatkokysymys B')
    await adjacentFieldset
      .locator('.editor-form__text-field')
      .nth(2)
      .fill('Jatkokysymys C')

    await clickRemoveAndConfirm(multipleChoice)
    await expect(multipleChoice).not.toBeVisible()
  })
  //   describe('multiple choice', () => {
  //     before(
  //       clickComponentMenuItem('Lista, monta valittavissa'),
  //       setTextFieldValue(
  //         () => formComponents().eq(5).find('.editor-form__text-field').eq(0),
  //         'Viides kysymys'
  //       ),
  //       clickElement(() =>
  //         formComponents().eq(5).find('.editor-form__add-dropdown-item a')
  //       ),
  //       setTextFieldValue(
  //         () => formComponents().eq(5).find('.editor-form__text-field:last'),
  //         'Ensimmäinen vaihtoehto'
  //       ),
  //       clickElement(() =>
  //         formComponents().eq(5).find('.editor-form__add-dropdown-item a')
  //       ),
  //       setTextFieldValue(
  //         () => formComponents().eq(5).find('.editor-form__text-field:last'),
  //         'Toinen vaihtoehto'
  //       ),
  //       clickElement(() =>
  //         formComponents().eq(5).find('.editor-form__add-dropdown-item a')
  //       ),
  //       setTextFieldValue(
  //         () => formComponents().eq(5).find('.editor-form__text-field:last'),
  //         'Kolmas vaihtoehto'
  //       ),
  //       clickElement(() =>
  //         formComponents().eq(5).find('.editor-form__add-dropdown-item a')
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question:eq(1) a:contains("Lisäkysymykset")'
  //           )
  //       ),
  //       clickSubComponentMenuItem('Painikkeet, yksi valittavissa', () =>
  //         formComponents().eq(5)
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(5)
  //             .find(
  //               '.editor-form__followup-question-overlay input.editor-form__text-field'
  //             ),
  //         'Oletko punavihervärisokea?'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(5)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(0)'
  //             ),
  //         'Kyllä'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(5)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(1)'
  //             ),
  //         'En'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__checkbox + .editor-form__checkbox-label:contains("Pakollinen")'
  //           )
  //       ),
  //       clickSubComponentMenuItem('Vierekkäiset tekstikentät', () =>
  //         formComponents().eq(5)
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(5)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__text-field'
  //             )
  //             .eq(3),
  //         'Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")'
  //           )
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(5)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(0),
  //         'Jatkokysymys A'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
  //           )
  //           .eq(0)
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(5)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(1),
  //         'Jatkokysymys B'
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(5)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(2),
  //         'Jatkokysymys C'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
  //           )
  //           .eq(2)
  //       )
  //     )
  //     it('has expected contents', () => {
  //       expect(formComponents()).to.have.length(6)
  //       expect(
  //         formComponents().eq(5).find('.editor-form__text-field:first').val()
  //       ).to.equal('Viides kysymys')
  //       expect(
  //         formComponents()
  //           .eq(5)
  //           .find('.editor-form__checkbox-container input')
  //           .prop('checked')
  //       ).to.equal(false)
  //       expect(
  //         formComponents()
  //           .eq(5)
  //           .find('.editor-form__multi-option-wrapper input')
  //           .not('.editor-form__followup-question-overlay input').length
  //       ).to.equal(4)
  //       const options = _.map(
  //         formComponents()
  //           .eq(5)
  //           .find('.editor-form__multi-option-wrapper input')
  //           .not('.editor-form__followup-question-overlay input'),
  //         (inputField) => $(inputField).val()
  //       )
  //       expect(options).to.eql([
  //         'Ensimmäinen vaihtoehto',
  //         'Toinen vaihtoehto',
  //         'Kolmas vaihtoehto',
  //         '',
  //       ])
  //       expect(
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__text-field'
  //           )
  //           .eq(3)
  //           .val()
  //       ).to.equal('Vierekkäinen tekstikenttä monivalinnan jatkokysymyksenä')
  //       expect(
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(0)
  //           .val()
  //       ).to.equal('Jatkokysymys A')
  //       expect(
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(1)
  //           .val()
  //       ).to.equal('Jatkokysymys B')
  //       expect(
  //         formComponents()
  //           .eq(5)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(2)
  //           .val()
  //       ).to.equal('Jatkokysymys C')
  //     })
  //   })
  //
  //   describe('multiple choice from koodisto', () => {
  //     before(
  //       clickComponentMenuItem('Lista, monta valittavissa, koodisto'),
  //       setTextFieldValue(
  //         () => formComponents().eq(6).find('.editor-form__text-field'),
  //         'Kuudes kysymys'
  //       ),
  //       () => {
  //         const e = formComponents()
  //           .eq(6)
  //           .find('.editor-form__select-koodisto-dropdown')
  //         e.val('tutkinto')
  //         triggerEvent(e, 'change')
  //         return
  //       }
  //     )
  //     it('selected correctly', () => {
  //       expect(formComponents()).to.have.length(7)
  //       expect(
  //         formComponents()
  //           .eq(6)
  //           .find('.editor-form__select-koodisto-dropdown')
  //           .val()
  //       ).to.equal('tutkinto')
  //     })
  //   })
  //
  //   describe('section with contents', () => {
  //     before(
  //       clickComponentMenuItem('Lomakeosio'),
  //       setTextFieldValue(
  //         () => formSections().eq(0).find('.editor-form__text-field').eq(0),
  //         'Testiosio'
  //       ),
  //       clickSubComponentMenuItem('Tekstialue', () => formSections().eq(0)),
  //       clickElement(() =>
  //         formSections()
  //           .eq(0)
  //           .find(
  //             '.editor-form__checkbox-wrapper label:contains("Pakollinen")'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () => formSections().eq(0).find('.editor-form__text-field').eq(1),
  //         'Osiokysymys'
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(0)
  //           .find('.editor-form__button-group div:eq(0) label')
  //       )
  //     )
  //     it('has expected contents', () => {
  //       expect(formComponents()).to.have.length(9)
  //       expect(
  //         formSections().eq(0).find('.editor-form__text-field').eq(0).val()
  //       ).to.equal('Testiosio')
  //       expect(
  //         formSections().eq(0).find('.editor-form__text-field').eq(1).val()
  //       ).to.equal('Osiokysymys')
  //       expect(
  //         formSections()
  //           .eq(0)
  //           .find('.editor-form__button-group input:checked')
  //           .val()
  //       ).to.equal('S')
  //       expect(
  //         formSections()
  //           .eq(0)
  //           .find('.editor-form__checkbox-container input')
  //           .prop('checked')
  //       ).to.equal(true)
  //     })
  //   })
  //
  //   describe('textfield with info text', () => {
  //     before(
  //       clickComponentMenuItem('Tekstikenttä'),
  //       clickInfoTextCheckbox(() => formComponents().eq(9)),
  //       setTextFieldValue(
  //         () => formComponents().eq(9).find('.editor-form__text-field'),
  //         'Infoteksti'
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(9)
  //             .find('.editor-form__info-addon-inputs textarea')
  //             .eq(0),
  //         'oikeen pitka infoteksti sitten tassa.'
  //       )
  //     )
  //
  //     it('has expected contents', () => {
  //       expect(formComponents()).to.have.length(10)
  //       expect(
  //         formComponents()
  //           .eq(9)
  //           .find('.editor-form__info-addon-checkbox input')
  //           .prop('checked')
  //       ).to.equal(true)
  //       expect(
  //         formComponents()
  //           .eq(9)
  //           .find('.editor-form__info-addon-inputs textarea')
  //           .eq(0)
  //           .val()
  //       ).to.equal('oikeen pitka infoteksti sitten tassa.')
  //     })
  //   })
  //
  //   /*
  //    * This field is not supposed to be filled in the application tests, they should ignore it and submitting should
  //    * work because this is optional. This was added because of regression: optional dropdown failed the server-side
  //    * validation.
  //    */
  //   describe('second dropdown from koodisto (optional)', () => {
  //     before(
  //       clickComponentMenuItem('Pudotusvalikko, koodisto'),
  //       setTextFieldValue(
  //         () => formComponents().eq(10).find('.editor-form__text-field'),
  //         'Viimeinen kysymys'
  //       ),
  //       () => {
  //         const e = formComponents()
  //           .eq(10)
  //           .find('.editor-form__select-koodisto-dropdown')
  //         e.val('tutkinto')
  //         triggerEvent(e, 'change')
  //         return
  //       }
  //     )
  //     it('selected correctly', () => {
  //       expect(formComponents()).to.have.length(11)
  //       expect(
  //         formComponents()
  //           .eq(10)
  //           .find('.editor-form__select-koodisto-dropdown')
  //           .val()
  //       ).to.equal('tutkinto')
  //     })
  //   })
  //
  //   describe('semantic radio button', () => {
  //     before(
  //       clickComponentMenuItem('Lomakeosio'),
  //       setTextFieldValue(
  //         () => formSections().eq(1).find('.editor-form__text-field:first'),
  //         'Testiosio 2'
  //       ),
  //       clickSubComponentMenuItem('Painikkeet, yksi valittavissa', () =>
  //         formComponents().eq(11)
  //       ),
  //       setTextFieldValue(
  //         () => formSections().eq(1).find('.editor-form__text-field:last'),
  //         'Lyhyen listan kysymys'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(11)
  //           .find('.editor-form__checkbox-wrapper label:first')
  //       ),
  //       clickElement(() =>
  //         formComponents().eq(11).find('.editor-form__add-dropdown-item a')
  //       ),
  //       setTextFieldValue(
  //         () => formSections().eq(1).find('.editor-form__text-field:last'),
  //         'Ensimmäinen vaihtoehto'
  //       ),
  //       clickElement(() =>
  //         formSections().eq(1).find('.editor-form__add-dropdown-item a')
  //       ),
  //       setTextFieldValue(
  //         () => formSections().eq(1).find('.editor-form__text-field:last'),
  //         'Toinen vaihtoehto'
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(1)
  //           .find(
  //             '.editor-form__followup-question:eq(0) a:contains("Lisäkysymykset")'
  //           )
  //       ),
  //       clickSubComponentMenuItem('Lista, monta valittavissa', () =>
  //         formSections().eq(1).find('.editor-form__followup-question-overlay')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formSections()
  //             .eq(1)
  //             .find(
  //               '.editor-form__followup-question-overlay input.editor-form__text-field'
  //             ),
  //         'Monivalinta jatkokysymyksenä'
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(1)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__checkbox + .editor-form__checkbox-label:contains("Pakollinen")'
  //           )
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(1)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formSections()
  //             .eq(1)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(0)'
  //             ),
  //         'Jatkokysymys A'
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(1)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__add-dropdown-item a:contains("Lisää")'
  //           )
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formSections()
  //             .eq(1)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__multi-option-wrapper .editor-form__text-field:eq(1)'
  //             ),
  //         'Jatkokysymys B'
  //       ),
  //       clickSubComponentMenuItem('Vierekkäiset tekstikentät', () =>
  //         formSections().eq(1).find('.editor-form__followup-question-overlay')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formSections()
  //             .eq(1)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__text-field'
  //             )
  //             .eq(3),
  //         'Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä'
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(1)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")'
  //           )
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formSections()
  //           .eq(1)
  //           .find('.editor-form__adjacent-fieldset-container')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formSections()
  //             .eq(1)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(0),
  //         'Jatkokysymys A'
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(1)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
  //           )
  //           .eq(0)
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formSections()
  //           .eq(1)
  //           .find('.editor-form__adjacent-fieldset-container')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formSections()
  //             .eq(1)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(1),
  //         'Jatkokysymys B'
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formSections()
  //           .eq(1)
  //           .find('.editor-form__adjacent-fieldset-container')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formSections()
  //             .eq(1)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(2),
  //         'Jatkokysymys C'
  //       ),
  //       clickElement(() =>
  //         formSections()
  //           .eq(1)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
  //           )
  //           .eq(2)
  //       )
  //     )
  //     it('has expected contents', () => {
  //       expect(formComponents()).to.have.length(13)
  //       expect(
  //         formComponents().eq(11).find('.editor-form__text-field:eq(1)').val()
  //       ).to.equal('Lyhyen listan kysymys')
  //       expect(
  //         formComponents()
  //           .eq(11)
  //           .find('.editor-form__checkbox-container input')
  //           .prop('checked')
  //       ).to.equal(true)
  //       expect(
  //         formComponents()
  //           .eq(11)
  //           .find(
  //             '.editor-form__multi-options-container > div:nth-child(1) .editor-form__text-field'
  //           )
  //           .not('.editor-form__followup-question-overlay input')
  //           .val()
  //       ).to.equal('Ensimmäinen vaihtoehto')
  //       expect(
  //         formComponents()
  //           .eq(11)
  //           .find(
  //             '.editor-form__multi-options-container > div:nth-child(2) .editor-form__text-field'
  //           )
  //           .not('.editor-form__followup-question-overlay input')
  //           .val()
  //       ).to.equal('Toinen vaihtoehto')
  //       expect(
  //         formComponents()
  //           .eq(11)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__text-field'
  //           )
  //           .eq(3)
  //           .val()
  //       ).to.equal('Vierekkäinen tekstikenttä painikkeiden jatkokysymyksenä')
  //       expect(
  //         formComponents()
  //           .eq(11)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(0)
  //           .val()
  //       ).to.equal('Jatkokysymys A')
  //       expect(
  //         formComponents()
  //           .eq(11)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(1)
  //           .val()
  //       ).to.equal('Jatkokysymys B')
  //       expect(
  //         formComponents()
  //           .eq(11)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(2)
  //           .val()
  //       ).to.equal('Jatkokysymys C')
  //     })
  //   })
  //
  //   describe('adjacent fields', () => {
  //     before(
  //       clickComponentMenuItem('Vierekkäiset tekstikentät'),
  //       setTextFieldValue(
  //         () => formComponents().eq(13).find('.editor-form__text-field'),
  //         'Vierekkäinen tekstikenttä'
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents().eq(13)
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(13)
  //             .find(
  //               '.editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             ),
  //         'Tekstikenttä 1'
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents().eq(13)
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(13)
  //             .find(
  //               '.editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(1),
  //         'Tekstikenttä 2'
  //       )
  //     )
  //     it('🌸  is working so wonderfully 🌸', () => {})
  //   })
  //
  //   describe('dropdown with adjacent fields as followup', () => {
  //     before(
  //       clickComponentMenuItem('Pudotusvalikko'),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(16)
  //             .find(
  //               '.editor-form__component-content-wrapper ' +
  //               '.editor-form__text-field:first'
  //             ),
  //         'Päätason pudotusvalikko'
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(16)
  //             .find(
  //               '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
  //             )
  //             .eq(0),
  //         'Pudotusvalikon 1. kysymys'
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(16)
  //             .find(
  //               '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
  //             )
  //             .eq(1),
  //         'Pudotusvalikon 2. kysymys'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__multi-options-container a:contains("Lisäkysymykset")'
  //           )
  //       ),
  //       clickSubComponentMenuItem('Vierekkäiset tekstikentät', () =>
  //         formComponents().eq(16)
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(16)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__text-field'
  //             ),
  //         'Vierekkäinen tekstikenttä jatkokysymyksenä'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__checkbox + label.editor-form__checkbox-label:contains("Vastaaja voi lisätä useita vastauksia")'
  //           )
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents()
  //           .eq(16)
  //           .find('.editor-form__adjacent-fieldset-container')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(16)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(0),
  //         'Jatkokysymys A'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
  //           )
  //           .eq(0)
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents()
  //           .eq(16)
  //           .find('.editor-form__adjacent-fieldset-container')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(16)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(1),
  //         'Jatkokysymys B'
  //       ),
  //       clickSubComponentMenuItem('Tekstikenttä', () =>
  //         formComponents()
  //           .eq(16)
  //           .find('.editor-form__adjacent-fieldset-container')
  //       ),
  //       setTextFieldValue(
  //         () =>
  //           formComponents()
  //             .eq(16)
  //             .find(
  //               '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //             )
  //             .eq(2),
  //         'Jatkokysymys C'
  //       ),
  //       clickElement(() =>
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__checkbox + label:contains("Pakollinen tieto")'
  //           )
  //           .eq(2)
  //       )
  //     )
  //     it('has expected contents', () => {
  //       expect(formComponents()).to.have.length(17)
  //       expect(
  //         formComponents().eq(16).find('.editor-form__text-field:first').val()
  //       ).to.equal('Päätason pudotusvalikko')
  //       expect(
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
  //           )
  //           .eq(0)
  //           .val()
  //       ).to.equal('Pudotusvalikon 1. kysymys')
  //       expect(
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__multi-options-wrapper-outer .editor-form__text-field'
  //           )
  //           .eq(1)
  //           .val()
  //       ).to.equal('Pudotusvalikon 2. kysymys')
  //       expect(
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__text-field'
  //           )
  //           .eq(0)
  //           .val()
  //       ).to.equal('Vierekkäinen tekstikenttä jatkokysymyksenä')
  //       expect(
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(0)
  //           .val()
  //       ).to.equal('Jatkokysymys A')
  //       expect(
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(1)
  //           .val()
  //       ).to.equal('Jatkokysymys B')
  //       expect(
  //         formComponents()
  //           .eq(16)
  //           .find(
  //             '.editor-form__followup-question-overlay .editor-form__adjacent-fieldset-container .editor-form__text-field'
  //           )
  //           .eq(2)
  //           .val()
  //       ).to.equal('Jatkokysymys C')
  //     })
  //   })
  //
  //   describe('numeric textfield', () => {
  //     before(
  //       clickComponentMenuItem('Tekstikenttä'),
  //       setTextFieldValue(
  //         () => formComponents().eq(17).find('.editor-form__text-field'),
  //         'Tekstikenttä numeerisilla arvoilla'
  //       ),
  //       clickNumericAnswer('Tekstikenttä numeerisilla arvoilla'),
  //       () => {
  //         formComponents().eq(17).find('option').eq(4).prop('selected', true)
  //         triggerEvent(formComponents().eq(17).find('select'), 'change')
  //       }
  //     )
  //     it('has expected contents', () => {
  //       expect(formComponents()).to.have.length(18)
  //       expect(
  //         formComponents().eq(17).find('.editor-form__text-field').val()
  //       ).to.equal('Tekstikenttä numeerisilla arvoilla')
  //       expect(
  //         formComponents()
  //           .eq(17)
  //           .find('.editor-form__checkbox-container input')
  //           .eq(2)
  //           .prop('checked')
  //       ).to.equal(true)
  //       expect(
  //         formComponents().eq(17).find('select')[0].selectedIndex
  //       ).to.equal(4)
  //     })
  //   })
  //
  //   describe('dropdown from koodisto, with invalid options', () => {
  //     before(
  //       clickComponentMenuItem('Pudotusvalikko, koodisto'),
  //       setTextFieldValue(
  //         () => formComponents().eq(18).find('.editor-form__text-field'),
  //         'Alasvetovalikko, koodisto, päättyneet'
  //       ),
  //       () => {
  //         const e = formComponents()
  //           .eq(18)
  //           .find('.editor-form__select-koodisto-dropdown')
  //         e.val('maatjavaltiot2')
  //         triggerEvent(e, 'change')
  //         return
  //       },
  //       clickElement(() =>
  //         formComponents()
  //           .eq(18)
  //           .find(
  //             '.editor-form__checkbox + label:contains("Sisällytä päättyneet koodit")'
  //           )
  //       ),
  //       clickElement(() =>
  //         formComponents().eq(18).find('.editor-form__show-koodisto-values a')
  //       ),
  //       wait.until(() =>
  //         elementExists(
  //           formComponents()
  //             .eq(18)
  //             .find('.editor-form__koodisto-field:contains("Suomi")')
  //         )
  //       )
  //     )
  //     it('selected correctly', () => {
  //       expect(formComponents()).to.have.length(19)
  //       expect(
  //         formComponents()
  //           .eq(18)
  //           .find('.editor-form__select-koodisto-dropdown')
  //           .val()
  //       ).to.equal('maatjavaltiot2')
  //       expect(
  //         formComponents()
  //           .eq(18)
  //           .find(
  //             '.editor-form__checkbox + label:contains("Sisällytä päättyneet koodit")'
  //           )
  //           .siblings()
  //           .prop('checked')
  //       ).to.equal(true)
  //       expect(
  //         elementExists(
  //           formComponents()
  //             .eq(18)
  //             .find(
  //               '.editor-form__koodisto-field:contains("Entinen Neuvostoliitto")'
  //             )
  //         )
  //       ).to.equal(true)
  //     })
  //   })
  //
  //   describe('locking form', () => {
  //     before(
  //       clickLomakeForEdit('Testilomake'),
  //       wait.forMilliseconds(1000), // wait abit since
  //       clickLockForm(), // this locking is sometimes so fast that the previous request gets blocked.
  //       wait.until(() =>
  //         elementExists(testFrame().find('.editor-form__form-editing-locked'))
  //       ),
  //       wait.until(() => {
  //         return getInputs(':enabled').length === 0
  //       })
  //     )
  //     it('all inputs are locked', () => {
  //       expect(getInputs(':disabled').length).to.equal(getInputs('').length)
  //       expect(getInputs(':enabled').length).to.equal(0)
  //       expect(getRemoveElementButtons(':disabled').length).to.equal(
  //         getRemoveElementButtons('').length
  //       )
  //       expect(getRemoveElementButtons(':enabled').length).to.equal(0)
  //       expect(
  //         testFrame().find(
  //           '.editor-form__add-component-toolbar .plus-component--disabled'
  //         ).length
  //       ).to.equal(1)
  //       expect(
  //         elementExists(testFrame().find('.editor-form__form-editing-locked'))
  //       ).to.equal(true)
  //     })
  //   })
  //
  //   describe('releasing form lock', () => {
  //     before(
  //       clickLockForm(),
  //       wait.until(
  //         () =>
  //           !elementExists(
  //             testFrame().find('.editor-form__form-editing-locked')
  //           )
  //       )
  //     )
  //     it('all inputs are unlocked', () => {
  //       expect(getInputs(':disabled').length).to.equal(0)
  //       expect(getInputs(':enabled').length).to.equal(getInputs('').length)
  //       expect(
  //         testFrame().find(
  //           '.editor-form__add-component-toolbar .plus-component--disabled'
  //         ).length
  //       ).to.equal(0)
  //       expect(
  //         elementExists(testFrame().find('.editor-form__form-editing-locked'))
  //       ).to.equal(false)
  //     })
  //   })
  //
  //   describe('autosave', () => {
  //     before(
  //       wait.until(() => {
  //         const flasher = testFrame().find('.top-banner .flasher')
  //         return (
  //           flasher.css('opacity') !== '0' &&
  //           flasher.find('span:visible').text() ===
  //           'Kaikki muutokset tallennettu'
  //         )
  //       })
  //     )
  //     it('notification shows success', () => {
  //       expect(testFrame().find('.top-banner .flasher span').text()).to.equal(
  //         'Kaikki muutokset tallennettu'
  //       )
  //     })
  //   })
  // })
  //
  // describe('Copy from a form and paste into another after closing the first form', () => {
  //   before(
  //     clickLomakeForEdit('Testilomake'),
  //     wait.forMilliseconds(1000),
  //     clickCopyFormComponent('Testiosio'),
  //     wait.forMilliseconds(1000),
  //     clickCloseDetailsButton(),
  //     wait.forMilliseconds(500),
  //     clickLomakeForEdit('Selaintestilomake4'),
  //     wait.forMilliseconds(1000),
  //     clickPasteFormComponent(0),
  //     wait.forMilliseconds(500)
  //   )
  //   it('creates the copy in another form', () => {
  //     expect(
  //       formSections().eq(0).find('.editor-form__text-field').eq(0).val()
  //     ).to.equal('Testiosio 2')
  //   })
  // })
  //
  // describe('hakukohde specific question', () => {
  //   const component = () => formComponents().eq(0)
  //   before(
  //     clickLomakeForEdit('belongs-to-hakukohteet-test-form'),
  //     clickElement(() =>
  //       component().find('.editor-form__component-fold-button')
  //     ),
  //     wait.until(() =>
  //       elementExists(
  //         component().find('.belongs-to-hakukohteet__modal-toggle')
  //       )
  //     ),
  //     clickElement(() =>
  //       component().find('.belongs-to-hakukohteet__modal-toggle')
  //     ),
  //     clickElement(() =>
  //       component().find(
  //         '.hakukohde-and-hakukohderyhma-category-list-item:first'
  //       )
  //     )
  //   )
  //   it('shows the selected hakukohde', () => {
  //     expect(
  //       component().find('.belongs-to-hakukohteet__hakukohde-label').length
  //     ).to.equal(1)
  //   })
  // })
})
