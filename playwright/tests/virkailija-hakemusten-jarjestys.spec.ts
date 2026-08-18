import { test, expect, Page, Locator } from '@playwright/test'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'
import {
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenAvain: string

// Sukunimet valittu niin, että aakkos- ja lähetysjärjestys eroavat toisistaan
// selvästi: hakemukset lähetetään tässä järjestyksessä (Virtanen ensin,
// Kokko viimeisenä), mutta aakkosjärjestys on Aallikko, Kokko, Virtanen.
const HAKIJAT = [
  {
    'first-name': 'Ville',
    'last-name': 'Virtanen',
    ssn: '020202A0202',
    email: 'virtanen@example.com',
    'verify-email': 'virtanen@example.com',
    phone: '0400000001',
    address: 'Katutie 1',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
  {
    'first-name': 'Aino',
    'last-name': 'Aallikko',
    ssn: '160600A999C',
    email: 'aallikko@example.com',
    'verify-email': 'aallikko@example.com',
    phone: '0400000002',
    address: 'Katutie 2',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
  {
    'first-name': 'Kalle',
    'last-name': 'Kokko',
    ssn: '020202A0202',
    email: 'kokko@example.com',
    'verify-email': 'kokko@example.com',
    phone: '0400000003',
    address: 'Katutie 3',
    'postal-code': '00100',
    'home-town': 'Forssa',
  },
]

test.beforeAll(async ({ browser }) => {
  test.setTimeout(120000)
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  for (const hakija of HAKIJAT) {
    await Promise.all([
      page.goto(getHakijanNakymanOsoite(lomakkeenAvain)),
      waitForResponse(page, 'GET', (url) =>
        url.includes(getLomakkeenHaunOsoite(lomakkeenAvain))
      ),
    ])
    await taytaHenkilotietomoduuli(page, hakija)
    await Promise.all([
      waitForResponse(page, 'POST', (url) =>
        url.includes(getHakemuksenLahettamisenOsoite())
      ),
      page.getByTestId('send-application-button').click(),
    ])
  }

  await page.goto(
    `/lomake-editori/applications/${lomakkeenAvain}?ensisijaisesti=false`
  )
  await clickAndWaitForReload(page.getByTestId('show-results'))
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

const applicantNameCells = () =>
  page.locator('.application-handling__list-row--applicant-name')

// Nimisarakkeen klikattava otsikko: skoopataan tarkasti .application-handling__
// basic-list-basic-column-header -luokkaan, koska sama yläsolu sisältää myös
// suodatinkomponentin.
const applicantNameColumnHeader = () =>
  page.locator(
    '.application-handling__list-row--applicant .application-handling__basic-list-basic-column-header'
  )

// Aikasarakkeen järjestyksenvaihtokuvake: sarakkeen otsikkoteksti ("Muokattu
// viimeksi" / "Lähetetty") vaihtaa VAIN, kumpaa aikaa näytetään, kun taas
// tämä zmdi-kuvake vaihtaa varsinaisen järjestyksen (nouseva/laskeva).
const createdTimeSortIcon = () =>
  page.locator(
    '.application-handling__list-row--created-time i.zmdi, .application-handling__list-row--submitted i.zmdi'
  )

const clickAndWaitForReload = async (locator: Locator) => {
  await Promise.all([
    waitForResponse(page, 'POST', (url) =>
      url.includes('/lomake-editori/api/applications/list')
    ),
    locator.click(),
  ])
}

test('oletusjärjestys on nouseva hakijan nimen mukaan', async () => {
  await expect(applicantNameCells()).toHaveText([
    'Aallikko, Aino',
    'Kokko, Kalle',
    'Virtanen, Ville',
  ])
})

test('nimisarakkeen klikkaus kääntää järjestyksen laskevaksi', async () => {
  await clickAndWaitForReload(applicantNameColumnHeader())

  await expect(applicantNameCells()).toHaveText([
    'Virtanen, Ville',
    'Kokko, Kalle',
    'Aallikko, Aino',
  ])
})

test('nimisarakkeen klikkaus toistamiseen palauttaa nousevan järjestyksen', async () => {
  await clickAndWaitForReload(applicantNameColumnHeader())

  await expect(applicantNameCells()).toHaveText([
    'Aallikko, Aino',
    'Kokko, Kalle',
    'Virtanen, Ville',
  ])
})

test('aikasarakkeen klikkaus järjestää lähetysajan mukaan (nouseva säilyy)', async () => {
  // Vaihdettaessa järjestyssaraketta nykyinen suunta (nouseva) säilyy, joten
  // ensimmäinen aikajärjestykseen vaihto näyttää vanhimman ensin eli
  // lähetysjärjestyksen sellaisenaan.
  await clickAndWaitForReload(createdTimeSortIcon())

  await expect(applicantNameCells()).toHaveText([
    'Virtanen, Ville',
    'Aallikko, Aino',
    'Kokko, Kalle',
  ])
})

test('aikasarakkeen klikkaus toistamiseen kääntää sen laskevaksi', async () => {
  await clickAndWaitForReload(createdTimeSortIcon())

  await expect(applicantNameCells()).toHaveText([
    'Kokko, Kalle',
    'Aallikko, Aino',
    'Virtanen, Ville',
  ])
})
