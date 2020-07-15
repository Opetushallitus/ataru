import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import tekstikentanLisakysymyksenLisays from '../testit/lomake-elementit/tekstikentta/lomakkeet/tekstikentanLisakysymyksenLisays'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import henkilotietoModuulinTayttaminen from '../testit/henkilotietoModuulinTayttaminen'
import hakemuksenLahettaminen from '../testit/hakemuksenLahettaminen'
import tekstikentanLisakysymykseenVastaaminen from '../testit/lomake-elementit/tekstikentta/hakija/tekstikentanLisakysymykseenVastaaminen'
import tekstikentanLisakysymysEsikatselussa from '../testit/lomake-elementit/tekstikentta/hakija/esikatselu/tekstikentanLisakysymysEsikatselussa'

describe('Tekstikentän lisäkysymys', () => {
  kirjautuminenVirkailijanNakymaan(() => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      tekstikentanLisakysymyksenLisays(lomakkeenTunnisteet, () => {
        hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
          henkilotietoModuulinTayttaminen(() => {
            tekstikentanLisakysymykseenVastaaminen(() => {
              hakemuksenLahettaminen(() => {
                tekstikentanLisakysymysEsikatselussa()
              })
            })
          })
        })
      })
    })
  })
})
