import { randomUUID } from 'crypto'
import { test, expect, Page, Locator } from '@playwright/test'
import { waitForResponse } from '../playwright-utils'
import {
  FormNode,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  poistaLomake,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'parallel' })

// Testaa hakijan puolen dropdown-komponentin toimintaa minimaalisen lomakkeen kautta.
// Yksikkötestaaminen olisi hankalaa, koska komponentti käyttä re-framea tilan tallentamiseen.

const metadata = {
  'created-by': {
    name: 'system',
    oid: 'system',
    date: '1970-01-01T00:00:00Z',
  },
  'modified-by': {
    name: 'system',
    oid: 'system',
    date: '1970-01-01T00:00:00Z',
  },
}

// Hakemuksen käsittely (ks. mm. :application/validate-hakukohteet) olettaa
// aina tämän kentän olevan olemassa, vaikka lomakkeella ei olisi oikeita
// hakukohteita
const hakukohteetFieldFixture: FormNode = {
  fieldClass: 'formField',
  fieldType: 'hakukohteet',
  id: 'hakukohteet',
  label: { fi: 'Hakukohteet', sv: 'Ansökningsmål', en: 'Application options' },
  metadata,
  params: {},
  options: [],
  validators: ['hakukohteet'],
  'exclude-from-answers-if-hidden': true,
}

const DROPDOWN_LABEL = 'Valitse maa'
const CLEAR_BUTTON_LABEL = 'Tyhjennä'

const dropdownFieldFixture: FormNode = {
  fieldClass: 'formField',
  fieldType: 'dropdown',
  id: 'pw-single-dropdown',
  label: { fi: DROPDOWN_LABEL },
  metadata,
  params: {},
  validators: [],
  options: [
    { value: 'fi', label: { fi: 'Suomi' } },
    { value: 'se', label: { fi: 'Ruotsi' } },
    { value: 'no', label: { fi: 'Norja' } },
    { value: 'dk', label: { fi: 'Tanska' } },
    { value: 'is', label: { fi: 'Islanti' } },
    {
      value: 'kp',
      label: { fi: 'Korean demokraattinen kansantasavalta (Pohjois-Korea)' },
    },
    { value: 'us', label: { fi: 'Yhdysvallat (USA)' } },
    { value: 'de', label: { fi: 'Saksa' } },
    { value: 'ru', label: { fi: 'Venäjä' } },
    { value: 'za', label: { fi: 'Etelä-Afrikka' } },
    { value: 'ae', label: { fi: 'Yhdistyneet arabiemiirikunnat' } },
    { value: 'fo', label: { fi: 'Färsaaret' } },
  ],
}

const OPTION_COUNT = 12

// Toinen pudotusvalikko, jolla ei koskaan ole tyhjää/valitsematonta vaihtoehtoa. Testaa, että tyhjennysnappi pysyy piilossa tällaiselle kentälle valinnan jälkeenkin.
const NO_BLANK_OPTION_LABEL = 'Onko sinulla ajokortti?'

const noBlankOptionFieldFixture: FormNode = {
  fieldClass: 'formField',
  fieldType: 'dropdown',
  id: 'pw-no-blank-option-dropdown',
  label: { fi: NO_BLANK_OPTION_LABEL },
  metadata,
  params: {},
  'no-blank-option': true,
  options: [
    { value: 'true', label: { fi: 'Kyllä' } },
    { value: 'false', label: { fi: 'Ei' } },
  ],
}

const lomakkeenAvain = randomUUID()

const getFieldByLabel = (page: Page, label: string) =>
  page
    .locator('.application__form-field')
    .filter({ has: page.getByLabel(label, { exact: true }) })

const getField = (page: Page) => getFieldByLabel(page, DROPDOWN_LABEL)

const getCombobox = (page: Page) => getField(page).getByRole('combobox')

const getListbox = async (page: Page) => {
  const combobox = getCombobox(page)
  const listboxId = await combobox.getAttribute('aria-controls')
  const listbox = page.locator(`[id="${listboxId}"]`)
  return listbox
}

