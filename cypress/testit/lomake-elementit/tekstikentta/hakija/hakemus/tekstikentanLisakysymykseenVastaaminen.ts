import { lisakysymys } from './lisakysymys'

export default (testit: () => void) => {
  describe('Tekstikentän lisäkysymykseen vastaaminen', () => {
    before(() => {
      lisakysymys
        .syötäTekstikenttäänVastaus('Vastaus')
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
