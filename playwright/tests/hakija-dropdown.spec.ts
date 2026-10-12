import { randomUUID } from 'crypto'
import { test, expect, Page, Locator } from '@playwright/test'
import { waitForResponse } from '../playwright-utils'
import {
  FormNode,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  haeOletuslomakkeenSisalto,
  kirjauduVirkailijanNakymaan,
  luoLomakeAvaimella,
  poistaLomake,
} from '../playwright-ataru-utils'

test.describe.configure({ mode: 'parallel' })

// Testaa ataru.application-common.components.dropdown-component/dropdown-
// komponentin toimintaa käyttäjän näkökulmasta (haku, näppäimistönavigointi,
// valinta, tyhjennys) sekä työpöytä- että mobiilinäkymässä. Lomakkeella on
// tarkoituksella vain yksi käyttäjän määrittelemä kenttä — pudotusvalikko.
//
// Oletuslomakkeen henkilötietomoduulissa on kuitenkin muitakin samaa
// komponenttia käyttäviä kenttiä (esim. kotikunta, kansalaisuus), joten
// kaikki locatorit skoopataan tämän yhden kentän omaan
// .application__form-field-wrapperiin nimen (label) perusteella sen sijaan,
// että luotettaisiin sivun ainoaan pudotusvalikkoon.

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
  // Useita vaihtoehtoja (jotta lista myös vierittyy) ja mukana muutama
  // huomattavan pitkä, katkeamaton nimi — nämä eivät mahdu kentän/listan
  // leveyteen, joten ne osuvat samaan flexbox-min-width-loukkuun, joka
  // aiemmin levensi koko kentän yli 100 % leveän (ks. min-width: 0
  // dropdown-component.less:ssä).
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

const getListbox = (page: Page) => getField(page).getByRole('listbox')

const getOptions = (page: Page) => getField(page).getByRole('option')

const getOption = (page: Page, name: string) =>
  getField(page).getByRole('option', { name })

const getClearButton = (page: Page) =>
  getField(page).getByRole('button', { name: CLEAR_BUTTON_LABEL })

// Avausnappi (karetti) on aria-hidden ja tab-index -1 — se on tarkoituksella
// poissa saavutettavuuspuusta, joten sitä ei voi (eikä pidä) tavoittaa
// getByRolella. Tämä on ainoa paikka koko tiedostossa, jossa locator
// osoittaa suoraan komponentin omaan CSS-luokkaan.
const getTriggerButton = (page: Page) =>
  getField(page).locator('button.a-dropdown-trigger')

const getNoBlankOptionCombobox = (page: Page) =>
  getFieldByLabel(page, NO_BLANK_OPTION_LABEL).getByRole('combobox')

