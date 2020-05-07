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
    lomakkeenId: httpPaluusanomat.lomakkeenLahetyksenPaluusanoma.haeLomakkeenId(
      response
    ),
  }))
}

export const haeLomakkeenNimenSyote = () =>
  cy.get('[data-test-id=form-name-input]:visible')

export const asetaLomakkeenNimi = (name: string, lomakkeenId: number) =>
  odota.odotaHttpPyyntoa(
    () =>
      cy.route(
        'PUT',
        reitit.virkailija.haeLomakkeenMuuttamisenOsoite(lomakkeenId)
      ),
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

export const komponentinLisays = {
  hover: () =>
    cy.get('[data-test-id=component-toolbar]:visible').trigger('mouseover'),

  haeLisaaArvosanatLinkki: () =>
    cy.get('[data-test-id=component-toolbar-arvosanat]:visible'),

  lisaaArvosanat: (formId: number) =>
    odota.odotaHttpPyyntoa(
      () =>
        cy.route(
          'PUT',
          reitit.virkailija.haeLomakkeenMuuttamisenOsoite(formId)
        ),
      () => {
        komponentinLisays.hover()
        return komponentinLisays.haeLisaaArvosanatLinkki().click()
      }
    ),
}

export const arvosanat = {
  haeOsionNimi: () =>
    cy.get('[data-test-id=arvosanat-moduuli-header-label]:visible'),

  haePoistaOsioNappi: () =>
    cy.get(
      '[data-test-id=arvosanat-moduuli-header-remove-component-button]:visible'
    ),

  haeVahvistaPoistaOsioNappi: () =>
    cy.get(
      '[data-test-id=arvosanat-moduuli-header-remove-component-button-confirm]:visible'
    ),

  poistaArvosanat: (lomakkeenId: number) =>
    arvosanat
      .haePoistaOsioNappi()
      .click()
      .then(() =>
        odota.odotaHttpPyyntoa(
          () =>
            cy.route(
              'PUT',
              reitit.virkailija.haeLomakkeenMuuttamisenOsoite(lomakkeenId)
            ),
          () => arvosanat.haeVahvistaPoistaOsioNappi().click()
        )
      ),
}
