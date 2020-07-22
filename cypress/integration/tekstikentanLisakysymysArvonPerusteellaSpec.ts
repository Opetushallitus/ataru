import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import henkilotietoModuulinTayttaminen from '../testit/henkilotietoModuulinTayttaminen'
import tekstikentanLisakysymysArvonPerusteellaLisays from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/tekstikentanLisakysymysArvonPerusteellaLisays'
import hakemuksenLahettaminen from '../testit/hakemuksenLahettaminen'
import tekstikentanLisakysymysEsikatselussa from '../testit/lomake-elementit/tekstikentta/hakija/esikatselu/tekstikentanLisakysymysEsikatselussa'
import hakemustenKasittelyynSiirtyminen from '../testit/hakemustenKasittelyynSiirtyminen'
import tekstikentanLisakysymysHakemustenKasittelyssa from '../testit/lomake-elementit/tekstikentta/virkailija/hakemukset/tekstikentanLisakysymysHakemustenKasittelyssa'
import tekstikentanLisakysymykseenArvonPerusteellaVastaaminen from '../testit/lomake-elementit/tekstikentta/hakija/hakemus/tekstikentanLisakysymykseenArvonPerusteellaVastaaminen'

describe('Tekstikentän lisäkysymys arvon perusteella', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      tekstikentanLisakysymysArvonPerusteellaLisays(lomakkeenTunnisteet, () => {
        hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
          henkilotietoModuulinTayttaminen(() => {
            tekstikentanLisakysymykseenArvonPerusteellaVastaaminen(() => {
              hakemuksenLahettaminen(() => {
                tekstikentanLisakysymysEsikatselussa()
                hakemustenKasittelyynSiirtyminen(lomakkeenTunnisteet, () => {
                  tekstikentanLisakysymysHakemustenKasittelyssa()
                })
              })
            })
          })
        })
      })
    })
  })
})
