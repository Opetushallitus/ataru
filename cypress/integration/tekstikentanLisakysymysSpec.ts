import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import tekstikentanLisakysymyksenLisays from '../testit/komponentit/tekstikentta/tekstikentanLisakysymyksenLisays'

describe('Tekstikentän lisäkysymys', () => {
  kirjautuminenVirkailijanNakymaan(() => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      tekstikentanLisakysymyksenLisays(lomakkeenTunnisteet)
    })
  })
})
