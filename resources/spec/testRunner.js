var runner = mocha.run()
var failed = 0

$(document).keyup(function (e) {
  if (e.keyCode == 27) {
    runner._abort = true
  }
})

function parseTitle(test) {
  var titleArr = []
  var thisTest = test
  while(thisTest && thisTest.title) {
    titleArr.unshift(thisTest.title)
    thisTest = thisTest.parent
  }
  return titleArr.join(' / ')
}

runner.on('test', function(t) {
  console.log("Starting", parseTitle(t));
})

runner.on('fail', function(t, err) {
  console.log("Failed:", parseTitle(t))
  console.log(err)
  failed++
})

runner.on('end', function() {
  if (failed > 0) {
    console.log("*** TEST FAIL", failed)
  } else {
    console.log("*** TEST SUCCESS")
  }
})