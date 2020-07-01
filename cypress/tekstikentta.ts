import * as asetukset from './asetukset'

import Chainable = Cypress.Chainable

/**
 * Tämä tapa on nopeampi joten sitä kannattaa käyttää, jos ei ole riskiä siitä,
 * että Reactin hallinta hävittäisi syötettyjä merkkejä.
 */
export const syotaTekstiTarkistamatta = <T>(
  elementti: Chainable<T>,
  teksti: string
): Chainable<T> => {
  return elementti
    .clear()
    .type(teksti, { delay: asetukset.tekstikentanSyotonViive })
}

/**
 * Tämä on turvallinen tapa täyttää tekstikenttä, koska se tarkistaa jokaisen
 * merkin syöttämisen jälkeen, että syötetty merkki ehtii renderöityä kenttään
 * ennen kuin seuraava merkki syötetään.
 */
export const syotaTeksti = <T>(
  elementti: Chainable<T>,
  teksti: string
): Chainable<T> => {
  return elementti
    .clear()
    .then(() => {
      elementti.should('have.value', '')
    })
    .then((tyhjennettyElementti) => {
      const merkit = [...teksti]
      return Cypress.Promise.all(
        merkit.map((m, i) => {
          elementti
            .type(m, {
              delay: 0,
            })
            .then(() => {
              elementti.should('have.value', teksti.substring(0, i + 1))
            })
        })
      ).then(() => tyhjennettyElementti)
    })
}
