import { tekstikentta } from './tekstikentta'

export default () => {
  describe('Tekstikentän lisäkysymys esikatselussa', () => {
    it('Lisäkysymyksen vastaus näkyy', () => {
      tekstikentta
        .haeLisäkysymyksenVastaus()
        .should('have.text', 'Vastaus lisäkysymykseen')
    })
  })
}
