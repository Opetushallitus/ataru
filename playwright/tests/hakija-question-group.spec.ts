import { test, expect, Page } from '@playwright/test'
import { unsafeFoldOption } from '../playwright-utils'
import {
  fillAndSubmitQuestionGroupApplication,
  injectQuestionGroupFormData,
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
} from '../playwright-ataru-utils'

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

  await injectQuestionGroupFormData(page, lomakkeenId, formName)
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test('Hakija täyttää kysymysryhmällisen lomakkeen ja lähettää hakemuksen', async () => {
  await fillAndSubmitQuestionGroupApplication(page, lomakkeenAvain, formName)

  await expect(page.locator('.application__sent-placeholder-text')).toHaveText(
    'Hakemus lähetetty'
  )
  await expect(page.locator('.application-feedback-form')).toBeVisible()

  await page.getByTestId('close-feedback-form-button').click()
  await expect(page.locator('.application-feedback-form')).toBeHidden()

  const readonlyAnswer = page.locator('.application__text-field-paragraph')
  const adjacentAnswer = page.locator('.application__readonly-adjacent-cell')

  await expect(readonlyAnswer.nth(0)).toHaveText('Etunimi Tokanimi')
  await expect(readonlyAnswer.nth(1)).toHaveText('Etunimi')
  await expect(readonlyAnswer.nth(2)).toHaveText('Sukunimi')
  await expect(readonlyAnswer.nth(3)).toHaveText('Suomi')
  await expect(readonlyAnswer.nth(4)).toHaveText('020202A0202')
  await expect(readonlyAnswer.nth(5)).toHaveText('test@example.com')
  await expect(readonlyAnswer.nth(6)).toHaveText('050123')
  await expect(readonlyAnswer.nth(7)).toHaveText('Suomi')
  await expect(readonlyAnswer.nth(8)).toHaveText('Katutie 12 B')
  await expect(readonlyAnswer.nth(9)).toHaveText('40100')
  await expect(readonlyAnswer.nth(10)).toHaveText('JYVÄSKYLÄ')
  await expect(readonlyAnswer.nth(11)).toHaveText('Jyväskylä')
  await expect(readonlyAnswer.nth(12)).toHaveText('suomi')
  await expect(readonlyAnswer.nth(13)).toHaveText('Päätaso: B')

  await expect(readonlyAnswer.nth(14)).toHaveText('Pudotusvalikko: A')
  await expect(readonlyAnswer.nth(15)).toHaveText(
    'Painikkeet, yksi valittavissa: A'
  )
  await expect(readonlyAnswer.nth(16)).toHaveText(
    'Lista, monta valittavissa: A'
  )
  await expect(readonlyAnswer.nth(17)).toHaveText(
    'Lista, monta valittavissa: B'
  )
  await expect(readonlyAnswer.nth(18)).toHaveText(
    'Tekstikenttä, yksi vastaus: A'
  )
  await expect(readonlyAnswer.nth(19)).toHaveText(
    'Tekstikenttä, monta vastausta: A'
  )
  await expect(readonlyAnswer.nth(20)).toHaveText(
    'Tekstikenttä, monta vastausta: B'
  )
  await expect(readonlyAnswer.nth(21)).toHaveText('Tekstialue: AAAAA')

  await expect(readonlyAnswer.nth(22)).toHaveText('Pudotusvalikko: B')
  await expect(readonlyAnswer.nth(23)).toHaveText(
    'Painikkeet, yksi valittavissa: B'
  )
  await expect(readonlyAnswer.nth(24)).toHaveText(
    'Lista, monta valittavissa: B'
  )
  await expect(readonlyAnswer.nth(25)).toHaveText(
    'Tekstikenttä, yksi vastaus: B'
  )
  await expect(readonlyAnswer.nth(26)).toHaveText(
    'Tekstikenttä, monta vastausta: C'
  )
  await expect(readonlyAnswer.nth(27)).toHaveText(
    'Tekstikenttä, monta vastausta: D'
  )
  await expect(readonlyAnswer.nth(28)).toHaveText('Tekstialue: BBBBB')

  await expect(adjacentAnswer.nth(0)).toHaveText(
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus A'
  )
  await expect(adjacentAnswer.nth(1)).toHaveText(
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus B'
  )
  await expect(adjacentAnswer.nth(2)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus A1'
  )
  await expect(adjacentAnswer.nth(3)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus B1'
  )
  await expect(adjacentAnswer.nth(4)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus A2'
  )
  await expect(adjacentAnswer.nth(5)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus B2'
  )
  await expect(adjacentAnswer.nth(6)).toHaveText(
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus C'
  )
  await expect(adjacentAnswer.nth(7)).toHaveText(
    'Vierekkäiset tekstikentät, yksi vastaus: vastaus D'
  )
  await expect(adjacentAnswer.nth(8)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus C1'
  )
  await expect(adjacentAnswer.nth(9)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus D1'
  )
  await expect(adjacentAnswer.nth(10)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus C2'
  )
  await expect(adjacentAnswer.nth(11)).toHaveText(
    'Vierekkäiset tekstikentät, monta vastausta: vastaus D2'
  )
})
