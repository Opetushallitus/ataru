import * as lomakkeenMuokkaus from '../lomakkeenMuokkaus'
import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'

describe('Henkilötietomoduuli', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      it('Näyttää henkilötietomoduulin', () => {
        lomakkeenMuokkaus.henkilotiedot
          .haeOtsikko()
          .should('have.text', 'Henkilötiedot')
        lomakkeenMuokkaus.henkilotiedot
          .haeHenkilotietojenValintaKomponentti()
          .find(':selected')
          .should('have.attr', 'value', 'onr')
          .should('have.text', 'Opiskelijavalinta')
        lomakkeenMuokkaus.henkilotiedot
          .haeKaytettavatHenkilotietoKentat()
          .should(
            'have.text',
            'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sukupuoli, Syntymäpaikka ja -maa, Passin numero, Kansallinen ID-tunnus, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa, Äidinkieli'
          )
      })

      describe('Henkilötietomoduulin kenttien vaihtaminen', () => {
        before(() => {
          lomakkeenMuokkaus.henkilotiedot.valitseHenkilotietolomakkeenKentat(
            'Muu käyttö',
            lomakkeenTunnisteet().lomakkeenId
          )
        })

        it('Näyttää henkilötietomoduulin muutetut kentät', () => {
          lomakkeenMuokkaus.henkilotiedot
            .haeKaytettavatHenkilotietoKentat()
            .should(
              'have.text',
              'Etunimet, Kutsumanimi, Sukunimi, Kansalaisuus, Onko sinulla suomalainen henkilötunnus?, Henkilötunnus, Syntymäaika, Sähköpostiosoite, Matkapuhelin, Asuinmaa, Katuosoite, Postinumero, Postitoimipaikka, Kotikunta, Kaupunki ja maa'
            )
        })

        after(() => {
          lomakkeenMuokkaus.henkilotiedot.valitseHenkilotietolomakkeenKentat(
            'Opiskelijavalinta',
            lomakkeenTunnisteet().lomakkeenId
          )
        })
      })
    })
  })
})
