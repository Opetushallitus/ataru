import { test, expect, Page } from '@playwright/test'

import {
  expectUusiLomakeValid,
  getHakemuksenLahettamisenOsoite,
  getHakijanNakymanOsoite,
  getLomakkeenHaunOsoite,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  taytaHenkilotietomoduuli,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

const TESTILOMAKKEEN_NIMI = 'Testilomake'

const sisaltaaPeruskoulunArvosanatosion = (value: unknown): boolean => {
  if (Array.isArray(value)) {
    return value.some(sisaltaaPeruskoulunArvosanatosion)
  }

  if (value && typeof value === 'object') {
    const objectValue = value as Record<string, unknown>

    return (
      objectValue.id === 'arvosanat-peruskoulu' ||
      objectValue.module === 'arvosanat-peruskoulu' ||
      objectValue['section-name'] === 'arvosanat-peruskoulu' ||
      Object.values(objectValue).some(sisaltaaPeruskoulunArvosanatosion)
    )
  }

  return false
}

const avaaKomponenttivalikko = async (page: Page) => {
  const toolbar = page.getByTestId('component-toolbar')
  await expect(toolbar).toBeVisible()
  await toolbar.hover()
}

const asetaPudotusvalikonArvo = async (
  page: Page,
  dataTestIdPrefix: string,
  value: string
) => {
  await page.getByTestId(`${dataTestIdPrefix}-button`).click()
  await page.getByTestId(`${dataTestIdPrefix}-option-${value}`).click()
}

const asetaOppiaineenArvosanat = async (
  page: Page,
  {
    oppiaine,
    arvosana,
    oppimaara,
    index,
  }: {
    oppiaine: string
    arvosana: string
    oppimaara?: string
    index: number
  }
) => {
  await asetaPudotusvalikonArvo(
    page,
    `oppiaineen-arvosana-${oppiaine}-arvosana-${index}`,
    `arvosana-${oppiaine}-${arvosana}`
  )

  if (oppimaara) {
    await asetaPudotusvalikonArvo(
      page,
      `oppiaineen-arvosana-${oppiaine}-oppimaara-${index}`,
      oppimaara
    )
  }
}

const lisaaValinnaisaine = async (page: Page, oppiaine: string) => {
  await page
    .locator(
      `a[data-test-id="oppiaineen-arvosana-${oppiaine}-lisaa-valinnaisaine-linkki-0-lisaa"]`
    )
    .click()
}

const lisaaValinnainenKieli = async (
  page: Page,
  {
    oppiaine,
    oppimaara,
    arvosana,
    index,
  }: {
    oppiaine: string
    oppimaara: string
    arvosana: string
    index: number
  }
) => {
  await asetaPudotusvalikonArvo(
    page,
    'valinnaiset-kielet-oppiaine-dropdown',
    `oppiaine-valinnainen-kieli-${oppiaine}`
  )
  await asetaPudotusvalikonArvo(
    page,
    `valinnaiset-kielet-oppiaine-oppimaara-${index}`,
    oppimaara
  )
  await asetaPudotusvalikonArvo(
    page,
    `valinnaiset-kielet-oppiaine-arvosana-${index}`,
    `arvosana-valinnainen-kieli-${arvosana}`
  )
}

const lisaaPeruskoulunArvosanaosio = async (
  page: Page,
  lomakkeenId: number
) => {
  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await avaaKomponenttivalikko(page)

    const arvosanatLinkki = page.getByTestId('component-toolbar-arvosanat')
    await expect(arvosanatLinkki).toHaveText('Arvosanat (peruskoulu)')
    await arvosanatLinkki.click()
  })

  await expect(page.getByTestId('arvosanat-moduuli-header-label')).toHaveText(
    'Arvosanat (peruskoulu)'
  )
  await expect(
    page.getByTestId('arvosanat-moduuli-header-remove-component-button')
  ).toBeEnabled()
  await expect(
    page.getByTestId('arvosanat-moduuli-header-cut-component-button')
  ).toBeEnabled()
}

const odotaKunnesHakijanLomakeSisaltaaPeruskoulunArvosanatosion = async (
  page: Page,
  lomakkeenAvain: string
) => {
  await expect
    .poll(
      async () => {
        const response = await page.evaluate(async (osoite) => {
          const result = await fetch(osoite)
          return result.json()
        }, getLomakkeenHaunOsoite(lomakkeenAvain))

        return sisaltaaPeruskoulunArvosanatosion(response)
      },
      {
        message:
          'hakijan lomake ei sisällä arvosanaosiota vielä persisted payloadissa',
        timeout: 30_000,
        intervals: [500],
      }
    )
    .toBe(true)
}

