var page = require('webpage').create();
var system = require('system');
var args = system.args;

var app = args[1];
var cookieOrFormIdOrSecret = args.length > 2 ? args[2] : ''

var url
function addPhantomCookie() {
  phantom.addCookie({
    'name': 'ring-session',
    'value': cookieOrFormIdOrSecret,
    'domain': 'localhost'
  });
}
switch (app) {
  case 'virkailija':
    addPhantomCookie();
    url = 'http://localhost:8350/lomake-editori/virkailija-test.html'
    break;
  case 'hakija-form':
    url = 'http://localhost:8351/hakemus/hakija-form-test.html?formId=' + cookieOrFormIdOrSecret
    break;
  case 'virkailija-edit':
    addPhantomCookie();
    url = 'http://localhost:8350/lomake-editori/virkailija-edit-test.html'
    break;
  case 'hakija-haku':
    url = 'http://localhost:8351/hakemus/hakija-haku-test.html?hakuOid=' + cookieOrFormIdOrSecret
    break;
  case 'hakija-hakukohde':
    url = 'http://localhost:8351/hakemus/hakija-hakukohde-test.html?hakukohdeOid=' + cookieOrFormIdOrSecret
    break;
  case 'hakija-edit':
    url = 'http://localhost:8351/hakemus/hakija-edit-test.html?modify=' + cookieOrFormIdOrSecret
    break;
  default:
    console.log('invalid app: ' + app)
    phantom.exit(1)
}

console.log("running browser tests for", app, url, cookieOrFormIdOrSecret);

global.testsSuccessful = undefined;
var resultPrefix = '*** TEST';
var successMsg = ' SUCCESS';
var failMsg = ' FAIL';
var TIMEOUT_MINS = 2
var timeoutMs = TIMEOUT_MINS * 60 * 1000;
var startTime = new Date().getTime();

function startsWith(haystack, needle) {
  return haystack.substring(0, needle.length) === needle
}

function takeScreenshot() {
  var filename = '/tmp/ataru-fail-' + new Date().getTime() + '.png'
  console.log('Taking screenshot', filename)
  page.render(filename)
}

page.onConsoleMessage = function (message) {
  console.log(message);
  if (startsWith(message, resultPrefix)) {
    if (startsWith(message, resultPrefix + successMsg)) {
      global.testsSuccessful = true;
    } else if (startsWith(message, resultPrefix + failMsg)) {
      takeScreenshot()
      global.testsSuccessful = false;
    } else {
      console.log("Unknown result:", message);
      global.testsSuccessful = false;
    }
  }
};

page.open(url, function (status) {
  if (status !== "success") {
    console.log('Failed to open');
    phantom.exit(1);
  }

  function stopWhenFinished() {
    if (new Date().getTime() > startTime + timeoutMs) {
      console.log('Tests timed out after', timeoutMs);
      phantom.exit(2);
    } else if (typeof global.testsSuccessful === 'undefined') {
      setTimeout(stopWhenFinished, 1000);
    } else {
      phantom.exit(global.testsSuccessful ? 0 : 1);
    }
  }

  stopWhenFinished();
});
