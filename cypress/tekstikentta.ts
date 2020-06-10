import * as asetukset from './asetukset'

import Chainable = Cypress.Chainable

export const syotaTeksti = <T>(
  elementti: Chainable<T>,
  teksti: string
): Chainable<T> =>
  elementti.clear().type(teksti, { delay: asetukset.tekstikentanSyotonViive })
