import { test, expect, Page } from '@playwright/test'

import {
  fillAndSubmitQuestionGroupApplication,
  injectQuestionGroupFormData,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
} from '../playwright-ataru-utils'
import { unsafeFoldOption, waitForResponse } from '../playwright-utils'

test.describe.configure({ mode: 'serial' })

let page: Page
let lomakkeenTunnisteet: { lomakkeenAvain: string; lomakkeenId: number }

const formName = `Kysymysryhmä: testilomake (PW) ${Date.now()}`

test.describe('Virkailijan hakemuksen käsittely lomakkeella, jossa on kysymysryhmä', () => {
  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage()

    await Promise.all([
      waitForResponse(page, 'GET', (url) =>
        url.includes('/lomake-editori/api/forms')
      ),
      kirjauduVirkailijanNakymaan(page),
    ])

    const lomake = await lisaaLomake(page)

    lomakkeenTunnisteet = {
      lomakkeenAvain: unsafeFoldOption(lomake.lomakkeenAvain),
      lomakkeenId: unsafeFoldOption(lomake.lomakkeenId),
    }

    await injectQuestionGroupFormData(
      page,
      lomakkeenTunnisteet.lomakkeenId,
      formName
    )
    await fillAndSubmitQuestionGroupApplication(
      page,
      lomakkeenTunnisteet.lomakkeenAvain,
      formName
    )
    await expect(page.getByText(/Hakemus/)).toBeVisible()

    await page.goto(
      `/lomake-editori/applications/${lomakkeenTunnisteet.lomakkeenAvain}?ensisijaisesti=false`
    )
    await page.getByTestId('show-results').click()
    await expect(page.locator('.application__wrapper-heading h2')).toBeVisible()
  })

  test.afterAll(async ({ request }) => {
    if (lomakkeenTunnisteet?.lomakkeenAvain) {
      await poistaLomake(request, lomakkeenTunnisteet.lomakkeenAvain)
    }
    await page.close()
  })

  test('näyttää automaattisesti ainoan lomakkeeseen kuuluvan hakemuksen', async () => {
    const readonlyAnswers = await page
      .locator('.application__text-field-paragraph')
      .allTextContents()

    // Keep person-info expectations strict in order and value.
    const expectedPersonInfoPrefix = [
      'Etunimi Tokanimi',
      'Etunimi',
      'Sukunimi',
      'Suomi',
      '020202A0202',
      '02.02.2002',
      'nainen',
      'test@example.com',
      '050123',
      'Suomi',
      'Katutie 12 B',
      '40100',
    ]

    expect(readonlyAnswers.slice(0, expectedPersonInfoPrefix.length)).toEqual(
      expectedPersonInfoPrefix
    )

    const expectedQuestionGroupValues = [
      'Pudotusvalikko: A',
      'Painikkeet, yksi valittavissa: A',
      'Lista, monta valittavissa: A',
      'Lista, monta valittavissa: B',
      'Tekstikenttä, yksi vastaus: A',
      'Tekstikenttä, monta vastausta: A',
      'Tekstikenttä, monta vastausta: B',
      'Pudotusvalikko: B',
      'Painikkeet, yksi valittavissa: B',
      'Lista, monta valittavissa: B',
      'Tekstikenttä, yksi vastaus: B',
      'Tekstikenttä, monta vastausta: C',
      'Tekstikenttä, monta vastausta: D',
    ]

    expect(readonlyAnswers.some((value) => /Päätaso: B/.test(value))).toBe(true)
    for (const value of expectedQuestionGroupValues) {
      expect(readonlyAnswers).toContain(value)
    }

    expect(readonlyAnswers).toEqual(
      expect.arrayContaining([
        'Etunimi Tokanimi',
        'Etunimi',
        'Sukunimi',
        'Suomi',
        '020202A0202',
        '02.02.2002',
        'nainen',
        'test@example.com',
        '050123',
        'Katutie 12 B',
        '40100',
      ])
    )

    // Locale/casing can vary by environment, but both values should be present.
    expect(readonlyAnswers.some((value) => /JYVÄSKYLÄ/.test(value))).toBe(true)
    expect(readonlyAnswers.some((value) => /Jyväskylä/.test(value))).toBe(true)
    expect(readonlyAnswers.some((value) => /suomi/i.test(value))).toBe(true)

    const adjacentAnswerAt = async (
      formFieldIndex: number,
      answerIndex: number
    ) =>
      page
        .locator('.application__readonly-adjacent')
        .nth(formFieldIndex)
        .locator('td')
        .nth(answerIndex)
        .innerText()

    await expect(async () => {
      expect(await adjacentAnswerAt(0, 0)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus A'
      )
      expect(await adjacentAnswerAt(0, 1)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus B'
      )
      expect(await adjacentAnswerAt(1, 0)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus A1'
      )
      expect(await adjacentAnswerAt(1, 1)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus B1'
      )
      expect(await adjacentAnswerAt(1, 2)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus A2'
      )
      expect(await adjacentAnswerAt(1, 3)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus B2'
      )
      expect(await adjacentAnswerAt(2, 0)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus C'
      )
      expect(await adjacentAnswerAt(2, 1)).toBe(
        'Vierekkäiset tekstikentät, yksi vastaus: vastaus D'
      )
      expect(await adjacentAnswerAt(3, 0)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus C1'
      )
      expect(await adjacentAnswerAt(3, 1)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus D1'
      )
      expect(await adjacentAnswerAt(3, 2)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus C2'
      )
      expect(await adjacentAnswerAt(3, 3)).toBe(
        'Vierekkäiset tekstikentät, monta vastausta: vastaus D2'
      )
    }).toPass()
  })

  test('näyttää täydennyspyynnön lomakkeen käyttäjälle', async () => {
    await page
      .locator(
        '.application-handling__review-state-container-processing-state .application-handling__review-state-row--selected'
      )
      .click()

    const informationRequestStateButton = page
      .locator('.application-handling__review-state-row')
      .filter({ hasText: /Täydennyspyyntö/ })
    await expect(informationRequestStateButton).toBeVisible()
    await informationRequestStateButton.click()

    const informationRequestContainer = page.locator(
      '.application-handling__information-request-container'
    )
    const submitInformationRequestButton = informationRequestContainer.locator(
      '.application-handling__send-information-request-button'
    )

    await expect(submitInformationRequestButton).toBeDisabled()

    await informationRequestContainer
      .locator('.application-handling__information-request-text-input')
      .fill('Täydennyspyyntö: otsikko')

    await expect(submitInformationRequestButton).toBeDisabled()

    await informationRequestContainer
      .locator('.application-handling__information-request-message-area')
      .fill('Täydennyspyyntö: viesti')

    await expect(submitInformationRequestButton).toBeEnabled()

    await Promise.all([
      waitForResponse(page, 'POST', (url) =>
        url.includes('/lomake-editori/api/applications/information-request')
      ),
      submitInformationRequestButton.click(),
    ])

    await expect(
      page.locator('.application-handling__information-request-submitted-text')
    ).toBeVisible()

    await expect(
      page.locator(
        '.application-handling__information-request-show-container-link'
      )
    ).toBeVisible()
  })
})
