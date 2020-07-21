import { lisakysymys } from './lisakysymys'

export default (testit: () => void) => {
  describe('Tekstikentän lisäkysymykseen arvon perusteella vastaaminen', () => {
    before(() => {
      lisakysymys
        .syötäTekstikenttäänVastaus('2')
        .then(() =>
          lisakysymys.syötäLisäkysymykseenVastaus('Vastaus lisäkysymykseen')
        )
    })

    it('Lisäkysymykseen on syötetty vastaus', () => {
      lisakysymys
        .haeLisäkysymyksenVastaus()
        .should('have.value', 'Vastaus lisäkysymykseen')
    })

    testit()
  })
}
