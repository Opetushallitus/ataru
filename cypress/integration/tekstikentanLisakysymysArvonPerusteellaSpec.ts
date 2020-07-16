import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import henkilotietoModuulinTayttaminen from '../testit/henkilotietoModuulinTayttaminen'
import tekstikentanLisakysymysArvonPerusteellaLisays from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/tekstikentanLisakysymysArvonPerusteellaLisays'

describe('Tekstikentän lisäkysymys', () => {
  kirjautuminenVirkailijanNakymaan(() => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      tekstikentanLisakysymysArvonPerusteellaLisays(lomakkeenTunnisteet, () => {
        hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
          henkilotietoModuulinTayttaminen(() => {})
        })
      })
    })
  })
})
