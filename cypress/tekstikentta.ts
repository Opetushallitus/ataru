import Chainable = Cypress.Chainable

export const syotaTeksti = <T>(
  elementti: Chainable<T>,
  teksti: string
): Chainable<T> => {
  return elementti.clear().then((tyhjennettyElementti) => {
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