const getOptions = (listbox: Locator) => listbox.getByRole('option')

const getOption = (listbox: Locator, name: string) =>
  listbox.getByRole('option', { name })

const getClearButton = (page: Page) =>
  getField(page).getByRole('button', { name: CLEAR_BUTTON_LABEL })

const getTriggerButton = (page: Page) =>
  getField(page).locator('button.a-dropdown-trigger')

const getNoBlankOptionCombobox = (page: Page) =>
  getFieldByLabel(page, NO_BLANK_OPTION_LABEL).getByRole('combobox')

const getNoBlankOptionListbox = async (page: Page) => {
  const combobox = getNoBlankOptionCombobox(page)
  const listboxId = await combobox.getAttribute('aria-controls')
  const listbox = page.locator(`[id="${listboxId}"]`)
  return listbox
}

const getNoBlankOptionClearButton = (page: Page) =>
  getFieldByLabel(page, NO_BLANK_OPTION_LABEL).getByRole('button', {
    name: CLEAR_BUTTON_LABEL,
  })

// Odottaa, että näppäimistöllä siirretty korostus (ks. move-active-to
// dropdown_component.cljs:ssä) on oikeasti ehtinyt renderöityä ennen kuin
// Enter lähetetään — move-active-ton oma re-frame/dispatch on asynkroninen
// eikä siis ole vielä varmasti käsitelty pelkän edeltävän näppäinpainalluksen
// valmistumisesta.
const waitForActiveOption = async (page: Page, name: string) => {
  const listbox = await getListbox(page)
  const option = await getOption(listbox, name)
  const optionId = await option.getAttribute('id')
  await expect(getCombobox(page)).toHaveAttribute(
    'aria-activedescendant',
    optionId ?? ''
  )
}

const waitForNoActiveOption = async (page: Page) => {
  await expect(getCombobox(page)).not.toHaveAttribute('aria-activedescendant')
}

// Lähettää synteettisen, peruutettavissa olevan touchmove-tapahtuman
// annetulle elementille ja palauttaa, kutsuiko joku kuuntelija sille
// preventDefaultia (ks. make-fullscreen-touchmove-listener dropdown_
// component.cljs:ssä) — ei yritä todentaa itse sivun visuaalista
// vierittymistä/panorointia, koska sen simulointi ja havaitseminen
// luotettavasti pelkillä synteettisillä kosketustapahtumilla ei ole
// mielekästä (todellinen selaimen oma "panorointi" ei reagoi niihin).
const dispatchTouchmove = (locator: Locator): Promise<boolean> =>
  locator.evaluate((el) => {
    const touch = new Touch({
      identifier: 1,
      target: el,
      clientX: 10,
      clientY: 10,
    })
    const event = new TouchEvent('touchmove', {
      bubbles: true,
      cancelable: true,
      touches: [touch],
      targetTouches: [touch],
      changedTouches: [touch],
    })
    el.dispatchEvent(event)
    return event.defaultPrevented
  })

const avaaLomake = async (page: Page): Promise<void> => {
  await Promise.all([
    page.goto(getHakijanNakymanOsoite(lomakkeenAvain)),
    waitForResponse(page, 'GET', (url) =>
      url.includes(getLomakkeenHaunOsoite(lomakkeenAvain))
    ),
  ])
}

// Ei haeta oletuslomakkeen sisältöä (hakukohteet + koko henkilötieto-
// moduuli) editorin "Uusi lomake" -napin kautta, kuten muut hakija-testit
// yleensä tekevät (ks. haeOletuslomakkeenSisalto playwright-ataru-utils.
// ts:ssä) — tämän tiedoston omat kentät eivät tarvitse henkilötieto-
// moduulia ollenkaan, pelkkä hakukohteetFieldFixture riittää pitämään
// hakijan puolen re-frame-koodin tyytyväisenä, ja sen välttäminen säästää
// yhden ylimääräisen editori-UI:n kautta kulkevan luonti-/haku-/poisto-
// kierroksen jokaiselta testiajolta.
test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage()
  await kirjauduVirkailijanNakymaan(page)
  await luoLomakeAvaimella(page, lomakkeenAvain, [
    hakukohteetFieldFixture,
    noBlankOptionFieldFixture,
    dropdownFieldFixture,
  ])
  await page.close()
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
})

