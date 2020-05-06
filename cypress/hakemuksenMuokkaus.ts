import * as httpPaluusanomat from './httpPaluusanomat'
import * as reitit from './reitit'
import * as asetukset from './asetukset'
import * as odota from './odota'

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
    formId: httpPaluusanomat.lomakkeenLahetyksenPaluusanoma.haeLomakkeenId(
      response
    ),
  }))
}

export const haeLomakkeenNimenSyote = () =>
  cy.get('[data-test-id=form-name-input]:visible')

export const asetaLomakkeenNimi = (name: string, formId: number) =>
  odota.odotaHttpPyyntoa(
    () =>
      cy.route('PUT', reitit.virkailija.haeLomakkeenMuuttamisenOsoite(formId)),
    () =>
      haeLomakkeenNimenSyote()
        .clear()
        .type(name, { delay: asetukset.tekstikentanSyotonViive })
  )

export const haeLomakkeenEsikatseluLinkki = () =>
  cy.get('[data-test-id=application-preview-link-fi]:visible')

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
    odota.odotaHttpPyyntoa(
      () =>
        cy.route(
          'PUT',
          reitit.virkailija.haeLomakkeenMuuttamisenOsoite(lomakkeenId)
        ),
      () =>
        henkilotiedot
          .haeHenkilotietojenValintaKomponentti()
          .select(kenttienKuvaus)
    ),
}
