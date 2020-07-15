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
    })

    it('Lataa virkailijan hakemukset-näkymän', () => {})

    testit()
  })
}
