(function() {
  before(function () {
    var id = '123'
    loadInFrame('/hakemus/' + id)
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