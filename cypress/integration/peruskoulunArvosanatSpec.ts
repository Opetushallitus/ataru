import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import peruskoulunArvosanatOsionPoistaminen from '../testit/arvosanat/peruskoulunArvosanatOsionPoistaminen'
import peruskoulunArvosanatOsionLisays from '../testit/arvosanat/peruskoulunArvosanatOsionLisays'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'

describe.skip('Peruskoulun arvosanat -osio', () => {
  kirjautuminenVirkailijanNakymaan(() => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      peruskoulunArvosanatOsionLisays(lomakkeenTunnisteet, () => {
        peruskoulunArvosanatOsionPoistaminen(lomakkeenTunnisteet)
        hakijanNakymaanSiirtyminen(lomakkeenTunnisteet)
      })
    })
  })
})
