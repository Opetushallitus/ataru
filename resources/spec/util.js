mocha.ui('bdd')
mocha.reporter('html')
mocha.useColors(false)
mocha.bail(true)
mocha.timeout(30000)

var expect = chai.expect;
chai.should();

var loadInFrame = function(src) {
  $('#test')
    .attr('src', src)
    .attr('width', 1024)
    .attr('height', 768)
    .on('error', function (err) {
      console.error(err);
      window.uiError = err;
    });
}

var testFrame = function() {
  return $('#test').contents()
}

var elementExists = function($e) {
  return $e && $e.length > 0
}

var triggerEvent = function($e, type) {
  var evt = new Event(type, { bubbles: true })
  evt.simulated = true
  $e.get(0).dispatchEvent(evt)
}

var isRadioButton = function($e) {
  return $e.attr('for') && $e.parent().find("#" + $e.attr('for')) !== null
}

var clickElement = function(selectFn) {
  return wait.until(function () {
    $e = selectFn()
    if (elementExists($e)) {
      if (isRadioButton($e)) {
        $e.click()
      } else {
        triggerEvent($e, 'click')
      }
      return true
    }
  })
}

function setTextFieldValue(selectFn, contents) {
  return wait.until(function() {
    $e = selectFn()
    if (elementExists($e)) {
      $e.get(0).value = contents
      triggerEvent($e, 'input')
      return true
    }
  })
}

var wait = {
  waitIntervalMs: 100,
  testTimeoutDefault: 10000,
  until: function(condition, maxWaitMs) {
    return function() {
      if (maxWaitMs == undefined) maxWaitMs = wait.testTimeoutDefault;
      var deferred = Q.defer()
      var count = Math.floor(maxWaitMs / wait.waitIntervalMs);

      (function waitLoop(remaining) {
        if (condition()) {
          deferred.resolve()
        } else if (remaining === 0) {
          const errorStr = "timeout of " + maxWaitMs + "ms in wait.until for condition:\n" + condition
          console.error(new Error(errorStr))
          deferred.reject(errorStr)
        } else {
          setTimeout(function() {
            waitLoop(remaining-1)
          }, wait.waitIntervalMs)
        }
      })(count)
      return deferred.promise
    }
  },
  untilFalse: function(condition) {
    return wait.until(function() { return !condition()})
  },
  forMilliseconds: function(ms) {
    return function() {
      var deferred = Q.defer()
      setTimeout(function() {
        deferred.resolve()
      }, ms)
      return deferred.promise
    }
  },
  forElement: function(elementQueryFn) {
    return wait.until(function() {
      return elementExists(elementQueryFn())
    })
  }
};

(function improveMocha() {
  var origBefore = before
  before = function() {
    Array.prototype.slice.call(arguments).forEach(function(arg) {
      if (typeof arg !== "function") {
        throw ("not a function: " + arg)
      }
      origBefore(arg)
      origBefore(wait.forMilliseconds(200)) // :(
    })
  }
})()
