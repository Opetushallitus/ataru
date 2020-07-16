import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import tekstikentanLisakysymyksenLisays from '../testit/lomake-elementit/tekstikentta/lomakkeet/tekstikentanLisakysymyksenLisays'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import henkilotietoModuulinTayttaminen from '../testit/henkilotietoModuulinTayttaminen'
import hakemuksenLahettaminen from '../testit/hakemuksenLahettaminen'
import tekstikentanLisakysymykseenVastaaminen from '../testit/lomake-elementit/tekstikentta/hakija/tekstikentanLisakysymykseenVastaaminen'
import tekstikentanLisakysymysEsikatselussa from '../testit/lomake-elementit/tekstikentta/hakija/esikatselu/tekstikentanLisakysymysEsikatselussa'
import hakemustenKasittelyynSiirtyminen from '../testit/hakemustenKasittelyynSiirtyminen'
import tekstikentanLisakysymysHakemustenKasittelyssa from '../testit/lomake-elementit/tekstikentta/virkailija/hakemukset/tekstikentanLisakysymysHakemustenKasittelyssa'

describe('Tekstikentän lisäkysymys', () => {
  kirjautuminenVirkailijanNakymaan(() => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      tekstikentanLisakysymyksenLisays(lomakkeenTunnisteet, () => {
        hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
          henkilotietoModuulinTayttaminen(() => {
            tekstikentanLisakysymykseenVastaaminen(() => {
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
