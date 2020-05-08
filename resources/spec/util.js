mocha.ui('bdd')
mocha.reporter('html')
mocha.useColors(false)
mocha.bail(true)
mocha.timeout(120000)

const expect = chai.expect
chai.should()

const loadInFrame = (src) => {
  $('#test')
    .attr('src', src)
    .attr('width', 1400)
    .attr('height', 900)
    .on('error', (err) => {
      console.error(err)
      window.uiError = err
    })
}

const httpGet = (url) => {
  return $.get(url).promise()
}

const testFrame = () => {
  return $('#test').contents()
}

const elementExists = ($e) => {
  return $e && $e.length > 0
}

const triggerEvent = ($e, type) => {
  const evt = new Event(type, { bubbles: true })
  evt.simulated = true
  $e.get(0).dispatchEvent(evt)
}

const isRadioButton = ($e) => {
  return $e.attr('for') && $e.parent().find('#' + $e.attr('for')) !== null
}

const wait = {
  waitIntervalMs: 100,
  testTimeoutDefault: 60000,
  until: (condition, maxWaitMs, infoText) => {
    return () => {
      if (maxWaitMs == undefined) maxWaitMs = wait.testTimeoutDefault
      const deferred = Q.defer()
      const count = Math.floor(maxWaitMs / wait.waitIntervalMs)
      const waitLoop = (remaining) => {
        if (condition()) {
          deferred.resolve()
        } else if (remaining === 0) {
          const errorStr =
            'timeout of ' +
            maxWaitMs +
            'ms in wait.until for condition:\n' +
            condition +
            '\ninfo: ' +
            infoText
          console.error(errorStr)
          deferred.reject(new Error(errorStr))
        } else {
          setTimeout(() => {
            waitLoop(remaining - 1)
          }, wait.waitIntervalMs)
        }
      }
      waitLoop(count)
      return deferred.promise
    }
  },
  untilFalse: (condition) => {
    return wait.until(() => {
      return !condition()
    })
  },
  forMilliseconds: (ms) => {
    return () => {
      const deferred = Q.defer()
      setTimeout(() => {
        deferred.resolve()
      }, ms)
      return deferred.promise
    }
  },
  forElement: (elementQueryFn) => {
    return wait.until(() => {
      return elementExists(elementQueryFn())
    })
  },
}

const blurField = (selectFn) => {
  return wait.until(() => {
    $e = selectFn()
    if (elementExists($e)) {
      triggerEvent($e, 'blur')
      return true
    }
  })
}

const clickElement = (selectFn, infoText) => {
  return wait.until(
    () => {
      $e = selectFn()
      if (elementExists($e)) {
        if (isRadioButton($e)) {
          $e.click()
        } else {
          triggerEvent($e, 'click')
        }
        return true
      }
    },
    null,
    infoText ? infoText : `clickElement: ${selectFn}`
  )
}

const setNativeValue = (element, value) => {
  const valueSetter = Object.getOwnPropertyDescriptor(element, 'value').set
  const prototype = Object.getPrototypeOf(element)
  const prototypeValueSetter = Object.getOwnPropertyDescriptor(
    prototype,
    'value'
  ).set

  if (valueSetter && valueSetter !== prototypeValueSetter) {
    prototypeValueSetter.call(element, value)
  } else {
    valueSetter.call(element, value)
  }
}

const setTextFieldValue = (selectFn, contents) => {
  return wait.until(() => {
    $e = selectFn()
    if (elementExists($e)) {
      setNativeValue($e.get(0), contents)
      $e.get(0).dispatchEvent(new Event('change', { bubbles: true }))
      return true
    }
  })
}

;(() => {
  const origBefore = before
  before = (...arguments) => {
    Array.prototype.slice.call(arguments).forEach((arg) => {
      if (typeof arg !== 'function') {
        throw 'not a function: ' + arg
      }
      origBefore(arg)
      origBefore(wait.forMilliseconds(200)) // :(
    })
  }
})()