const getNoBlankOptionOption = (page: Page, name: string) =>
  getFieldByLabel(page, NO_BLANK_OPTION_LABEL).getByRole('option', { name })

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
  const optionId = await getOption(page, name).getAttribute('id')
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

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage()
  await kirjauduVirkailijanNakymaan(page)
  const sisalto = await haeOletuslomakkeenSisalto(page)
  await luoLomakeAvaimella(page, lomakkeenAvain, [
    ...sisalto,
    dropdownFieldFixture,
    noBlankOptionFieldFixture,
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
    await expect(getListbox(page)).toBeHidden()
    await expect(getClearButton(page)).toHaveCount(0)
  })

  test('klikkaus avaa valikon ja näyttää kaikki vaihtoehdot', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await expect(getListbox(page)).toBeVisible()
    await expect(getOptions(page)).toHaveCount(OPTION_COUNT)
  })

  test('pitkätkin vaihtoehdot pysyvät näkymän leveyden sisällä', async ({
    page,
  }) => {
    await getCombobox(page).click()
    const viewport = page.viewportSize()
    const fieldBox = await getField(page).boundingBox()
    const listboxBox = await getListbox(page).boundingBox()
    expect(fieldBox?.width).toBeLessThanOrEqual(viewport?.width ?? Infinity)
    expect(listboxBox?.width).toBeLessThanOrEqual(viewport?.width ?? Infinity)
  })

  test('kirjoittaminen suodattaa vaihtoehdot kirjainkoosta riippumatta', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await getCombobox(page).fill('RUOT')
    await expect(getOptions(page)).toHaveCount(1)
    await expect(getOption(page, 'Ruotsi')).toBeVisible()
  })

  test('haun tyhjentäminen palauttaa kaikki vaihtoehdot näkyviin', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await getCombobox(page).fill('RUOT')
    await getCombobox(page).fill('')
    await expect(getOptions(page)).toHaveCount(OPTION_COUNT)
  })

  test('vaihtoehdon klikkaaminen valitsee sen, sulkee valikon ja näyttää tyhjennysnapin', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await getOption(page, 'Ruotsi').click()
    await expect(getCombobox(page)).toHaveValue('Ruotsi')
    await expect(getListbox(page)).toBeHidden()
    await expect(getClearButton(page)).toBeVisible()
  })

  test('tyhjennysnappi tyhjentää valinnan ja katoaa itse', async ({ page }) => {
    await getCombobox(page).click()
    await getOption(page, 'Ruotsi').click()
    await getClearButton(page).click()
    await expect(getCombobox(page)).toHaveValue('')
    await expect(getClearButton(page)).toBeHidden()
  })

  test('nuolinäppäimellä ja Enterillä voi valita ensimmäisen vaihtoehdon suljetusta valikosta', async ({
    page,
  }) => {
    await getCombobox(page).focus()
    await getCombobox(page).press('ArrowDown')
    await expect(getListbox(page)).toBeVisible()
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Suomi')
    await expect(getListbox(page)).toBeHidden()
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
    await getCombobox(page).click()
    await expect(getListbox(page)).toBeVisible()
    await getCombobox(page).press('End')
    await waitForActiveOption(page, 'Färsaaret')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Färsaaret')

    await getCombobox(page).click()
    await expect(getListbox(page)).toBeVisible()
    await getCombobox(page).press('Home')
    await waitForActiveOption(page, 'Suomi')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('Suomi')
  })

  test('Escape sulkee valikon eikä muuta olemassa olevaa valintaa', async ({
    page,
  }) => {
    await getCombobox(page).click()
    await getOption(page, 'Suomi').click()

    await getCombobox(page).click()
    await expect(getListbox(page)).toBeVisible()
    await getCombobox(page).press('Escape')
    await expect(getListbox(page)).toBeHidden()
    await expect(getCombobox(page)).toHaveValue('Suomi')
  })

  test('klikkaus valikon ulkopuolelle sulkee sen', async ({ page }) => {
    await getCombobox(page).click()
    await expect(getListbox(page)).toBeVisible()
    await page.getByTestId('application-header-label').click()
    await expect(getListbox(page)).toBeHidden()
  })

  test('siirtyminen kenttään näppäimistöllä (Tab) ei avaa valikkoa, mutta kirjoittaminen avaa', async ({
    page,
  }) => {
    await page.getByLabel('Äidinkieli').focus()
    await page.keyboard.press('Tab')
    await expect(getCombobox(page)).toBeFocused()
    await expect(getListbox(page)).toBeHidden()
    await getCombobox(page).fill('S')
    await expect(getListbox(page)).toBeVisible()
  })

  test('avausnapin klikkaus avaa ja sulkee valikon', async ({ page }) => {
    await getTriggerButton(page).click()
    await expect(getListbox(page)).toBeVisible()
    await getTriggerButton(page).click()
    await expect(getListbox(page)).toBeHidden()
  })

  test('kentän oman labelin klikkaus sulkee auki olevan valikon työpöydällä', async ({
    page,
  }) => {
    // Työpöydällä label ja kenttä ovat selvästi erilliset elementit, joten
    // labelin klikkaus käyttäytyy kuten mikä tahansa muu ulkopuolinen
    // klikkaus (ks. make-outside-click-listener dropdown_component.cljs:ssä
    // — poikkeus labelille on käytössä vain mobiilissa).
    await getCombobox(page).click()
    await expect(getListbox(page)).toBeVisible()
    await getField(page).locator('label').click()
    await expect(getListbox(page)).toBeHidden()
  })

  test('ei-tyhjennettävä kenttä ei näytä tyhjennysnappia valinnan jälkeenkään', async ({
    page,
  }) => {
    await getNoBlankOptionCombobox(page).click()
    await getNoBlankOptionOption(page, 'Kyllä').click()
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
    // Korostuksen puuttuessa seuraava ArrowDown palaa listan alkuun sen
    // sijaan, että se jatkaisi siitä, mihin korostus viimeksi jäi — tämä on
    // ainoa tapa todentaa käyttäytymisen kautta, että ArrowUp todella
    // poisti korostuksen sen sijaan, että se olisi vain jäänyt paikoilleen.
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
    await expect(getField(page).getByText('Ei hakutuloksia')).toBeVisible()
    await getCombobox(page).press('ArrowDown')
    await getCombobox(page).press('ArrowUp')
    await getCombobox(page).press('Home')
    await getCombobox(page).press('End')
    await getCombobox(page).press('Enter')
    await expect(getCombobox(page)).toHaveValue('ei osumaa xyz')
    await expect(getField(page).getByText('Ei hakutuloksia')).toBeVisible()
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
    await expect(getCombobox(page)).toHaveValue('')
    await expect(getListbox(page)).toBeHidden()
    await expect(getClearButton(page)).toHaveCount(0)
  })

  test('napautus avaa valikon ja vaihtoehdot ovat näkyvissä', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    await expect(getListbox(page)).toBeVisible()
    await expect(getOptions(page)).toHaveCount(OPTION_COUNT)
  })

  test('kentän oman labelin napautus ei sulje auki olevaa valikkoa mobiilissa', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    await expect(getListbox(page)).toBeVisible()
    await getField(page).locator('label').tap()
    await expect(getListbox(page)).toBeVisible()
  })

  test('pitkätkin vaihtoehdot pysyvät kokoruutunäkymän leveyden sisällä', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    const viewport = page.viewportSize()
    const fieldBox = await getField(page).boundingBox()
    const listboxBox = await getListbox(page).boundingBox()
    expect(fieldBox?.width).toBeLessThanOrEqual(viewport?.width ?? Infinity)
    expect(listboxBox?.width).toBeLessThanOrEqual(viewport?.width ?? Infinity)
  })

  test('valikon uudelleennapautus ei sulje sitä ja vaihtoehdon voi silti valita', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    await expect(getListbox(page)).toBeVisible()
    await getCombobox(page).tap()
    await expect(getListbox(page)).toBeVisible()
    await expect(getOptions(page)).toHaveCount(OPTION_COUNT)
    await getOption(page, 'Norja').tap()
    await expect(getCombobox(page)).toHaveValue('Norja')
    await expect(getListbox(page)).toBeHidden()
  })

  test('valikon voi avata uudelleen valinnan jälkeen', async ({ page }) => {
    await getCombobox(page).tap()
    await getOption(page, 'Norja').tap()

    await getCombobox(page).tap()
    await expect(getListbox(page)).toBeVisible()
    await expect(getOptions(page)).toHaveCount(OPTION_COUNT)
  })

  test('kirjoittaminen suodattaa vaihtoehdot myös mobiilissa', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    await getCombobox(page).fill('saar')
    await expect(getOptions(page)).toHaveCount(1)
    await expect(getOption(page, 'Färsaaret')).toBeVisible()
    await getCombobox(page).fill('')
    await expect(getOptions(page)).toHaveCount(OPTION_COUNT)
  })

  test('tyhjennysnappi näkyy valinnan jälkeen ja tyhjentää arvon', async ({
    page,
  }) => {
    await getCombobox(page).tap()
    await getOption(page, 'Norja').tap()
    await expect(getClearButton(page)).toBeVisible()
    await getClearButton(page).tap()
    await expect(getCombobox(page)).toHaveValue('')
    await expect(getClearButton(page)).toBeHidden()
  })

  test('kokoruutuvalikon ollessa auki touchmove ei vieritä/panoroi sivua muualla, mutta listan sisällä se sallitaan', async ({
    page,
  }) => {
    // Regressiotesti: virtuaalinäppäimistön avautuminen voi saada mobiili-
    // selaimen "panoroimaan" koko sivua, ellei touchmovea ole estetty.
    expect(await dispatchTouchmove(page.locator('body'))).toBe(false)

    await getCombobox(page).tap()
    await expect(getListbox(page)).toBeVisible()

    const label = page.getByText(DROPDOWN_LABEL, { exact: true })

    const banner = page.locator('.application__banner-container')

    expect(await dispatchTouchmove(page.locator('body'))).toBe(true)
    expect(await dispatchTouchmove(label)).toBe(true)
    expect(await dispatchTouchmove(banner)).toBe(true)
    expect(await dispatchTouchmove(getOptions(page).first())).toBe(false)
  })
})
