import { tekstikentta } from './tekstikentta'

export default (testit: () => void) => {
  describe('Tekstikentän lisäkysymykseen arvon perusteella vastaaminen', () => {
    before(() => {
      tekstikentta
        .syötäTekstikenttäänVastaus('2')
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
