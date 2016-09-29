(function() {
  before(function () {
    var query = location.search.substring(1).split('&')
    var formId = ''

    for (var i = 0; i < query.length; i++) {
      var param = query[i].split('=')
      if (param[0] == 'formId') {
        formId = param[1]
      }
    }

    console.log("form id", formId || 'UNDEFINED')
    loadInFrame('/hakemus/' + formId)
  })

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('hakemus', function() {

    describe('page loads', function () {
      before(
        wait.until(function() { return true; })
      )
      it('with complete form', function () {
        expect(function() { return true; })
      })
    })
  })
})()