test.describe('Työpöytänäkymä', () => {
  test.use({ viewport: { width: 1280, height: 800 } })

  test.beforeEach(async ({ page }) => {
    await avaaLomake(page)
  })

  test('valikko on aluksi tyhjä ja suljettu', async ({ page }) => {
    await expect(getCombobox(page)).toHaveValue('')
    await expect(await getListbox(page)).toBeHidden()
    await expect(getClearButton(page)).toHaveCount(0)
  })

  test('klikkaus avaa valikon ja näyttää kaikki vaihtoehdot', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    const options = await getOptions(listbox)
    await getCombobox(page).click()
    await expect(listbox).toBeVisible()
    await expect(options).toHaveCount(OPTION_COUNT)
  })

  test('pitkätkin vaihtoehdot pysyvät näkymän leveyden sisällä', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).click()
    const viewport = page.viewportSize()
    const fieldBox = await getField(page).boundingBox()
    const listboxBox = await listbox.boundingBox()
    expect(fieldBox?.width).toBeLessThanOrEqual(viewport?.width ?? Infinity)
    expect(listboxBox?.width).toBeLessThanOrEqual(viewport?.width ?? Infinity)
  })

  test('kirjoittaminen suodattaa vaihtoehdot kirjainkoosta riippumatta', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    const options = getOptions(listbox)

    await getCombobox(page).click()
    await getCombobox(page).fill('RUOT')
    await expect(options).toHaveCount(1)
    await expect(getOption(listbox, 'Ruotsi')).toBeVisible()
  })

  test('haun tyhjentäminen palauttaa kaikki vaihtoehdot näkyviin', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    const options = getOptions(listbox)

    await getCombobox(page).click()
    await getCombobox(page).fill('RUOT')
    await expect(options).toHaveCount(1)
    await getCombobox(page).fill('')
    await expect(options).toHaveCount(OPTION_COUNT)
  })

  test('vaihtoehdon klikkaaminen valitsee sen, sulkee valikon ja näyttää tyhjennysnapin', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).click()
    await getOption(listbox, 'Ruotsi').click()
    await expect(getCombobox(page)).toHaveValue('Ruotsi')
    await expect(listbox).toBeHidden()
    await expect(getClearButton(page)).toBeVisible()
  })

  test('tyhjennysnappi tyhjentää valinnan ja katoaa itse', async ({ page }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).click()
    await getOption(listbox, 'Ruotsi').click()
    await getClearButton(page).click()
    await expect(getCombobox(page)).toHaveValue('')
    await expect(getClearButton(page)).toBeHidden()
  })

  test('nuolinäppäimellä ja Enterillä voi valita ensimmäisen vaihtoehdon suljetusta valikosta', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).focus()
    await getCombobox(page).press('ArrowDown')
    await expect(listbox).toBeVisible()
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Suomi')
    await expect(listbox).toBeHidden()
  })

  test('End valitsee listan viimeisen ja Home ensimmäisen vaihtoehdon', async ({
    page,
  }) => {
    // Klikkaus ja jokainen näppäinpainallus laukaisevat oman, toisistaan
    // erillisen re-frame/dispatchinsa (ks. open-popup/move-active-to
    // dropdown_component.cljs:ssä), joka renderöityy asynkronisesti. Ilman
    // odotusta välissä Enter voi osua renderiin, jossa valikko ei ole vielä
    // auki tai korostus ei ole vielä siirtynyt End/Home-näppäimen
    // kohteeseen (ks. waitForActiveOption).
    const listbox = await getListbox(page)
    await getCombobox(page).click()
    await expect(listbox).toBeVisible()
    await getCombobox(page).press('End')
    await waitForActiveOption(page, 'Färsaaret')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Färsaaret')

    await getCombobox(page).click()
    await expect(listbox).toBeVisible()
    await getCombobox(page).press('Home')
    await waitForActiveOption(page, 'Suomi')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Suomi')
  })

  test('Escape sulkee valikon eikä muuta olemassa olevaa valintaa', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).click()
    await getOption(listbox, 'Suomi').click()

    await getCombobox(page).click()
    await expect(listbox).toBeVisible()
    await getCombobox(page).press('Escape')
    await expect(listbox).toBeHidden()
    await expect(getCombobox(page)).toHaveValue('Suomi')
  })

  test('klikkaus valikon ulkopuolelle sulkee sen', async ({ page }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).click()
    await expect(listbox).toBeVisible()
    await page.getByTestId('application-header-label').click()
    await expect(listbox).toBeHidden()
  })

  test('siirtyminen kenttään näppäimistöllä (Tab) ei avaa valikkoa, mutta kirjoittaminen avaa', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getNoBlankOptionCombobox(page).focus()
    await page.keyboard.press('Tab')
    await expect(getCombobox(page)).toBeFocused()
    await expect(listbox).toBeHidden()
    await getCombobox(page).fill('S')
    await expect(listbox).toBeVisible()
  })

  test('avausnapin klikkaus avaa ja sulkee valikon', async ({ page }) => {
    const listbox = await getListbox(page)
    await getTriggerButton(page).click()
    await expect(listbox).toBeVisible()
    await getTriggerButton(page).click()
    await expect(listbox).toBeHidden()
  })

  test('kentän oman labelin klikkaus sulkee auki olevan valikon työpöydällä', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).click()
    await expect(listbox).toBeVisible()
    await getField(page).locator('label').click()
    await expect(listbox).toBeHidden()
  })

  test('ei-tyhjennettävä kenttä ei näytä tyhjennysnappia valinnan jälkeenkään', async ({
    page,
  }) => {
    await getNoBlankOptionCombobox(page).click()
    const listbox = await getNoBlankOptionListbox(page)
    await getOption(listbox, 'Kyllä').click()
    await expect(getNoBlankOptionCombobox(page)).toHaveValue('Kyllä')
    await expect(getNoBlankOptionClearButton(page)).toHaveCount(0)
  })

  test('ArrowDown auki olevassa valikossa siirtää kohdistusta eteenpäin yksi vaihtoehto kerrallaan', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await getCombobox(page).press('ArrowDown')
    await waitForActiveOption(page, 'Suomi')
    await getCombobox(page).press('ArrowDown')
    await waitForActiveOption(page, 'Ruotsi')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Ruotsi')
  })

  test('ArrowUp siirtää kohdistusta taaksepäin ja lopulta pois listasta', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await getCombobox(page).press('ArrowDown')
    await waitForActiveOption(page, 'Suomi')
    await getCombobox(page).press('ArrowDown')
    await waitForActiveOption(page, 'Ruotsi')
    await getCombobox(page).press('ArrowUp')
    await waitForActiveOption(page, 'Suomi')
    await getCombobox(page).press('ArrowUp')
    await waitForNoActiveOption(page)
    await getCombobox(page).press('ArrowDown')
    await waitForActiveOption(page, 'Suomi')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Suomi')
  })

  test('näppäimet eivät tee mitään kun suodatettu lista on tyhjä', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await getCombobox(page).fill('ei osumaa xyz')
    const listbox = await getListbox(page)
    await expect(listbox).toContainText('Ei hakutuloksia')
    await getCombobox(page).press('ArrowDown')
    await getCombobox(page).press('ArrowUp')
    await getCombobox(page).press('Home')
    await getCombobox(page).press('End')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('ei osumaa xyz')
    await expect(listbox).toContainText('Ei hakutuloksia')
    await getCombobox(page).press('Escape')
    await expect(getCombobox(page)).toHaveValue('')
  })
})

