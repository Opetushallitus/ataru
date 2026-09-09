import { test, expect, Page, Locator } from '@playwright/test'
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

const formName = `Kysymysryhmä: testilomake ${Date.now()}`

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page)

  const lomake = await lisaaLomake(page)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await page.getByTestId('form-name-input').fill(formName)
  })
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

// .last(): kysymysryhmän oma "lisää seuraava kenttä" -työkalurivi renderöityy
// aina kaikkien sen lapsikenttien JÄLKEEN, joten se on DOM-järjestyksessä
// viimeinen silloinkin, kun joku jo lisätyistä lapsista (esim. vielä
// täyttymätön vierekkäiskenttäryhmä) näyttää samaan aikaan oman
// component-toolbar-testid:llä varustetun työkalurivinsä. Silloin, kun
// osumia on vain yksi, .last() ei muuta mitään.
const componentToolbar = (loc: Page | Locator) =>
  loc.getByTestId('component-toolbar').last()

// Lomakkeen kasvaessa työkalurivin avauspainike voi vierähtää kiinteästi
// paikallaan pysyvän esikatselu-työkalurivin (.editor-form__toolbar) alle,
// jolloin normaali klikkaus jää odottamaan elementin vakautumista
// loputtomiin ("... subtree intercepts pointer events"). Vieritys pitää
// tehdä ENNEN hover()-kutsua eikä sen jälkeen: hover-tila perustuu oikeaan
// hiiren sijaintiin, joten kohteen vierittäminen hoverin JÄLKEEN siirtäisi
// elementin pois paikallaan pysyvän hiiren alta ja sulkisi juuri avatun
// valikon (näkyy virheenä "element was detached from the DOM, retrying").
// Vieritetään siksi itse avauspainike (eikä vasta hoverin paljastama
// valikkokohde) näkymän keskelle ennen hoveria.
const scrollToViewportCenter = (locator: Locator) =>
  locator.evaluate((el) => el.scrollIntoView({ block: 'center' }))

const hoverComponentToolbar = async (loc: Page | Locator) => {
  const toolbar = componentToolbar(loc)
  await scrollToViewportCenter(toolbar)
  await toolbar.hover()
  return toolbar
}

const clickComponentToolbar = async (
  loc: Page | Locator,
  component: string
) => {
  const toolbar = await hoverComponentToolbar(loc)
  await toolbar.getByTestId(`component-toolbar-${component}`).click()
}

const clickComponentToolbarByText = async (
  loc: Page | Locator,
  text: string
) => {
  const toolbar = await hoverComponentToolbar(loc)
  await toolbar.getByText(text, { exact: true }).click()
}

// Vierekkäiskenttäryhmän oma "lisää tekstikenttä" -valikko sisältää vain yhden
// nimettömän vaihtoehdon (ks. adjacent-fieldset-toolbar-elements
// toolbar.cljs:ssä), joten sitä ei voi valita testid:llä.
const clickAddAdjacentTextField = async (adjacentFieldset: Locator) => {
  const toolbar = await hoverComponentToolbar(adjacentFieldset)
  await toolbar.locator('li').click()
}

const toolbarItemTexts = async (loc: Page | Locator) => {
  const toolbar = componentToolbar(loc)
  await toolbar.hover()
  return toolbar.locator('li').allTextContents()
}

const pakollinen = (loc: Locator) => loc.getByLabel('Pakollinen')

// Sama järjestys kuin src/cljs/ataru/virkailija/editor/components/toolbar.cljs:n
// followup-toolbar-element-names (suodatettu toolbar-elements-vektorista, joka
// säilyttää järjestyksen) — "Kysymysryhmä" puuttuu kysymysryhmän omasta
// työkaluriviltä, koska kysymysryhmiä ei voi sisäkkäistää.
const FOLLOWUP_TOOLBAR_ITEMS = [
  'Painikkeet, yksi valittavissa',
  'Painikkeet, yksi valittavissa, koodisto',
  'Pudotusvalikko',
  'Pudotusvalikko, koodisto',
  'Lista, monta valittavissa',
  'Lista, monta valittavissa, koodisto',
  'Tekstikenttä',
  'Tekstialue',
  'Vierekkäiset tekstikentät',
  'Liitepyyntö',
  'Kysymysryhmä',
  'Infoteksti',
  'Infoteksti, koko ruutu',
]
const QUESTION_GROUP_TOOLBAR_ITEMS = FOLLOWUP_TOOLBAR_ITEMS.filter(
  (item) => item !== 'Kysymysryhmä'
)