const luoTestilomake = async (page: Page) => {
  await kirjauduVirkailijanNakymaan(page)
  await expect(page.getByTestId('add-form-button')).toBeVisible()

  const lomake = await lisaaLomake(page)
  const lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
  const lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    await page.getByTestId('form-name-input').fill(TESTILOMAKKEEN_NIMI)
  })

  await expectUusiLomakeValid(page, lomakkeenAvain, TESTILOMAKKEEN_NIMI)

  return { lomakkeenId, lomakkeenAvain }
}

const tarkistaLukunakymanOppiaine = async (
  page: Page,
  {
    oppiaine,
    arvosana,
    index,
    oppimaara,
  }: {
    oppiaine: string
    arvosana: string
    index: number
    oppimaara?: string
  }
) => {
  await expect(
    page.getByTestId(
      `oppiaineen-arvosana-readonly-${oppiaine}-arvosana-${index}`
    )
  ).toHaveText(arvosana)

  if (oppimaara) {
    await expect(
      page.getByTestId(
        `oppiaineen-arvosana-readonly-${oppiaine}-oppimaara-${index}`
      )
    ).toHaveText(oppimaara)
  }
}

const tarkistaLukunakymanValinnainenKieli = async (
  page: Page,
  {
    oppimaara,
    arvosana,
    index,
  }: {
    oppimaara: string
    arvosana: string
    index: number
  }
) => {
  await expect(
    page.getByTestId(
      `valinnaiset-kielet-readonly-oppiaineen-arvosanat-valinnaiset-kielet-oppimaara-${index}`
    )
  ).toHaveText(oppimaara)
  await expect(
    page.getByTestId(
      `valinnaiset-kielet-readonly-oppiaineen-arvosanat-valinnaiset-kielet-arvosana-${index}`
    )
  ).toHaveText(arvosana)
}

const siivoaLomakeJaSuljeSivu = async (
  page: Page,
  request: Parameters<typeof poistaLomake>[0],
  lomakkeenAvain?: string
) => {
  if (lomakkeenAvain) {
    await poistaLomake(request, lomakkeenAvain)
  }

  await page.close()
}

