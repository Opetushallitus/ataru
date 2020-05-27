import * as dropdown from './dropdown'

interface OppiaineenArvosanat {
  oppiaine: string
  arvosana: string
  oppimaara?: string
  index: number
}

export const haeLomakkeenNimi = () =>
  cy.get('[data-test-id=application-header-label]:visible')

export const arvosanat = {
  asetaOppiaineenArvosanat: ({
    oppiaine,
    arvosana,
    oppimaara,
    index,
  }: OppiaineenArvosanat) =>
    dropdown
      .setDropdownValue(
        `oppiaineen-arvosana-${oppiaine}-arvosana-${index}`,
        `arvosana-${oppiaine}-${arvosana}`
      )
      .then((c) =>
        oppimaara
          ? dropdown.setDropdownValue(
              `oppiaineen-arvosana-${oppiaine}-oppimaara-${index}`,
              oppimaara
            )
          : c
      ),

  haeValinnaisaineLinkki: ({
    oppiaine,
    index,
    poisKaytosta,
  }: {
    oppiaine: string
    index: number
    poisKaytosta: boolean
  }) =>
    cy.get(
      `${
        poisKaytosta ? 'span' : 'a'
      }[data-test-id=oppiaineen-arvosana-${oppiaine}-lisaa-valinnaisaine-linkki-0-${
        index === 0 ? 'lisaa' : 'poista'
      }]:visible`
    ),

  lisaaValinnaisaine: ({ oppiaine }: { oppiaine: string }) =>
    arvosanat
      .haeValinnaisaineLinkki({ oppiaine, index: 0, poisKaytosta: false })
      .click(),
}
