import { test, expect, Page } from '@playwright/test'
import { unsafeFoldOption } from '../playwright-utils'
import {
  kirjauduVirkailijanNakymaan,
  lisaaLomake,
  poistaLomake,
  teeJaOdotaLomakkeenTallennusta,
} from '../playwright-ataru-utils'

const toolbarButtonText = 'Tutkintotiedot Koski-Palvelusta'
let lomakkeenAvain: string
let lomakkeenId: number
let page: Page

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage()

  await kirjauduVirkailijanNakymaan(page, 'SUPERUSER')

  const lomake = await lisaaLomake(page)

  lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
  lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)

  await teeJaOdotaLomakkeenTallennusta(page, lomakkeenId, async () => {
    const nameInput = page.getByTestId('form-name-input')
    await nameInput.fill('Testilomake')
  })
})

test.afterAll(async ({ request }) => {
  await poistaLomake(request, lomakkeenAvain)
  await page.close()
})

test.describe('Koski-tutkinnot -moduuli', (): void => {
  test('Moduuli piilotettu jos ei hakeminen tunnistautuneena ole sallittu', async () => {
    const koskiModuuli = page.getByTestId('tutkinnot-wrapper')
    const salliHakeminenTunnistautuneenaCheckbox = page.getByTestId(
      'toggle-allow-hakeminen-tunnistautuneena'
    )
    await expect(salliHakeminenTunnistautuneenaCheckbox).not.toBeChecked()
    await expect(koskiModuuli).toBeHidden()
    const valikko = page.getByTestId('component-toolbar')
    await valikko.dispatchEvent('mouseover')
    const lisaysLinkki = valikko.getByText(toolbarButtonText)
    await expect(lisaysLinkki).toBeHidden()
  })

  test('Lisää moduuli lomakkeelle', async () => {
    const koskiModuuli = page.getByTestId('tutkinnot-wrapper')
    const salliHakeminenTunnistautuneenaCheckbox = page.getByTestId(
      'toggle-allow-hakeminen-tunnistautuneena'
    )
    await expect(salliHakeminenTunnistautuneenaCheckbox).not.toBeChecked()
    await salliHakeminenTunnistautuneenaCheckbox.click()

    await expect(koskiModuuli).toBeHidden()
    const valikko = page.getByTestId('component-toolbar')
    await valikko.dispatchEvent('mouseover')
    let lisaysLinkki = valikko.getByText(toolbarButtonText)
    await lisaysLinkki.click()
    await expect(koskiModuuli).toBeVisible()
    lisaysLinkki = valikko.getByText(toolbarButtonText)
    await expect(lisaysLinkki).toBeHidden()

    // @TODO Otetaan käyttöön siinä vaiheessa kun opintosuoritukset lisätään editoriin
    /*
    const completedStudiesCheckbox = page.getByTestId(
      'completed-studies-question-id'
    )
    await expect(completedStudiesCheckbox).not.toBeChecked()
    let putRequestPromise = page.waitForRequest((req) => req.method() === 'PUT')
    await completedStudiesCheckbox.click()
    let putRequest = await putRequestPromise
    await expect(completedStudiesCheckbox).toBeChecked()
    let updatedProperties = JSON.parse(putRequest.postData() || '')[0][
      'new-form'
    ].properties
    expect(
      updatedProperties['tutkinto-properties']['show-completed-studies']
    ).toBe(true)
     */

    const perusopetusCheckbox = page.getByRole('checkbox', {
      name: 'Perusopetus',
    })
    const tohtoritutkinnotCheckbox = page.getByRole('checkbox', {
      name: 'Tohtoritutkinnot',
    })
    await expect(perusopetusCheckbox).not.toBeChecked()
    await expect(tohtoritutkinnotCheckbox).not.toBeChecked()
    await perusopetusCheckbox.click()
    let putRequestPromise = page.waitForRequest((req) => req.method() === 'PUT')
    await tohtoritutkinnotCheckbox.click()
    let putRequest = await putRequestPromise
    await expect(perusopetusCheckbox).toBeChecked()
    await expect(tohtoritutkinnotCheckbox).toBeChecked()
    let updatedProperties = JSON.parse(putRequest.postData() || '')[0]['new-form']
      .properties
    expect(
      updatedProperties['tutkinto-properties']['selected-option-ids']
    ).toStrictEqual(['perusopetus', 'itse-syotetty', 'tohtori'])
  })

  test('Poista moduuli lomakkeelta', async () => {
    let koskiModuuli = page.getByTestId('tutkinnot-wrapper')
    const valikko = page.getByTestId('component-toolbar')
    await expect(koskiModuuli).toBeVisible()

    const removeButton = page.getByTestId(
      'tutkinnot-header-remove-component-button'
    )
    const confirmButton = page.getByTestId(
      'tutkinnot-header-remove-component-button-confirm'
    )
    const putRequestPromise = page.waitForRequest(
      (req) => req.method() === 'PUT'
    )
    await removeButton.click()
    await confirmButton.click()
    const putRequest = await putRequestPromise
    const updatedProperties = JSON.parse(putRequest.postData() || '')[0][
      'new-form'
    ].properties
    koskiModuuli = page.getByTestId('tutkinnot-wrapper')
    await expect(koskiModuuli).toBeHidden()
    expect(updatedProperties['tutkinto-properties']).toBe(undefined)

    await valikko.dispatchEvent('mouseover')
    const lisaysLinkki = valikko.getByText(toolbarButtonText)
    await expect(lisaysLinkki).toBeVisible()
  })
})
