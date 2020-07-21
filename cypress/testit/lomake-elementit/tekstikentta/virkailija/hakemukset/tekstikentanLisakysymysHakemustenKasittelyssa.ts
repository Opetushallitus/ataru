import { lisakysymys } from './lisakysymys'

export default () => {
  describe('Tekstikentän lisäkysymys hakemusten käsittelyssä', () => {
    it('Lisäkysymyksen vastaus näkyy', () => {
      lisakysymys
        .haeLisäkysymyksenVastaus()
        .should('have.text', 'Vastaus lisäkysymykseen')
    })
  })
}
