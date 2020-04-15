const runner = mocha.run()
let failed = 0

$(document).keyup((e) => {
  if (e.keyCode == 27) {
    runner._abort = true
  }
})

const parseTitle = (test) => {
  const titleArr = []
  let thisTest = test
  while (thisTest && thisTest.title) {
    titleArr.unshift(thisTest.title)
    thisTest = thisTest.parent
  }
  return titleArr.join(' / ')
}

runner.on('test end', (t) => {
  console.log('Completed:', parseTitle(t))
})

runner.on('fail', (t, err) => {
  console.log('Failed:', parseTitle(t))
  console.log(err)
  failed++
})

runner.on('end', () => {
  if (failed > 0) {
    console.log('*** TEST FAIL', failed)
  } else {
    console.log('*** TEST SUCCESS')
  }
})