test('luo tyhjän lomakkeen', async () => {
  await expect(page.getByTestId('form-name-input')).toHaveValue(formName)
  await expect(page.locator('.editor-form__component-wrapper')).toHaveCount(3)
  await expect(
    page
      .locator('.editor-form__component-wrapper')
      .filter({ hasText: 'Henkilötiedot' })
  ).toHaveCount(1)
})

test('lisää päätason pudotusvalikko', async () => {
  await clickComponentToolbar(page, 'dropdown')

  const dropdown = page.getByTestId('editor-form__dropdown-component-wrapper')
  await dropdown
    .getByTestId('editor-form__dropdown-label')
    .fill('Päätaso: pudotusvalikko')

  const options = dropdown
    .getByTestId('editor-form__multi-options-container')
    .locator('.editor-form__multi-options-wrapper-outer')
  // Uusi pudotusvalikko sisältää oletuksena kaksi tyhjää vaihtoehtoa, joten
  // niitä ei tarvitse lisätä erikseen (ks. component.cljc:n dropdown-funktio).
  await options.nth(0).locator('input').fill('Päätaso: A')
  await options.nth(1).locator('input').fill('Päätaso: B')

  await pakollinen(dropdown).click()

  await expect(dropdown.getByTestId('editor-form__dropdown-label')).toHaveValue(
    'Päätaso: pudotusvalikko'
  )
  await expect(options.nth(0).locator('input')).toHaveValue('Päätaso: A')
  await expect(options.nth(1).locator('input')).toHaveValue('Päätaso: B')
  await expect(pakollinen(dropdown)).toBeChecked()
  await expect(page.locator('.editor-form__component-wrapper')).toHaveCount(4)
})

test('näyttää jatkokysymysten työkalurivin sisällön', async () => {
  const dropdown = page.getByTestId('editor-form__dropdown-component-wrapper')
  const options = dropdown
    .getByTestId('editor-form__multi-options-container')
    .locator('.editor-form__multi-options-wrapper-outer')

  await options.nth(1).getByTestId('followup-question-followups').click()

  await expect(toolbarItemTexts(dropdown)).resolves.toEqual(
    FOLLOWUP_TOOLBAR_ITEMS
  )
})

test('lisää kysymysryhmä jatkokysymykseksi', async () => {
  const dropdown = page.getByTestId('editor-form__dropdown-component-wrapper')

  await clickComponentToolbarByText(dropdown, 'Kysymysryhmä')

  const questionGroup = page.getByTestId(
    'editor-form__questionGroup-component-wrapper'
  )
  await questionGroup
    .locator('.editor-form__text-field')
    .first()
    .fill('Kysymysryhmä: ryhmän otsikko')

  await expect(
    questionGroup.locator('.editor-form__text-field').first()
  ).toHaveValue('Kysymysryhmä: ryhmän otsikko')
  await expect(toolbarItemTexts(questionGroup)).resolves.toEqual(
    QUESTION_GROUP_TOOLBAR_ITEMS
  )
})

const questionGroupWrapper = (page: Page) =>
  page.getByTestId('editor-form__questionGroup-component-wrapper')

// soresu->reagent (editor/core.cljs) kietoo jokaisen lapsikomponentin omaan
// div-wrapperiinsa raahaus-pudotus-tyhjentäjää varten, joten
// .editor-form__component-wrapper ei ole .editor-form__wrapper-element-wellin
// suora lapsi vaan lapsenlapsi.
const nestedComponentWrapper = (group: Locator, index: number) =>
  group
    .locator(
      '.editor-form__wrapper-element-well > div > .editor-form__component-wrapper'
    )
    .nth(index)

