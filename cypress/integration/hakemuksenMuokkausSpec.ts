import * as reitit from '../reitit'
import * as hakijanNakyma from '../hakijanNakyma'

const luoLomakeJaAvaaHakijanNakyma = (lomakeFixture: string) => {
  cy.kirjauduVirkailijanNakymaan()
  return cy
    .fixture(lomakeFixture)
    .then((lomakeBody) =>
      cy
        .request(
          'POST',
          reitit.virkailija.haeUudenLomakkeenLahettamisenOsoite(),
          lomakeBody
        )
        .then((response) => cy.avaaLomakeHakijanNakymassa(response.body.key))
    )
}

const etsiInputTekstinPerusteella = (teksti: string) => {
  return cy
    .contains(teksti)
    .invoke('attr', 'for')
    .then((id) => cy.get(`#${id}`))
}

describe('Hakemuksen muokkaus', () => {
  it('Tallentaa muutetun valinnan', () => {
    luoLomakeJaAvaaHakijanNakyma('uusiLomake.json')
    hakijanNakyma.henkilotiedot.taytaTiedot()
    cy.contains('Valinta1').click()
    hakijanNakyma.lahetaHakemus()
    hakijanNakyma.avaaUusinHakemusMuokkaustaVarten()
    cy.contains('Valinta2').click()
    hakijanNakyma.tallennaMuokattuHakemus()

    hakijanNakyma.avaaUusinHakemusMuokkaustaVarten()
    etsiInputTekstinPerusteella('Valinta2').should('be.checked')
  })

  it('Ei näytä vastausta kysymykseen, joka on merkitty arkaluontoiseksi', () => {
    luoLomakeJaAvaaHakijanNakyma('uusiLomakeArkaluontoinenVastaus.json')
    hakijanNakyma.henkilotiedot.taytaTiedot()
    cy.contains('Valinta1').click()
    hakijanNakyma.lahetaHakemus()
    hakijanNakyma.avaaUusinHakemusMuokkaustaVarten()

    etsiInputTekstinPerusteella('Valinta1').should('be.disabled')
    etsiInputTekstinPerusteella('Valinta2').should('be.disabled')
  })
})
