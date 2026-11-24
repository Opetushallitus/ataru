import { randomUUID } from 'crypto'
import { test, expect, Page } from '@playwright/test'
import { fillField, waitForResponse } from '../playwright-utils'
import {
  asetaTestiHaku,
  asetaTestiHakukohde,
  haeOletuslomakkeenSisalto,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  luoTestiHaunOid,
  poistaLomake,
  poistaTestiHaku,
  poistaTestiHakukohde,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

// Testihakukohde 1 (oid 49028196523, mock_tarjonta_service.clj) on
// olemassa oleva, sisällöltään valmis hakukohde, jonka :hakuOid osoitetaan
// tässä testissä ajonaikaisesti omaan, dedikoituun testihakuun (ks.
// asetaTestiHaku/asetaTestiHakukohde), eikä sen omaan, staattiseen ja
// mahdollisesti muidenkin testien jakamaan hakuun 65950024186.
const hakuOid = luoTestiHaunOid()
const lomakkeenAvain = randomUUID()
const HAKUKOHDE_OID = '1.2.246.562.20.49028196523'

const getHakukohdeOsoite = (hakukohdeOid: string) =>
  `/hakemus/hakukohde/${hakukohdeOid}`

const getFormSections = (page: Page) =>
  page.locator('.application__wrapper-element')

const getFormFields = (page: Page) => page.locator('.application__form-field')

const getSubmitButton = (page: Page) =>
  page.getByTestId('send-application-button')

const TOTEUTUS_URL_PATTERN = /\/konfo\/fi\/toteutus\/1\.2\.246\.562\.17\.\d+$/

const getSelectedHakukohteet = (page: Page) =>
  page.locator('.application__selected-hakukohde-row')

const getSearchHits = (page: Page) =>
  page.locator('.application__search-hit-hakukohde-row')

const getSearchInput = (page: Page) =>
  page.locator('.application__form-text-input-in-box')

const getSelectedHakukohdeText = async (page: Page) =>
  (
    await page
      .locator('.application__selected-hakukohde-row--content')
      .allTextContents()
  ).join('')

let page: Page

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)
  const sisalto = await haeOletuslomakkeenSisalto(page)
  await luoLomakeAvaimella(page, lomakkeenAvain, sisalto)
  await asetaTestiHaku(page, {
    oid: hakuOid,
    ataruLomakeAvain: lomakkeenAvain,
    usePriority: true,
    hakukohdeOids: [
      '1.2.246.562.20.49028196523',
      '1.2.246.562.20.49028196524',
      '1.2.246.562.20.49028196525',
    ],
  })
  await asetaTestiHakukohde(page, { oid: HAKUKOHDE_OID, hakuOid })

  // Varmistetaan suoraan rajapinnasta, että hakukohde löytyy lomakkeineen,
  // ennen kuin luotetaan selaimen renderöintiin.
  const hakukohdeResponse = await page.request.get(
    `/hakemus/api/hakukohde/${HAKUKOHDE_OID}?role=hakija`
  )
  if (!hakukohdeResponse.ok()) {
    throw new Error(
      `Hakukohdetta ${HAKUKOHDE_OID} ei saatu haettua lomakkeen ${lomakkeenAvain} kautta: ${hakukohdeResponse.status()} ${await hakukohdeResponse.text()}`
    )
  }

  await Promise.all([
    waitForResponse(page, 'GET', (url) =>
      url.includes(`/hakemus/api/hakukohde/${HAKUKOHDE_OID}`)
    ),
    page.goto(getHakukohdeOsoite(HAKUKOHDE_OID)),
  ])

  await expect(getFormSections(page)).toHaveCount(2, { timeout: 30000 })
})

