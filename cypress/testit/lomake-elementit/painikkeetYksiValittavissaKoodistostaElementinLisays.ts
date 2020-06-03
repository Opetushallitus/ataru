import * as lomakkeenMuokkaus from '../../lomakkeenMuokkaus'
import LomakkeenTunnisteet from '../../LomakkeenTunnisteet'
import * as odota from '../../odota'
import * as reitit from '../../reitit'

export default (
  lomakkeenTunnisteet: () => LomakkeenTunnisteet,
  testit: () => void
) => {
  describe('Painikkeet, yksi valittavissa, koodisto -elementin lisäys', () => {
    before(() => {
      lomakkeenMuokkaus.komponentinLisays
        .lisaaElementti(
          lomakkeenTunnisteet().lomakkeenId,
          'Painikkeet, yksi valittavissa, koodisto'
        )
        .then(({ result: painikkeenLisaysLinkki }) =>
          cy
            .wrap(painikkeenLisaysLinkki.text())
            .as('component-toolbar-painikkeet-koodisto-text')
        )
        .then(() => {
          lomakkeenMuokkaus.painikeYksiValittavissa
            .syotaKysymysTeksti('Minkä koulutuksen olet suorittanut?')
            .should('have.value', 'Minkä koulutuksen olet suorittanut?')
        })
        .then(() => {
          lomakkeenMuokkaus
            .valitseKoodisto('Kk-pohjakoulutusvaihtoehdot')
            .then(() => lomakkeenMuokkaus.naytaVastausvaihtoehdot())
            .then(() => {
              lomakkeenMuokkaus
                .vastausvaihtoehdot()
                .contains(
                  'Korkeakoulun edellyttämät avoimen korkeakoulun opinnot'
                )
            })
        })
        .then(() => {
          odota.odotaHttpPyyntoa(
            () =>
              cy.route(
                'PUT',
                reitit.virkailija.haeLomakkeenMuuttamisenOsoite(
                  lomakkeenTunnisteet().lomakkeenId
                )
              ),
            () =>
              lomakkeenMuokkaus.painikeYksiValittavissa
                .haeElementinOtsikko()
                .should('have.text', 'Painikkeet, yksi valittavissa, koodisto')
          )
        })
    })

    it('Näyttää kysymyksen tekstin', () => {
      lomakkeenMuokkaus.painikeYksiValittavissa
        .haeKysymysTeksti()
        .should('have.value', 'Minkä koulutuksen olet suorittanut?')
    })

    it('Näyttää valitun koodiston', () => {
      lomakkeenMuokkaus
        .vastausvaihtoehdot()
        .contains('Korkeakoulun edellyttämät avoimen korkeakoulun opinnot')
    })

    testit()
  })
}
