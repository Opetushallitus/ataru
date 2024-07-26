import path from 'path'

import { Page, Response, Route } from '@playwright/test'
import * as Record from 'fp-ts/lib/Record'
import * as Option from 'fp-ts/lib/Option'
import { AssertionError } from 'assert'

type HttpMethod = 'PUT' | 'POST' | 'GET' | 'DELETE'

export const waitForResponse = (
  page: Page,
  method: HttpMethod,
  urlMatcher: (url: string) => boolean
) =>
  page.waitForResponse((response) => {
    return response.request().method() === method && urlMatcher(response.url())
  })

export const getJsonResponseKey = async <T>(res: Response, key: string) => {
  try {
    const body = await res.json()
    return Record.has(key, body) ? Option.some(body[key] as T) : Option.none
  } catch (e) {
    return Option.none
  }
}

export const unsafeFoldOption = <T>(o: Option.Option<T>): T => {
  return Option.fold<T, T>(
    () => {
      throw new AssertionError({ message: 'Option was None' })
    },
    (val) => val
  )(o)
}

const FIXTURES_PATH = path.resolve(__dirname, '../cypress/fixtures')

export const getFixturePath = (fileName: string) =>
  path.resolve(FIXTURES_PATH, fileName)

export const fixtureFromFile = (fileName: string) => (route: Route) =>
  route.fulfill({ path: getFixturePath(fileName) })