test.afterAll(async ({ request }) => {
  await poistaTestiHakukohde(request, HAKUKOHDE_OID)
  await poistaTestiHaku(request, hakuOid)
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('lomake latautuu täydellisenä ja oletushakukohde on valittuna', async () => {
  await expect(getFormFields(page)).toHaveCount(15)
  await expect(getSubmitButton(page)).toBeDisabled()
  await expect(
    page.locator('.application__invalid-field-status-title')
  ).toHaveText('Tarkista 10 tietoa')
  await expect(page.locator('.application__header')).toHaveText('testing2')
  await expect(getSelectedHakukohteet(page)).toHaveCount(1)
  await expect(getSearchHits(page)).toBeHidden()
  await expect(getSearchInput(page)).toBeHidden()
  expect(await getSelectedHakukohdeText(page)).toBe(
    'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne A'
  )
})

test('valitulla hakukohteella on Lue lisää -linkki toteutuksen tietoihin', async () => {
  const linkki = getSelectedHakukohteet(page).getByRole('link', {
    name: /^Lue lisää: Testihakukohde 1/,
  })
  await expect(linkki).toHaveAttribute('href', TOTEUTUS_URL_PATTERN)
  await expect(linkki).toHaveAttribute('target', '_blank')
  await expect(linkki).toHaveAttribute('rel', 'noopener noreferrer')
  await expect(linkki).toHaveAccessibleName(
    'Lue lisää: Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie (avautuu uuteen välilehteen)'
  )
})

test('hakukohteen hakutermien syöttäminen palauttaa oikeat hakutulokset', async () => {
  await page.locator('.application__hakukohde-selection-open-search').click()
  await fillField(page, getSearchInput(page), 'haku')
  await expect(getSearchHits(page)).toHaveCount(3)

  await expect(
    page.locator('.application__search-hit-hakukohde-row--content')
  ).toHaveText([
    'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne A',
    'Testihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne B',
    'Testihakukohde 3 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi C | Tutkintonimike C | Tarkenne C',
  ])
})

test('hakutuloksilla on Lue lisää -linkit toteutusten tietoihin', async () => {
  const linkit = getSearchHits(page).getByRole('link', { name: /^Lue lisää/ })
  await expect(linkit).toHaveCount(3)
  await expect(getSearchHits(page).nth(0)).toHaveAccessibleName(
    'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, Ilmajoentie Koulutuskoodi A | Tutkintonimike A | Tarkenne A'
  )
  const nimet = ['Testihakukohde 1', 'Testihakukohde 2', 'Testihakukohde 3']
  for (let i = 0; i < nimet.length; i++) {
    const nimi = nimet[i]
    await expect(linkit.nth(i)).toHaveAccessibleName(
      new RegExp(`^Lue lisää: ${nimi} – `)
    )
    await expect(linkit.nth(i)).toHaveAttribute('href', TOTEUTUS_URL_PATTERN)
    await expect(linkit.nth(i)).toHaveAttribute('target', '_blank')
    await expect(linkit.nth(i)).toHaveAttribute('rel', 'noopener noreferrer')
  }
})

test('nuolinäppäimet siirtävät fokuksen hakutuloksen Lue lisää -linkistä viereisille riveille', async () => {
  const linkit = getSearchHits(page).getByRole('link', { name: /^Lue lisää/ })
  const lisaaPainike = (n: number) =>
    getSearchHits(page)
      .nth(n)
      .locator('.application__search-hit-hakukohde-row--select-button')

  await linkit.nth(0).focus()
  await page.keyboard.press('ArrowDown')
  await expect(lisaaPainike(1)).toBeFocused()

  await linkit.nth(2).focus()
  await page.keyboard.press('ArrowUp')
  await expect(lisaaPainike(1)).toBeFocused()

  await linkit.nth(1).focus()
  await page.keyboard.press('ArrowUp')
  await expect(getSearchHits(page).nth(0)).toBeFocused()
})

test('hakutulosten tarkentaminen ja hakukohteen lisääminen valittuihin', async () => {
  await fillField(page, getSearchInput(page), 'hakukohde 2')
  await expect(getSearchHits(page)).toHaveCount(1)

  await page
    .locator('.application__search-hit-hakukohde-row--select-button')
    .nth(0)
    .click()
  await expect(getSelectedHakukohteet(page)).toHaveCount(2)

  await expect(
    page.locator('.application__invalid-field-status-title')
  ).toHaveText('Tarkista 10 tietoa')
  expect(await getSelectedHakukohdeText(page)).toBe(
    'Testihakukohde 1 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi A | Tutkintonimike A | Tarkenne ATestihakukohde 2 – Koulutuskeskus Sedu, Ilmajoki, IlmajoentieKoulutuskoodi B | Tutkintonimike B | Tarkenne B'
  )
})
