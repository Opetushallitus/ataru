import * as hakijanNakyma from '../../hakijanNakyma'

export default () => {
  describe('Peruskoulun arvosanat -osion lukunäkymä', () => {
    it('Näyttää peruskoulun arvosanat -osion lukunäkymän kaikki oppiaineet', () => {
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'A',
        oppimaara: 'Ruotsi toisena kielenä',
        arvosana: '7',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'A',
        oppimaara: 'Suomi viittomakielisille',
        arvosana: '8',
        index: 1,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'A',
        oppimaara: 'Suomi saamenkielisille',
        arvosana: 'Ei arvosanaa',
        index: 2,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'A',
        oppimaara: 'Ruotsi viittomakielisille',
        arvosana: 'S (Hyväksytty)',
        index: 3,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'A1',
        arvosana: 'O (Osallistunut)',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'B1',
        arvosana: '9',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanValinnainenKieli({
        oppimaara: 'japani',
        arvosana: 'Ei arvosanaa',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanValinnainenKieli({
        oppimaara: 'Muu oppilaan äidinkieli',
        arvosana: '6',
        index: 1,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'MA',
        arvosana: '10',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'BI',
        arvosana: '5',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'GE',
        arvosana: '6',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'FY',
        arvosana: '10',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'KE',
        arvosana: '8',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'TT',
        arvosana: '7',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'TY',
        arvosana: '5',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'HI',
        arvosana: '4',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'YH',
        arvosana: '10',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'MU',
        arvosana: '10',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'KU',
        arvosana: '8',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'KA',
        arvosana: '5',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'LI',
        arvosana: '9',
        index: 0,
      })
      hakijanNakyma.arvosanat.tarkistaLukunakymanOppiaine({
        oppiaine: 'KO',
        arvosana: '6',
        index: 0,
      })
    })
  })
}
