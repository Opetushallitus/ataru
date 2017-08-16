(function () {
  afterEach(function () {
    expect(window.uiError || null).to.be.null
  });

  describe('Virkailija hakemus edit', function () {
    describe('shows correct link', function () {
      before(
        navigateToApplicationHandling,
        wait.until(directFormHakuListExists),
        function () {
          //clickElement doesn't work for a href here:
          form1OnList()[0].click()
        },
        wait.until(function () {
          return applicationHeader().text() === 'Selaintestilomake1'
        }),
        clickElement(applicationRow),
        wait.until(function () {
          return reviewHeader().length > 0
        })
      );

      it('shows virkailija edit link', function() {
        expect(editLink().attr('href')).to.equal('/lomake-editori/api/applications/application-key2/modify');
      })
    });
  });

  function editLink() {
    return testFrame().find('.application-handling__edit-link > a')
  }

  function directFormHakuList() {
    return testFrame().find('.application__search-control-direct-form-haku')
  }

  function applicationHeader() {
    return testFrame().find('.application-handling__header-haku-name')
  }

  function form1OnList() {
    return testFrame().find(".application__search-control-direct-form-haku a:contains(Selaintestilomake1)")
  }

  function directFormHakuListExists() {
    return elementExists(directFormHakuList())
  }

  function navigateToApplicationHandling() {
    loadInFrame('http://localhost:8350/lomake-editori/applications/')
  }

  function applicationRow() {
    return testFrame().find('.application-handling__list-row:not(.application-handling__list-header) > .application-handling__list-row--applicant:contains(Vatanen)')
  }

  function reviewHeader() {
    return testFrame().find('.application-handling__review-header')
  }
})();