test('lisää pudotusvalikko kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'dropdown')

  const nested = nestedComponentWrapper(group, 0)
  const label = nested.getByTestId('editor-form__dropdown-label')
  await label.fill('Kysymysryhmä: pudotusvalikko')

  const options = nested
    .getByTestId('editor-form__multi-options-container')
    .locator('.editor-form__multi-options-wrapper-outer')
  await options.nth(0).locator('input').fill('Pudotusvalikko: A')
  await options.nth(1).locator('input').fill('Pudotusvalikko: B')
  await pakollinen(nested).click()

  await expect(label).toHaveValue('Kysymysryhmä: pudotusvalikko')
  await expect(options.nth(0).locator('input')).toHaveValue('Pudotusvalikko: A')
  await expect(options.nth(1).locator('input')).toHaveValue('Pudotusvalikko: B')
  await expect(pakollinen(nested)).toBeChecked()
})

test('lisää painikkeet, yksi valittavissa -kysymys kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'painikkeet-yksi-valittavissa')

  const nested = nestedComponentWrapper(group, 1)
  await nested
    .getByTestId('editor-form__singleChoice-label')
    .fill('Kysymysryhmä: painikkeet, yksi valittavissa')

  const options = nested
    .getByTestId('editor-form__multi-options-container')
    .locator('.editor-form__multi-options-wrapper-outer')
  await nested.locator('.editor-form__add-dropdown-item a').click()
  await options
    .nth(0)
    .locator('.editor-form__text-field')
    .fill('Painikkeet, yksi valittavissa: A')
  await nested.locator('.editor-form__add-dropdown-item a').click()
  await options
    .nth(1)
    .locator('.editor-form__text-field')
    .fill('Painikkeet, yksi valittavissa: B')
  await pakollinen(nested).click()

  await expect(
    nested.getByTestId('editor-form__singleChoice-label')
  ).toHaveValue('Kysymysryhmä: painikkeet, yksi valittavissa')
  await expect(options.nth(0).locator('.editor-form__text-field')).toHaveValue(
    'Painikkeet, yksi valittavissa: A'
  )
  await expect(options.nth(1).locator('.editor-form__text-field')).toHaveValue(
    'Painikkeet, yksi valittavissa: B'
  )
  await expect(pakollinen(nested)).toBeChecked()
})

test('lisää lista, monta valittavissa -kysymys kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'multiple-choice')

  const nested = nestedComponentWrapper(group, 2)
  await nested
    .getByTestId('editor-form__multipleChoice-label')
    .fill('Kysymysryhmä: lista, monta valittavissa')

  const options = nested
    .getByTestId('editor-form__multi-options-container')
    .locator('.editor-form__multi-options-wrapper-outer')
  await nested.locator('.editor-form__add-dropdown-item a').click()
  await options.nth(0).locator('input').fill('Lista, monta valittavissa: A')
  await nested.locator('.editor-form__add-dropdown-item a').click()
  await options.nth(1).locator('input').fill('Lista, monta valittavissa: B')
  await pakollinen(nested).click()

  await expect(
    nested.getByTestId('editor-form__multipleChoice-label')
  ).toHaveValue('Kysymysryhmä: lista, monta valittavissa')
  await expect(options.nth(0).locator('input')).toHaveValue(
    'Lista, monta valittavissa: A'
  )
  await expect(options.nth(1).locator('input')).toHaveValue(
    'Lista, monta valittavissa: B'
  )
  await expect(pakollinen(nested)).toBeChecked()
})

test('lisää yksivastauksinen tekstikenttä kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'tekstikenttä')

  const nested = nestedComponentWrapper(group, 3)
  await nested
    .getByTestId('tekstikenttä-kysymys')
    .fill('Tekstikenttä, yksi vastaus')
  await pakollinen(nested).click()

  await expect(nested.getByTestId('tekstikenttä-kysymys')).toHaveValue(
    'Tekstikenttä, yksi vastaus'
  )
  await expect(pakollinen(nested)).toBeChecked()
  await expect(
    nested.getByTestId('tekstikenttä-valinta-voi-lisätä-useita')
  ).not.toBeChecked()
})

test('lisää monivastauksinen tekstikenttä kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'tekstikenttä')

  const nested = nestedComponentWrapper(group, 4)
  await nested
    .getByTestId('tekstikenttä-kysymys')
    .fill('Tekstikenttä, monta vastausta')
  await pakollinen(nested).click()
  await nested.getByTestId('tekstikenttä-valinta-voi-lisätä-useita').click()

  await expect(nested.getByTestId('tekstikenttä-kysymys')).toHaveValue(
    'Tekstikenttä, monta vastausta'
  )
  await expect(pakollinen(nested)).toBeChecked()
  await expect(
    nested.getByTestId('tekstikenttä-valinta-voi-lisätä-useita')
  ).toBeChecked()
})

