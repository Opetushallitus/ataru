import * as httpPaluusanomat from './httpPaluusanomat'
import * as reitit from './reitit'
import * as odota from './odota'
import * as tekstikentta from './tekstikentta'
import { syotaTeksti } from './tekstikentta'

import Chainable = Cypress.Chainable
import WaitXHR = Cypress.WaitXHR

export const haeLomakkeenLisaysNappi = () =>
  cy.get('[data-test-id=add-form-button]:visible')

export const lisaaLomake = () => {
  cy.server()
  cy.route('POST', reitit.virkailija.haeUudenLomakkeenLahettamisenOsoite()).as(
    'postForms'
  )
  haeLomakkeenLisaysNappi().click()
  return cy.wait('@postForms').then((response) => ({
    lomakkeenAvain: httpPaluusanomat.lomakkeenLahetyksenPaluusanoma.haeLomakkeenAvain(
      response
    ),
    lomakkeenId: httpPaluusanomat.lomakkeenLahetyksenPaluusanoma.haeLomakkeenId(
      response
    ),
  }))
}

export const haeLomakkeenNimenSyote = () =>
  cy.get('[data-test-id=form-name-input]:visible')

// eslint-disable-next-line prefer-arrow/prefer-arrow-functions
export function teeJaodotaLomakkeenTallennusta<T>(
  lomakkeenId: number,
  muokkaaLomaketta: () => Chainable<T>
): Chainable<{ result: T; xhr: WaitXHR }> {
  return odota.odotaHttpPyyntoa<T>(
    () =>
      cy.route(
        'PUT',
        reitit.virkailija.haeLomakkeenMuuttamisenOsoite(lomakkeenId)
      ),
    muokkaaLomaketta
  )
}

export const asetaLomakkeenNimi = (name: string, lomakkeenId: number) =>
  teeJaodotaLomakkeenTallennusta(lomakkeenId, () =>
    syotaTeksti(haeLomakkeenNimenSyote(), name)
  )

const koodistonValitsin = () =>
  cy.get('[data-test-id=editor-form__select-koodisto-dropdown]:visible')

export const haeLomakkeenEsikatseluLinkki = () =>
  cy.get('[data-test-id=application-preview-link-fi]:visible')

export const valitseKoodisto = (koodistonNimi: string) =>
  koodistonValitsin().select(koodistonNimi)

export const naytaVastausvaihtoehdot = () =>
  cy
    .get('[data-test-id=editor-form__show_koodisto-values__link]:visible')
    .click()

export const vastausvaihtoehdot = () =>
  cy
    .get('[data-test-id=editor-form__multi-options-container')
    .find('[data-test-id=editor-form__koodisto-field]:visible')

export const hakukohteet = {
  haeOtsikko: () => cy.get('[data-test-id=hakukohteet-header-label]:visible'),
}

export const henkilotiedot = {
  haeOtsikko: () =>
    cy.get('[data-test-id=henkilotietomoduuli-header-label]:visible'),

  haeHenkilotietojenValintaKomponentti: () =>
    cy.get('[data-test-id=henkilotietomoduuli-select]:visible'),

  haeKaytettavatHenkilotietoKentat: () =>
    cy.get('[data-test-id=henkilotietomoduuli-fields-label]:visible'),

  valitseHenkilotietolomakkeenKentat: (
    kenttienKuvaus: string,
    lomakkeenId: number
  ) =>
    teeJaodotaLomakkeenTallennusta(lomakkeenId, () =>
      henkilotiedot
        .haeHenkilotietojenValintaKomponentti()
        .select(kenttienKuvaus)
    ),
}

export const komponentinLisays = {
  hover: () =>
    cy.get('[data-test-id=component-toolbar]:visible').trigger('mouseover'),

  haeLisaaArvosanatLinkki: () =>
    cy.get('[data-test-id=component-toolbar-arvosanat]:visible'),

  haeElementinLisaysLinkki: (elementinTeksti: string) =>
    cy
      .get('[data-test-id=component-toolbar]:visible')
      .contains(elementinTeksti),

  lisaaArvosanat: (formId: number) => {
    return teeJaodotaLomakkeenTallennusta(formId, () => {
      komponentinLisays.hover()
      return komponentinLisays.haeLisaaArvosanatLinkki().click()
    })
  },
  lisaaElementti: (formId: number, elementinTeksti: string) =>
    teeJaodotaLomakkeenTallennusta(formId, () => {
      komponentinLisays.hover()
      return komponentinLisays.haeElementinLisaysLinkki(elementinTeksti).click()
    }),
}

export const painikeYksiValittavissa = {
  haeKysymysTeksti: () =>
    cy
      .get(
        '[data-test-id=editor-form__singleChoice-component-question-wrapper]:visible'
      )
      .find('input'),
  haeElementinOtsikko: () =>
    cy.get(
      '[data-test-id=editor-form__singleChoice-component-main-label]:visible'
    ),
  syotaKysymysTeksti: (teksti: string) => {
    return tekstikentta.syotaTeksti(
      painikeYksiValittavissa.haeKysymysTeksti(),
      teksti
    )
  },
}
