import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import * as hakijanNakyma from '../hakijanNakyma'
import * as tekstinSyotto from '../tekstinSyotto'
import tekstikentanLisakysymyksenLisays from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/tekstikentanLisakysymyksenLisays'
import { lisakysymys } from '../testit/lomake-elementit/tekstikentta/hakija/hakemus/lisakysymys'
import { tekstikentta } from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/tekstikentta'
import { tekstialue } from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/tekstialue'

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
          cy.contains('Toisen huoltajan tiedot', {
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
          cy.contains(
            'Henkilötunnuksen on oltava muodossa PPKKVVvälimerkkiNNNT, jossa välimerkki on "-" tai "A". Myös välimerkit "Y" ja "B" ovat sallittuja.',
            {
              matchCase: true,
            }
          ).should('exist')
        })
        it('Näyttää huoltajan yhteystietokentät kun täyttää alaikäisen hetun', () => {
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.henkilotunnus(),
            '010123A968L' // tämä testihetu pysyy alaikäisenä hyvän aikaa!
          )
          cy.contains(
            'Henkilötunnuksen on oltava muodossa PPKKVVvälimerkkiNNNT, jossa välimerkki on "-" tai "A". Myös välimerkit "Y" ja "B" ovat sallittuja.',
            {
              matchCase: true,
            }
          ).should('not.exist') // virheilmoitus poistuu
          cy.contains('Toisen huoltajan tiedot', {
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
          cy.contains(
            'Sähköpostiosoitteesi on väärässä muodossa. Sähköpostiosoite on oltava muodossa nimi@osoite.fi.',
            {
              matchCase: true,
            }
          ).should('exist')
        })
      })
    })
  })
})
describe('Hakulomakkeen henkilötietojen tekstikentät trimmataan', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeen luomista varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      it('Lisää henkilötiedot ja huoltajan yhteystiedot', () => {
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
        it('Trimmaa henkilötiedoista async-validoitavat kentät', () => {
          hakijanNakyma.henkilotiedot.henkilotunnus().should('exist')
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.henkilotunnus(),
            '010123A968L   '
          )
          // kentän trimmaus tapahtuu on-blur-eventillä joten fokusoidaan toiseen kenttään
          hakijanNakyma.henkilotiedot.matkapuhelin().click()
          const hetukentta = hakijanNakyma.henkilotiedot.henkilotunnus()
          hetukentta.should('have.value', '010123A968L')
          hetukentta.invoke('attr', 'aria-invalid').should('eq', 'false')
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.sahkoposti(),
            '  testi@example.org  '
          )
          // kentän trimmaus tapahtuu on-blur-eventillä joten fokusoidaan toiseen kenttään
          hakijanNakyma.henkilotiedot.sahkopostitoisto().click()
          hakijanNakyma.henkilotiedot
            .sahkoposti()
            .should('have.value', 'testi@example.org')
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.sahkopostitoisto(),
            '  testi@example.org  '
          )
          // sähköpostikentän trimmaus tapahtuu on-blur-eventillä joten fokusoidaan toiseen kenttään
          hakijanNakyma.henkilotiedot.matkapuhelin().click()
          hakijanNakyma.henkilotiedot
            .sahkopostitoisto()
            .should('have.value', 'testi@example.org')
        })
        it('Trimmaa henkilötiedoista validoitavat tekstikentät', () => {
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.matkapuhelin(),
            ' 0401234567   '
          )
          // kentän trimmaus tapahtuu on-blur-eventillä joten fokusoidaan toiseen kenttään
          hakijanNakyma.henkilotiedot.etunimi().click()
          hakijanNakyma.henkilotiedot
            .matkapuhelin()
            .should('have.value', '0401234567')
          tekstinSyotto.syotaTeksti(
            hakijanNakyma.henkilotiedot.etunimi(),
            ' Testi   '
          )
          hakijanNakyma.henkilotiedot.sukunimi().click()
          hakijanNakyma.henkilotiedot.etunimi().should('have.value', 'Testi')
        })
      })
    })
  })
})
describe('Lisäkysymyksen tekstikenttä trimmataan', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeen luomista varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      tekstikentanLisakysymyksenLisays(lomakkeenTunnisteet, () => {
        hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
          it('Trimmaa lisäkysymysten vastaukset', () => {
            lisakysymys
              .syötäTekstikenttäänVastaus('  Vastaus  ')
              .then(() =>
                lisakysymys.syötäLisäkysymykseenVastaus(
                  ' Vastaus lisäkysymykseen '
                )
              )
            // trimmaus tapahtuu on-blur-eventillä joten fokusoidaan toiseen kenttään
            hakijanNakyma.henkilotiedot.matkapuhelin().click()
            lisakysymys.kysymysKenttä().should('have.value', 'Vastaus')
            lisakysymys
              .haeLisäkysymyksenVastaus()
              .should('have.value', 'Vastaus lisäkysymykseen')
          })
        })
      })
    })
  })
})

// pakko tehdä erillinen testi erityyppisille teksti-inputeille,
// koska lomake-editorissa ei saa yksilöityä eri tekstikenttiä
describe('Tekstialue trimmataan', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeen luomista varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      it('Lisää tekstialue', () => {
        tekstialue
          .lisaaTekstialue(lomakkeenTunnisteet().lomakkeenId)
          .then(() => tekstialue.asetaKysymys('Tekstialuekysymys'))
          .then(() => tekstialue.asetaMaxMerkkimaara('50'))
      })
      hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
        it('Trimmaa tekstialueen', () => {
          tekstialue.syötäTekstialueenVastaus(
            '  Tekstiä\nja toinen rivi\nja kolmas rivi  \n\n'
          )
          // trimmaus tapahtuu on-blur-eventillä joten fokusoidaan toiseen kenttään
          hakijanNakyma.henkilotiedot.matkapuhelin().click()
          tekstialue
            .haeTekstialue()
            .should('have.value', 'Tekstiä\nja toinen rivi\nja kolmas rivi')
        })
      })
    })
  })
})
describe('Hakulomakkeen toistuvat tekstikentät trimmataan', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeen luomista varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      it('Lisää toistuva tekstikenttä', () => {
        lomakkeenMuokkaus.teeJaodotaLomakkeenTallennusta(
          lomakkeenTunnisteet().lomakkeenId,
          () =>
            tekstikentta
              .lisaaTekstikentta(lomakkeenTunnisteet().lomakkeenId)
              .then(() =>
                tekstikentta.asetaKysymys('Toistettavan tekstikentän kysymys')
              )
              .then(() => tekstikentta.voiLisätäUseitaValinta().click())
        )
      })
      hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
        it('Trimmaa toistettavan tekstikentän', () => {
          tekstinSyotto.syotaTekstiTarkistamatta(
            cy.get('[data-test-id=repeatable-text-field-0]'),
            '  ensimmäinen teksti  '
          )
          tekstinSyotto.syotaTekstiTarkistamatta(
            cy.get('[data-test-id=repeatable-text-field-1]'),
            '  toinen teksti  '
          )
          hakijanNakyma.henkilotiedot.matkapuhelin().click()
          cy.get('[data-test-id=repeatable-text-field-0]').should(
            'have.value',
            'ensimmäinen teksti'
          )
          cy.get('[data-test-id=repeatable-text-field-1]').should(
            'have.value',
            'toinen teksti'
          )
        })
      })
    })
  })
})
