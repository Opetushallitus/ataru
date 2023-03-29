import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import * as hakijanNakyma from '../hakijanNakyma'
import * as tekstinSyotto from '../tekstinSyotto'

describe('Hakulomakkeen validoinnit', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeen luomista varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      it('Lisää huoltajan yhteystietomoduulin', () => {
        lomakkeenMuokkaus.henkilotiedot.valitseHenkilotietolomakkeenKentat(
          'Opiskelijavalinta, perusopetuksen jälkeinen yhteishaku',
          lomakkeenTunnisteet().lomakkeenId
        )
        lomakkeenMuokkaus.komponentinLisays.lisaaElementti(
          lomakkeenTunnisteet().lomakkeenId,
          'Huoltajan yhteystiedot'
        )
      })
      hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
        it('Ei näytä huoltajan yhteystietokenttiä, kun lomakkeen avaa ensimmäistä kertaa', () => {
          cy.contains('Huoltajan tiedot (jos olet alle 18v)', {
            matchCase: true,
          }).should('not.exist')
        })
        it('Tuottaa validointivirheen virheellisestä hetusta', () => {
          hakijanNakyma.henkilotiedot.henkilotunnus().should('exist')
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.henkilotunnus(),
            'asd'
          )
          hakijanNakyma.henkilotiedot
            .henkilotunnus()
            .invoke('attr', 'aria-invalid')
            .should('eq', 'true')
          cy.contains('Henkilötunnus on oltava muodossa PPKKVVXNNNT.', {
            matchCase: true,
          }).should('exist')
        })
        it('Näyttää huoltajan yhteystietokentät kun täyttää alaikäisen hetun', () => {
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.henkilotunnus(),
            '010123A968L' // tämä testihetu pysyy alaikäisenä hyvän aikaa!
          )
          cy.contains('Henkilötunnus on oltava muodossa PPKKVVXNNNT.', {
            matchCase: true,
          }).should('not.exist') // virheilmoitus poistuu
          cy.contains('Huoltajan tiedot (jos olet alle 18v)', {
            matchCase: true,
          }).should('exist')
          hakijanNakyma.huoltajantiedot.huoltajanSahkoposti().should('exist')
        })
        it('Näyttää virheilmoituksen kun huoltajan sähköposti on virheellinen', () => {
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.huoltajantiedot.huoltajanSahkoposti(),
            'asd'
          )
          hakijanNakyma.huoltajantiedot
            .huoltajanSahkoposti()
            .invoke('attr', 'aria-invalid')
            .should('eq', 'true')
        })
      })
    })
  })
})
