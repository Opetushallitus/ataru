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

const etsiInputLabelinTekstinPerusteella = (teksti: string) => {
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
    etsiInputLabelinTekstinPerusteella('Valinta2').should('be.checked')
  })

  it('Ei näytä vastausta kysymyksiin, jotka on merkitty arkaluontoiseksi', () => {
    luoLomakeJaAvaaHakijanNakyma('uusiLomakeArkaluontoinenVastaus.json')
    hakijanNakyma.henkilotiedot.taytaTiedot()

    cy.contains('Valinta1').click()
    etsiInputLabelinTekstinPerusteella('Salainen Tekstialue').type(
      'Salainen vastaus'
    )

    hakijanNakyma.lahetaHakemus()
    hakijanNakyma.avaaUusinHakemusMuokkaustaVarten()

    etsiInputLabelinTekstinPerusteella('Valinta1').should('be.disabled')
    etsiInputLabelinTekstinPerusteella('Valinta2').should('be.disabled')
    etsiInputLabelinTekstinPerusteella('Salainen Tekstialue').should(
      'be.disabled'
    )
    etsiInputLabelinTekstinPerusteella('Salainen Tekstialue').should(
      'have.value',
      '***********'
    )
  })
})