test.describe('Mobiilinäkymä', () => {
  test.use({ viewport: { width: 390, height: 844 }, hasTouch: true })

  test.beforeEach(async ({ page }) => {
    await avaaLomake(page)
  })

  test('valikko on aluksi tyhjä ja suljettu', async ({ page }) => {
    const listbox = await getListbox(page)
    await expect(getCombobox(page)).toHaveValue('')
    await expect(listbox).toBeHidden()
    await expect(getClearButton(page)).toHaveCount(0)
  })

  test('napautus avaa valikon ja vaihtoehdot ovat näkyvissä', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    const listbox = await getListbox(page)
    await expect(listbox).toBeVisible()
    await expect(getOptions(listbox)).toHaveCount(OPTION_COUNT)
  })

  test('kentän oman labelin napautus ei sulje auki olevaa valikkoa mobiilissa', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).tap()
    await expect(listbox).toBeVisible()
    await getField(page).locator('label').tap()
    await expect(listbox).toBeVisible()
  })

  test('pitkätkin vaihtoehdot pysyvät kokoruutunäkymän leveyden sisällä', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    const listbox = await getListbox(page)
    const viewport = page.viewportSize()
    const fieldBox = await getField(page).boundingBox()
    const listboxBoundingBox = await listbox.boundingBox()
    expect(fieldBox?.width).toBeLessThanOrEqual(viewport?.width ?? Infinity)
    expect(listboxBoundingBox?.width).toBeLessThanOrEqual(
      viewport?.width ?? Infinity
    )
  })

  test('valikon uudelleennapautus ei sulje sitä ja vaihtoehdon voi silti valita', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).tap()
    await expect(listbox).toBeVisible()
    await getCombobox(page).tap()
    await expect(listbox).toBeVisible()
    await expect(getOptions(listbox)).toHaveCount(OPTION_COUNT)
    await getOption(listbox, 'Norja').tap()
    await expect(getCombobox(page)).toHaveValue('Norja')
    await expect(listbox).toBeHidden()
  })

  test('valikon voi avata uudelleen valinnan jälkeen', async ({ page }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).tap()
    await getOption(listbox, 'Norja').tap()

    await getCombobox(page).tap()
    await expect(listbox).toBeVisible()
    await expect(getOptions(listbox)).toHaveCount(OPTION_COUNT)
  })

  test('kirjoittaminen suodattaa vaihtoehdot myös mobiilissa', async ({
    page,
  }) => {
    const listbox = await getListbox(page)
    await getCombobox(page).tap()
    await getCombobox(page).fill('saar')
    await expect(getOptions(listbox)).toHaveCount(1)
    await expect(getOption(listbox, 'Färsaaret')).toBeVisible()
    await getCombobox(page).fill('')
    await expect(getOptions(listbox)).toHaveCount(OPTION_COUNT)
  })

  test('tyhjennysnappi näkyy valinnan jälkeen ja tyhjentää arvon', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    const listbox = await getListbox(page)
    await getOption(listbox, 'Norja').tap()
    await expect(getClearButton(page)).toBeVisible()
    await getClearButton(page).tap()
    await expect(getCombobox(page)).toHaveValue('')
    await expect(getClearButton(page)).toBeHidden()
  })

  test('kokoruutuvalikon ollessa auki touchmove ei vieritä/panoroi sivua muualla, mutta listan sisällä se sallitaan', async ({
    page,
  }) => {
    expect(await dispatchTouchmove(page.locator('body'))).toBe(false)

    await getCombobox(page).tap()
    await expect(await getListbox(page)).toBeVisible()

    const label = page.getByText(DROPDOWN_LABEL, { exact: true })

    const banner = page.locator('.application__banner-container')

    expect(await dispatchTouchmove(page.locator('body'))).toBe(true)
    expect(await dispatchTouchmove(label)).toBe(true)
    expect(await dispatchTouchmove(banner)).toBe(true)

    const listbox = await getListbox(page)

    expect(await dispatchTouchmove((await getOptions(listbox)).first())).toBe(
      false
    )
  })
})