test('lisää tekstialue kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'tekstialue')

  const nested = nestedComponentWrapper(group, 5)
  await nested.getByTestId('tekstikenttä-kysymys').fill('Tekstialue')
  await pakollinen(nested).click()

  await expect(nested.getByTestId('tekstikenttä-kysymys')).toHaveValue(
    'Tekstialue'
  )
  await expect(pakollinen(nested)).toBeChecked()
})

// Sama soresu->reagent-kääre koskee myös vierekkäiskenttäryhmän lapsia.
const adjacentChild = (adjacentFieldset: Locator, index: number) =>
  adjacentFieldset
    .locator(
      '.editor-form__adjacent-fieldset-container > div > .editor-form__component-wrapper'
    )
    .nth(index)

test('lisää yksivastauksinen vierekkäiskenttä kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'adjacent-fieldset')

  const nested = nestedComponentWrapper(group, 6)
  await nested
    .locator('.editor-form__text-field')
    .first()
    .fill('Vierekkäiset tekstikentät, yksi vastaus')

  await clickAddAdjacentTextField(nested)
  const firstChild = adjacentChild(nested, 0)
  await firstChild
    .locator('.editor-form__text-field')
    .fill('Vierekkäiset tekstikentät, yksi vastaus: A')
  await pakollinen(firstChild).click()

  await clickAddAdjacentTextField(nested)
  const secondChild = adjacentChild(nested, 1)
  await secondChild
    .locator('.editor-form__text-field')
    .fill('Vierekkäiset tekstikentät, yksi vastaus: B')
  await pakollinen(secondChild).click()

  await expect(nested.locator('.editor-form__text-field').first()).toHaveValue(
    'Vierekkäiset tekstikentät, yksi vastaus'
  )
  await expect(nested.getByLabel('useita vastauksia')).not.toBeChecked()
  await expect(firstChild.locator('.editor-form__text-field')).toHaveValue(
    'Vierekkäiset tekstikentät, yksi vastaus: A'
  )
  await expect(pakollinen(firstChild)).toBeChecked()
  await expect(secondChild.locator('.editor-form__text-field')).toHaveValue(
    'Vierekkäiset tekstikentät, yksi vastaus: B'
  )
  await expect(pakollinen(secondChild)).toBeChecked()
})

test('lisää monivastauksinen vierekkäiskenttä kysymysryhmään', async () => {
  const group = questionGroupWrapper(page)
  await clickComponentToolbar(group, 'adjacent-fieldset')

  const nested = nestedComponentWrapper(group, 7)
  await nested
    .locator('.editor-form__text-field')
    .first()
    .fill('Vierekkäiset tekstikentät, monta vastausta')
  await nested.getByLabel('useita vastauksia').click()

  await clickAddAdjacentTextField(nested)
  const firstChild = adjacentChild(nested, 0)
  await firstChild
    .locator('.editor-form__text-field')
    .fill('Vierekkäiset tekstikentät, monta vastausta: A')
  await pakollinen(firstChild).click()

  await clickAddAdjacentTextField(nested)
  const secondChild = adjacentChild(nested, 1)
  await secondChild
    .locator('.editor-form__text-field')
    .fill('Vierekkäiset tekstikentät, monta vastausta: B')
  await pakollinen(secondChild).click()

  await expect(nested.locator('.editor-form__text-field').first()).toHaveValue(
    'Vierekkäiset tekstikentät, monta vastausta'
  )
  await expect(nested.getByLabel('useita vastauksia')).toBeChecked()
  await expect(firstChild.locator('.editor-form__text-field')).toHaveValue(
    'Vierekkäiset tekstikentät, monta vastausta: A'
  )
  await expect(pakollinen(firstChild)).toBeChecked()
  await expect(secondChild.locator('.editor-form__text-field')).toHaveValue(
    'Vierekkäiset tekstikentät, monta vastausta: B'
  )
  await expect(pakollinen(secondChild)).toBeChecked()
})

test('tallentaa muutokset automaattisesti', async () => {
  await expect(
    page.locator('.top-banner .flasher span').filter({
      hasText: 'Kaikki muutokset tallennettu',
    })
  ).toBeVisible()
})