test.describe('Peruskoulun arvosanat -osio', () => {
  test('Hakijan polku', async ({ browser, request }) => {
    test.setTimeout(180_000)
    test.fail(
      true,
      'Arvosanaosio ei persistoidu hakijan payloadiin nykyisessä toteutuksessa'
    )

    const page = await browser.newPage()
    let lomakkeenAvain: string | undefined

    try {
      const lomake = await luoTestilomake(page)
      lomakkeenAvain = lomake.lomakkeenAvain

      await lisaaPeruskoulunArvosanaosio(page, lomake.lomakkeenId)
      await odotaKunnesHakijanLomakeSisaltaaPeruskoulunArvosanatosion(
        page,
        lomake.lomakkeenAvain
      )

      await Promise.all([
        page.goto(getHakijanNakymanOsoite(lomake.lomakkeenAvain)),
        waitForResponse(page, 'GET', (url) =>
          url.includes(getLomakkeenHaunOsoite(lomake.lomakkeenAvain))
        ),
      ])

      await expect(page.getByTestId('application-header-label')).toHaveText(
        TESTILOMAKKEEN_NIMI
      )

      await taytaHenkilotietomoduuli(page)
      await expect(page.getByTestId('postal-office-input')).toHaveValue(
        'HELSINKI'
      )

      const aidinkieli = page.getByTestId('language-input')
      await aidinkieli.selectOption('SV')
      await expect(aidinkieli).toHaveValue('SV')
      await expect(page.getByTestId('oppiaineen-arvosana-A2')).toBeVisible()
      await expect(page.getByTestId('oppiaineen-arvosana-B1')).toHaveCount(0)

      await aidinkieli.selectOption('FI')
      await expect(aidinkieli).toHaveValue('FI')
      await expect(page.getByTestId('oppiaineen-arvosana-B1')).toBeVisible()

      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'A',
        arvosana: '7',
        oppimaara: 'ruotsi-toisena-kielena',
        index: 0,
      })
      await lisaaValinnaisaine(page, 'A')
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'A',
        arvosana: '8',
        oppimaara: 'suomi-viittomakielisille',
        index: 1,
      })
      await lisaaValinnaisaine(page, 'A')
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'A',
        arvosana: 'ei-arvosanaa',
        oppimaara: 'suomi-saamenkielisille',
        index: 2,
      })
      await lisaaValinnaisaine(page, 'A')
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'A',
        arvosana: 'hyvaksytty',
        oppimaara: 'ruotsi-viittomakielisille',
        index: 3,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'A1',
        arvosana: 'osallistunut',
        oppimaara: 'FI',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'B1',
        arvosana: '9',
        oppimaara: 'SV',
        index: 0,
      })
      await lisaaValinnainenKieli(page, {
        oppiaine: 'a1',
        oppimaara: 'JA',
        arvosana: 'ei-arvosanaa',
        index: 0,
      })
      await lisaaValinnainenKieli(page, {
        oppiaine: 'a',
        oppimaara: 'muu-oppilaan-aidinkieli',
        arvosana: '6',
        index: 1,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'MA',
        arvosana: '10',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'BI',
        arvosana: '5',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'GE',
        arvosana: '6',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'FY',
        arvosana: '10',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'KE',
        arvosana: '8',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'TT',
        arvosana: '7',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'TY',
        arvosana: '5',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'HI',
        arvosana: '4',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'YH',
        arvosana: '10',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'MU',
        arvosana: '10',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'KU',
        arvosana: '8',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'KA',
        arvosana: '5',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'LI',
        arvosana: '9',
        index: 0,
      })
      await asetaOppiaineenArvosanat(page, {
        oppiaine: 'KO',
        arvosana: '6',
        index: 0,
      })

      await expect(
        page.locator(
          'span[data-test-id="oppiaineen-arvosana-A-lisaa-valinnaisaine-linkki-0-lisaa"]'
        )
      ).toBeVisible()

      await Promise.all([
        waitForResponse(page, 'POST', (url) =>
          url.includes(getHakemuksenLahettamisenOsoite())
        ),
        page.getByTestId('send-application-button').click(),
      ])
      await page.getByTestId('send-feedback-button').click()
      await page.getByTestId('close-feedback-form-button').click()

      await expect(page.getByTestId('application-header-label')).toHaveText(
        TESTILOMAKKEEN_NIMI
      )

      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'A',
        oppimaara: 'Ruotsi toisena kielenä',
        arvosana: '7',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'A',
        oppimaara: 'Suomi viittomakielisille',
        arvosana: '8',
        index: 1,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'A',
        oppimaara: 'Suomi saamenkielisille',
        arvosana: 'Ei arvosanaa',
        index: 2,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'A',
        oppimaara: 'Ruotsi viittomakielisille',
        arvosana: 'S (Hyväksytty)',
        index: 3,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'A1',
        arvosana: 'O (Osallistunut)',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'B1',
        arvosana: '9',
        index: 0,
      })
      await tarkistaLukunakymanValinnainenKieli(page, {
        oppimaara: 'japani',
        arvosana: 'Ei arvosanaa',
        index: 0,
      })
      await tarkistaLukunakymanValinnainenKieli(page, {
        oppimaara: 'Muu oppilaan äidinkieli',
        arvosana: '6',
        index: 1,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'MA',
        arvosana: '10',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'BI',
        arvosana: '5',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'GE',
        arvosana: '6',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'FY',
        arvosana: '10',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'KE',
        arvosana: '8',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'TT',
        arvosana: '7',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'TY',
        arvosana: '5',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'HI',
        arvosana: '4',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'YH',
        arvosana: '10',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'MU',
        arvosana: '10',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'KU',
        arvosana: '8',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'KA',
        arvosana: '5',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'LI',
        arvosana: '9',
        index: 0,
      })
      await tarkistaLukunakymanOppiaine(page, {
        oppiaine: 'KO',
        arvosana: '6',
        index: 0,
      })
    } finally {
      await siivoaLomakeJaSuljeSivu(page, request, lomakkeenAvain)
    }
  })

  test('Poistopolku', async ({ browser, request }) => {
    test.setTimeout(120_000)

    const page = await browser.newPage()
    let lomakkeenAvain: string | undefined

    try {
      const lomake = await luoTestilomake(page)
      lomakkeenAvain = lomake.lomakkeenAvain

      await lisaaPeruskoulunArvosanaosio(page, lomake.lomakkeenId)

      await page
        .getByTestId('arvosanat-moduuli-header-remove-component-button')
        .click()
      await page
        .getByTestId('arvosanat-moduuli-header-remove-component-button-confirm')
        .click()

      await expect(
        page.getByTestId('arvosanat-moduuli-header-label')
      ).toHaveCount(0)
    } finally {
      await siivoaLomakeJaSuljeSivu(page, request, lomakkeenAvain)
    }
  })
})
