import * as httpPaluusanomat from './httpPaluusanomat'
import * as reitit from './reitit'
import * as asetukset from './asetukset'
import * as odota from './odota'
import { syotaTeksti } from './apu'

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

const siirryMuokkaamaanLomaketta = (formId: number) => () =>
  cy.route('PUT', reitit.virkailija.haeLomakkeenMuuttamisenOsoite(formId))

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
    .find('[data-test-id=editor-form__koodisto-field]')

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

  haeElementinLisaysLinkki: (elementinTeksti: string) =>
    cy
      .get('[data-test-id=component-toolbar]:visible')
      .contains(elementinTeksti),

  lisaaArvosanat: (formId: number) => {
    return odota.odotaHttpPyyntoa(siirryMuokkaamaanLomaketta(formId), () => {
      komponentinLisays.hover()
      return komponentinLisays.haeLisaaArvosanatLinkki().click()
    })
  },
  lisaaElementti: (formId: number, elementinTeksti: string) =>
    odota.odotaHttpPyyntoa(siirryMuokkaamaanLomaketta(formId), () => {
      komponentinLisays.hover()
      return komponentinLisays.haeElementinLisaysLinkki(elementinTeksti).click()
    }),
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

  haeLeikkaaOsioNappi: () =>
    cy.get(
      '[data-test-id=arvosanat-moduuli-header-cut-component-button]:visible'
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
    return syotaTeksti(painikeYksiValittavissa.haeKysymysTeksti(), teksti)
  },
}
