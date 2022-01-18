import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import painikkeetYksiValittavissaKoodistostaElementinLisays from '../testit/lomake-elementit/painikkeetYksiValittavissaKoodistostaElementinLisays'
import painikkeetYksiValittavissaKoodistostaHakijalle from '../testit/lomake-elementit/painikkeetYksiValittavissaKoodistostaHakijalle'
import henkilotietoModuulinTayttaminen from '../testit/henkilotietoModuulinTayttaminen'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import { getSensitiveAnswer } from '../checkbox'

describe('Painikkeet, yksi valittavissa, koodisto -lomake-elementti', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      painikkeetYksiValittavissaKoodistostaElementinLisays(
        lomakkeenTunnisteet,
        () => {
          it('Asetetaan tieto arkaluontoiseksi', () => {
            getSensitiveAnswer().should('not.be.checked')
            getSensitiveAnswer()
              .check()
              .then(() => getSensitiveAnswer().should('be.checked'))
          })
          hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
            henkilotietoModuulinTayttaminen(() => {
              painikkeetYksiValittavissaKoodistostaHakijalle()
            })
          })
        }
      )
    })
  })
})
