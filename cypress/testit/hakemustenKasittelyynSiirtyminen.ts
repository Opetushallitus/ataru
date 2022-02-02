import LomakkeenTunnisteet from '../LomakkeenTunnisteet'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Hakemusten käsittelyyn siirtyminen', () => {
    before(() => {
      cy.avaaLomakkeenHakemuksetVirkailijanNakymassa(
        lomakkeenTunnisteet().lomakkeenAvain
      )
      cy.get('[data-test-id=show-results]').click()
    })

    it('Lataa virkailijan hakemukset-näkymän', () => {})

    testit()
  })
}
