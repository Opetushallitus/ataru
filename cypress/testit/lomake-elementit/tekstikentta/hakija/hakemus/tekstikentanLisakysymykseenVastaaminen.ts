import { tekstikentta } from './tekstikentta'

export default (testit: () => void) => {
  describe('Tekstikentän lisäkysymykseen vastaaminen', () => {
    before(() => {
      tekstikentta
        .syötäTekstikenttäänVastaus('Vastaus')
        .then(() =>
          tekstikentta.syötäLisäkysymykseenVastaus('Vastaus lisäkysymykseen')
        )
    })

    it('Lisäkysymykseen on syötetty vastaus', () => {
      tekstikentta
        .haeLisäkysymyksenVastaus()
        .should('have.value', 'Vastaus lisäkysymykseen')
    })

    testit()
  })
}
