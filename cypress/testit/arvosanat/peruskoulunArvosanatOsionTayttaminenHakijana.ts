import * as hakijanNakyma from '../../hakijanNakyma'

export default () => {
  describe('Peruskoulun arvosanat -osion täyttäminen hakijana', () => {
    before(() => {
      hakijanNakyma.arvosanat
        .asetaOppiaineenArvosanat({
          oppiaine: 'A',
          arvosana: '7',
          oppimaara: 'ruotsi-toisena-kielena',
          index: 0,
        })
        .then(() =>
          hakijanNakyma.arvosanat.lisaaValinnaisaine({ oppiaine: 'A' })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'A',
            arvosana: '8',
            oppimaara: 'suomi-viittomakielisille',
            index: 1,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.lisaaValinnaisaine({ oppiaine: 'A' })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'A',
            arvosana: 'ei-arvosanaa',
            oppimaara: 'suomi-saamenkielisille',
            index: 2,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.lisaaValinnaisaine({ oppiaine: 'A' })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'A',
            arvosana: 'hyvaksytty',
            oppimaara: 'ruotsi-viittomakielisille',
            index: 3,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'A1',
            arvosana: 'osallistunut',
            oppimaara: 'FI',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'B1',
            arvosana: '9',
            oppimaara: 'SV',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.lisaaValinnainenKieli({
            oppiaine: 'a1',
            oppimaara: 'JA',
            arvosana: 'ei-arvosanaa',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.lisaaValinnainenKieli({
            oppiaine: 'a',
            oppimaara: 'muu-oppilaan-aidinkieli',
            arvosana: '6',
            index: 1,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'MA',
            arvosana: '10',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'BI',
            arvosana: '5',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'GE',
            arvosana: '6',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'FY',
            arvosana: '10',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'KE',
            arvosana: '8',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'TT',
            arvosana: '7',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'TY',
            arvosana: '5',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'HI',
            arvosana: '4',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'YH',
            arvosana: '10',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'MU',
            arvosana: '10',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'KU',
            arvosana: '8',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'KA',
            arvosana: '5',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'LI',
            arvosana: '9',
            index: 0,
          })
        )
        .then(() =>
          hakijanNakyma.arvosanat.asetaOppiaineenArvosanat({
            oppiaine: 'KO',
            arvosana: '6',
            index: 0,
          })
        )
    })

    it('Näyttää täytetyn peruskoulun arvosanat -osion', () => {
      hakijanNakyma.arvosanat
        .haeValinnaisaineLinkki({ oppiaine: 'A', index: 0, poisKaytosta: true })
        .should('exist')
    })
  })
}
