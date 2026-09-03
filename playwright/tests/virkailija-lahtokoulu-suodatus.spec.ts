import { randomUUID } from 'crypto'
import { test, expect, Page, Route } from '@playwright/test'
import { fixtureFromFile } from '../playwright-utils'
import {
  asetaTestiHaku,
  asetaTestiHakukohde,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  haeOletuslomakkeenSisalto,
  luoTestiHaunOid,
  poistaLomake,
  poistaTestiHaku,
  poistaTestiHakukohde,
} from '../playwright-ataru-utils'

// Toisen asteen yhteishaun lähtökoulu-suodatinta ei voi ajaa oikeaa
// organisaatiopalvelua vasten: kehitysympäristön FakeOrganizationService
// (ataru.organization-service.organization-service) ei tunne
// oppilaitostyyppi-/organisaatiotyypit-kenttiä, joten mikään sen palauttama
// organisaatio ei koskaan läpäise lähtökoulukelpoisuustarkistusta
// (organization-selection/filter-organizations). Siksi juuri tämä yksi
// päätepiste mockataan page.route():lla samaan tapaan kuin
// virkailijan-massatoiminnot.spec.ts jo tekee muille päätepisteille — muu
// haku (toisen asteen yhteishaku, hakukohde) on oikeaa, ajonaikaisesti
// rekisteröityä testidataa.
test.describe.configure({ mode: 'serial' })

let page: Page
let kayttajallaVainYksiOrganisaatio = false

const hakuOid = luoTestiHaunOid()
const hakukohdeOid = '1.2.246.562.20.90000000201'
const lomakkeenAvain = randomUUID()

const userOrganizationsRoute = (route: Route) =>
  fixtureFromFile(
    kayttajallaVainYksiOrganisaatio ? 'lahtokoulu.json' : 'lahtokoulut.json'
  )(route)

const naytaHaunHakemukset = async () => {
  const haunOsoite = `/lomake-editori/applications/haku/${hakuOid}?ensisijaisesti=false`
  await page.goto(haunOsoite)
  await page.getByTestId('show-results').click()
}

const openApplicationFilters = () => page.locator('#open-application-filters')
const schoolSearch = () => page.locator('#school-search')
const schoolOptions = () => page.locator('div.school-filter__option')
const selectedSchool = () => page.locator('#selected-school')
const removeSelectedSchoolButton = () =>
  page.locator('#remove-selected-school-button')
const classesDropdown = () => page.locator('.multi-option-dropdown__dropdown')
const classesOptions = () => page.locator('li.multi-option-dropdown__option')
const valpasLink = () => page.locator('#valpas-hakutilanne-link')

const searchSchool = async (term: string) => {
  await schoolSearch().fill(term)
}

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await page.route(
    '**/lomake-editori/api/organization/user-organizations*',
    userOrganizationsRoute
  )
  await page.route(
    '**/lomake-editori/api/applications/oppilaitos/*/luokat',
    fixtureFromFile('lahtokoulunLuokat.json')
  )

  await kirjauduVirkailijanNakymaan(page, 'OPINTO-OHJAAJA')

  const sisalto = await haeOletuslomakkeenSisalto(page)
  await luoLomakeAvaimella(page, lomakkeenAvain, sisalto)
  await asetaTestiHaku(page, {
    oid: hakuOid,
    ataruLomakeAvain: lomakkeenAvain,
    hakukohdeOids: [hakukohdeOid],
    kohdejoukkoUri: 'haunkohdejoukko_11#1',
    hakutapaUri: 'hakutapa_01#1',
  })
  await asetaTestiHakukohde(page, {
    oid: hakukohdeOid,
    hakuOid,
    hakukohteenNimet: { kieli_fi: 'Lähtökoulusuodatuksen testihakukohde' },
  })

  await naytaHaunHakemukset()
  await openApplicationFilters().click()
})

test.afterAll(async ({ request }) => {
  await poistaTestiHakukohde(request, hakukohdeOid)
  await poistaTestiHaku(request, hakuOid)
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('Valpas-linkissä ei ole lähtökoulua', async () => {
  await expect(valpasLink()).toHaveAttribute('href', /\/hakutilanne\/$/)
})

test('Lähtökouluksi voi hakea käyttäjän organisaatioita', async () => {
  await searchSchool('perus')
  await expect(schoolOptions()).toHaveCount(3)

  await searchSchool('Haa')
  await expect(schoolOptions()).toHaveCount(1)

  await searchSchool('haaga')
  await expect(schoolOptions()).toHaveCount(1)

  await searchSchool('1.2.3.4.7')
  await expect(schoolOptions()).toHaveCount(1)

  await searchSchool('lukio')
  await expect(schoolOptions()).toHaveCount(0)

  await searchSchool('Pell')
  await expect(schoolOptions()).toHaveCount(1)
})

test('Lähtökouluksi voi asettaa käyttäjän organisaation', async () => {
  await schoolOptions().first().click()
  await expect(selectedSchool()).toBeVisible()
  await expect(selectedSchool()).toContainText('Pellon peruskoulu')
  await expect(schoolSearch()).toHaveCount(0)
})

test('Valpas-linkissä on lähtökoulu', async () => {
  await expect(valpasLink()).toHaveAttribute(
    'href',
    /\/hakutilanne\/1\.2\.3\.4\.6$/
  )
})

test('Hakijoiden luokat voi valita lähtökoulusta', async () => {
  await classesDropdown().click()
  await expect(classesOptions()).toHaveCount(4)
})

test('Lähtökoulu valinnan voi poistaa jolloin myös valittavat luokat poistuu', async () => {
  await removeSelectedSchoolButton().click()
  await expect(selectedSchool()).toHaveCount(0)
  await expect(schoolSearch()).toBeVisible()

  await classesDropdown().click()
  await expect(classesOptions()).toHaveCount(0)
})

test.describe('Käyttäjällä on oikeus vain yhteen organisaatioon', () => {
  test.beforeAll(async () => {
    kayttajallaVainYksiOrganisaatio = true
    // Uusi sivulataus varmistaa puhtaan, edellisistä testeistä
    // riippumattoman sovellustilan sen sijaan, että luotettaisiin siihen,
    // että aiemmin avattu suodatinpaneeli reagoi taannehtivasti muuttuneeseen
    // organisaatiodataan.
    await naytaHaunHakemukset()
    await openApplicationFilters().click()
  })

  test('Hakemusten rajauksessa on valittu lähtökouluksi käyttäjän ainoa organisaatio', async () => {
    await expect(schoolSearch()).toHaveCount(0)
    await expect(selectedSchool()).toBeVisible()
    await expect(selectedSchool()).toContainText('Haagan peruskoulu')
  })
})
