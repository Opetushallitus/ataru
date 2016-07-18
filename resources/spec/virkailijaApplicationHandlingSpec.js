var applicationHandlingTab = function() {
  return testFrame().find('.section-link.application')
}

function applicationHandlingLinkExists() {
  return elementExists(applicationHandlingTab())
}

(function() {
  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Application handling', function() {
    describe('with no forms', function() {
      before(
        wait.until(applicationHandlingLinkExists)
      )
      it('has applications', function() {
        clickElement(applicationHandlingTab)
        expect('foo').to.not.equal('bar');
      })
    })
  })

})();
