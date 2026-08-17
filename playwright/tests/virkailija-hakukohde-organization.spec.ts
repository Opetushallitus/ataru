import { test, expect, Page } from '@playwright/test'
import {
  haeOletuslomakkeenSisalto,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  poistaLomake,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

// USER-WITH-HAKUKOHDE-ORGANIZATION-käyttäjä saa lomake-editorin oikeudet vain
// hakukohteen 1.2.246.562.20.49028100004 (mock_tarjonta_service.clj) kautta,
// jonka tarjoajaOids kattaa käyttäjän organisaation. Kyseisen mock-hakukohteen
// haku (1.2.246.562.29.65950024188) osoittaa kiinteästi tämän avaimiseen
// lomakkeeseen, joten lomake pitää luoda juuri tällä avaimella eikä satunnaisella.
const LOMAKKEEN_AVAIN = 'hakukohteen-organisaatiosta-form'

const formListItems = (page: Page) =>
  page.locator('.editor-form__list').locator('a')

let page: Page

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)
  const sisalto = await haeOletuslomakkeenSisalto(page)
  await luoLomakeAvaimella(
    page,
    LOMAKKEEN_AVAIN,
    sisalto,
    'hakukohteen-organisaatiosta'
  )

  await kirjauduVirkailijanNakymaan(page, 'USER-WITH-HAKUKOHDE-ORGANIZATION')
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, LOMAKKEEN_AVAIN)
  await page.close()
})

test('näyttää vain hakukohteen organisaation kautta assosioituneen lomakkeen', async () => {
  await expect(formListItems(page)).toHaveCount(1)
  await expect(
    formListItems(page).locator('.editor-form__list-form-name')
  ).toHaveText('hakukohteen-organisaatiosta')
})

test('hakukohteen organisaation kautta assosioitunut käyttäjä voi muokata lomaketta', async () => {
  await formListItems(page).first().click()
  await expect(page.getByTestId('form-name-input')).toHaveValue(
    'hakukohteen-organisaatiosta'
  )

  const toolbar = page.getByTestId('component-toolbar')
  await toolbar.hover()
  await toolbar.getByTestId('component-toolbar-tekstikenttä').click()

  const textfield = page.getByTestId(
    'editor-form__text-field-component-wrapper'
  )
  await textfield
    .getByTestId('tekstikenttä-kysymys')
    .fill('Ensimmäinen kysymys')
  await textfield.locator('.editor-form__info-addon-checkbox label').click()
  await textfield
    .locator('.editor-form__info-addon-inputs textarea')
    .fill('Ensimmäisen kysymyksen ohjeteksti')

  // Oletuslomake sisältää jo valmiiksi "Yleiset asetukset", "Hakukohteet" ja
  // "Henkilötiedot" -komponentit (ks. haeOletuslomakkeenSisalto), joten
  // lisätty tekstikenttä on niiden lisäksi neljäs.
  await expect(page.locator('.editor-form__component-wrapper')).toHaveCount(4)
  await expect(textfield.getByTestId('tekstikenttä-kysymys')).toHaveValue(
    'Ensimmäinen kysymys'
  )
  await expect(
    textfield.getByLabel('Kysymys sisältää ohjetekstin')
  ).toBeChecked()
  await expect(
    textfield.locator('.editor-form__info-addon-inputs textarea')
  ).toHaveValue('Ensimmäisen kysymyksen ohjeteksti')
})
