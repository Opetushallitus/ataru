import { test, expect, Page } from '@playwright/test'
import {
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'
import { unsafeFoldOption } from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenId: number
let lomakkeenAvain: string

const HAKU_OID = '1.2.246.562.29.00000000000000009901'
const HAKUKOHDE_OID = '1.2.246.562.20.00000000000000018390'
const HAKUKOHDE_NIMI = 'Testihakukohde näkyvyystestiin'

// Hakukohdekohtaisen näkyvyyden valintaikkuna näyttää hakukohdelistan vain,
// jos lomake on jonkin haun käytössä (ks. ataru.virkailija.editor.subs/
// used-by-haku? ja ataru.virkailija.editor.components.belongs-to-
// hakukohteet-component) — muuten ikkunassa näkyy vain kehotus asettaa
// lomakkeelle haku. Mockataan siis samat kolme rajapintakutsua, jotka
// :editor/refresh-form-used-in-hakus ja :editor/refresh-used-by-haut
// laukaisevat (ks. ataru.virkailija.editor.handlers/refresh-form-used-in-
// hakus ja ataru.virkailija.tarjonta/fetch-haut-with-hakukohteet), samaan
// tapaan kuin lomakkeen-kopiointi.spec.ts tekee "näkyy kaikille"
// -toiminnolle — mutta annetaan hakukohteelle myös oikea :hakukohteet-
// lista, jotta valintaikkunaan tulee oikea, klikattava hakukohderivi.
test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  // Käytetään funktiopohjaista reittiä (ei glob-merkkijonoa), jotta
  // haku-oidillinen ja form-key:llinen /tarjonta/haku-osoite eivät voi
  // mennä sekaisin keskenään "?"-merkin glob-tulkinnan takia.
  await page.route(
    (url) =>
      url.pathname === '/lomake-editori/api/tarjonta/haku' &&
      url.searchParams.has('form-key'),
    (route) => route.fulfill({ json: [{ oid: HAKU_OID, yhteishaku: true }] })
  )
  await page.route(
    (url) => url.pathname === `/lomake-editori/api/tarjonta/haku/${HAKU_OID}`,
    (route) =>
      route.fulfill({
        json: { oid: HAKU_OID, yhteishaku: true, name: { fi: 'Testihaku' } },
      })
  )
  await page.route(
    (url) => url.pathname === '/lomake-editori/api/tarjonta/hakukohde',
    (route) =>
      route.fulfill({
        json: [
          {
            oid: HAKUKOHDE_OID,
            name: { fi: HAKUKOHDE_NIMI },
            'tarjoaja-name': { fi: 'Testitarjoaja' },
          },
        ],
      })
  )

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await page
      .getByTestId('form-name-input')
      .fill('Hakukohdekohtaisen näkyvyyden testilomake')
    const toolbar = page.getByTestId('component-toolbar')
    await toolbar.hover()
    await toolbar.getByTestId('component-toolbar-tekstikenttä').click()
    await page
      .getByTestId('tekstikenttä-kysymys')
      .fill('Vain tietylle hakukohteelle näkyvä kysymys')
  })
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('rajaa kysymyksen näkyvyyden valittuun hakukohteeseen', async () => {
  await page.locator('button.belongs-to-hakukohteet__modal-toggle').click()

  // Uusi kysymys on oletuksena kokonaan piilotettu (:hidden true), kun
  // lomake on haun käytössä — "piilotettu"-tila menee näyttöteksissä aina
  // valittujen hakukohteiden edelle (ks. belongs-to-hakukohteet-component),
  // joten piilotus pitää poistaa ensin tästä valintaruudusta, ennen kuin
  // hakukohteen valinta vaikuttaa näkyvään tilaan mitenkään.
  await page
    .locator('.hakukohde-and-hakukohderyhma-visibility-checkbox input')
    .click()

  const hakukohdeRivi = page
    .locator('.hakukohde-and-hakukohderyhma-category-list-item')
    .filter({ hasText: HAKUKOHDE_NIMI })
  await expect(hakukohdeRivi).toBeVisible()
  await hakukohdeRivi.click()

  const valitutHakukohteet = page.locator(
    '.belongs-to-hakukohteet__hakukohde-label'
  )
  await expect(valitutHakukohteet).toHaveCount(1)
  await expect(valitutHakukohteet).toContainText(HAKUKOHDE_NIMI)
  await expect(
    page.locator('.belongs-to-hakukohteet__modal-toggle-label')
  ).toHaveText('vain valituille hakukohteille')
})
