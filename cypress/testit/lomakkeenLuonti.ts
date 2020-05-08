import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import * as reitit from '../reitit'
import { unsafeFoldOption } from '../option'
import LomakkeenTunnisteet from '../LomakkeenTunnisteet'

export default (
  testit: (lomakkeenTunnisteet: () => LomakkeenTunnisteet) => void
) => {
  let lomakkeenAvain: string
  let lomakkeenId: number

  before(() => {
    lomakkeenMuokkaus
      .lisaaLomake()
      .then((lomake) => {
        lomakkeenAvain = unsafeFoldOption(lomake.lomakkeenAvain)
        lomakkeenId = unsafeFoldOption(lomake.lomakkeenId)
      })
      .then(() =>
        lomakkeenMuokkaus.asetaLomakkeenNimi('Testilomake', lomakkeenId)
      )
  })

  after(() => {
    cy.poistaLomake(lomakkeenAvain)
  })

  it('Näyttää uuden lomakkeen luontinäkymän', () => {
    cy.url().should(
      (osoite) => expect(osoite.endsWith(lomakkeenAvain)).to.be.true
    )
    lomakkeenMuokkaus
      .haeLomakkeenNimenSyote()
      .should('have.value', 'Testilomake')
    lomakkeenMuokkaus
      .haeLomakkeenEsikatseluLinkki()
      .should('have.text', 'FI')
      .should(
        'have.attr',
        'href',
        reitit.virkailija.haeLomakkeenEsikatseluOsoite(lomakkeenAvain)
      )
  })

  testit(() => ({ lomakkeenAvain, lomakkeenId }))
}
