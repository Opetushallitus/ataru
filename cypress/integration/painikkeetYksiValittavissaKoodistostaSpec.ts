import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import painikkeetYksiValittavissaKoodistostaElementinLisays from '../testit/lomake-elementit/painikkeetYksiValittavissaKoodistostaElementinLisays'
import painikkeetYksiValittavissaKoodistostaHakijalle from '../testit/lomake-elementit/painikkeetYksiValittavissaKoodistostaHakijalle'

describe('Painikkeet, yksi valittavissa, koodisto -lomake-elementti', () => {
  kirjautuminenVirkailijanNakymaan(() => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      painikkeetYksiValittavissaKoodistostaElementinLisays(
        lomakkeenTunnisteet,
        painikkeetYksiValittavissaKoodistostaHakijalle(lomakkeenTunnisteet)
      )
    })
  })
})
