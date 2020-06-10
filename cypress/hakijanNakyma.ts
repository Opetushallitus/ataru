import * as tekstikentta from './tekstikentta'
import * as dropdown from './dropdown'

import Chainable = Cypress.Chainable

interface OppiaineenArvosanat {
  oppiaine: string
  arvosana: string
  oppimaara?: string
  index: number
}

export const haeLomakkeenNimi = () =>
  cy.get('[data-test-id=application-header-label]:visible')

export const arvosanat = {
  asetaOppiaineenArvosanat: ({
    oppiaine,
    arvosana,
    oppimaara,
    index,
  }: OppiaineenArvosanat) =>
    dropdown
      .setDropdownValue(
        `oppiaineen-arvosana-${oppiaine}-arvosana-${index}`,
        `arvosana-${oppiaine}-${arvosana}`
      )
      .then((c) =>
        oppimaara
          ? dropdown.setDropdownValue(
              `oppiaineen-arvosana-${oppiaine}-oppimaara-${index}`,
              oppimaara
            )
          : c
      ),

  haeValinnaisaineLinkki: ({
    oppiaine,
    index,
    poisKaytosta,
  }: {
    oppiaine: string
    index: number
    poisKaytosta: boolean
  }) =>
    cy.get(
      `${
        poisKaytosta ? 'span' : 'a'
      }[data-test-id=oppiaineen-arvosana-${oppiaine}-lisaa-valinnaisaine-linkki-0-${
        index === 0 ? 'lisaa' : 'poista'
      }]`
    ),

  lisaaValinnaisaine: ({ oppiaine }: { oppiaine: string }) =>
    arvosanat
      .haeValinnaisaineLinkki({ oppiaine, index: 0, poisKaytosta: false })
      .click(),
}

const syota = <T>(
  elementti: Chainable<T>,
  teksti: string
): (() => Chainable<T>) => () => tekstikentta.syotaTeksti(elementti, teksti)

export const henkilotiedot = {
  haeEtunimiKentta: () => cy.get('#first-name'),
  haeSukunimiKentta: () => cy.get('#last-name'),
  haeHenkilotunnusKentta: () => cy.get('#ssn'),
  haeSahkopostiKentta: () => cy.get('#email'),
  haeSahkostinVarmistus: () => cy.get('#verify-email'),
  haeMatkapuhelinKentta: () => cy.get('#phone'),
  haeKatuosoiteKentta: () => cy.get('#address'),
  haePostinumeroKentta: () => cy.get('#postal-code'),
  haeKotikuntaKentta: () => cy.get('#home-town'),

  taytaTiedot: () => {
    return tekstikentta
      .syotaTeksti(henkilotiedot.haeEtunimiKentta(), 'Frank Zacharias')
      .then(syota(henkilotiedot.haeSukunimiKentta(), 'Testerberg'))
      .then(syota(henkilotiedot.haeHenkilotunnusKentta(), '160600A999C'))
      .then(syota(henkilotiedot.haeSahkopostiKentta(), 'f.t@example.com'))
      .then(syota(henkilotiedot.haeSahkostinVarmistus(), 'f.t@example.com'))
      .then(syota(henkilotiedot.haeMatkapuhelinKentta(), '0401234567'))
      .then(syota(henkilotiedot.haeKatuosoiteKentta(), 'Yliopistonkatu 4'))
      .then(syota(henkilotiedot.haePostinumeroKentta(), '00100'))
      .then(() => henkilotiedot.haeKotikuntaKentta().select('Forssa'))
  },
}

export const klikkaa = (elementinTeksti: string) =>
  cy.get(`label:contains(${elementinTeksti})`).click({ multiple: true })

export const lahetaHakemus = () =>
  cy.get('[data-test-id=application__send-application-button]').click()

export const painaOkPalautenakymassa = () =>
  cy.get('[data-test-id=application__send-feedback-button--enabled]').click()

export const suljePalaute = () =>
  cy.get('[data-test-id=application-feedback-form__close-button]').click()
