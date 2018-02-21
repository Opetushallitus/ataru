(function() {
    before(function () {
        loadInFrame('/hakemus/hakukohde/1.2.246.562.20.49028100003')
    })

    afterEach(function() {
        expect(window.uiError || null).to.be.null
    })

    describe('hakutoiveita eri hakuajoilla', function() {

        describe('form loads', function () {
            before(
                wait.until(function() { return formSections().length == 2 }, 10000),
                clickElement(addHakukohdeLink),
                clickElement(function() { return nthHakukohdeSearchResultButton(1) }),
                setNthFieldInputValue(0, 'Etunimi Tokanimi'),
                selectNthField(1),
                setNthFieldInputValue(2, 'Lastname'),
                setNthFieldInputValue(4, '020202A0202'),
                setNthFieldInputValue(5, 'test@example.com'),
                setNthFieldInputValue(6, '0123456789'),
                setNthFieldInputValue(8, 'Katutie 12 B'),
                setNthFieldInputValue(9, '40100'),
                setNthFieldOption(11, '179'),
                setNthFieldOption(12, 'FI'),
                setNthFieldInputValue(13, 'Jee'),
                wait.until(function() {
                    return formFields().eq(10).find('input').val() !== ''
                }),
                clickElement(function() { return submitButton() }),
                wait.until(function() {
                    return testFrame().find('.application__sent-placeholder-text').length == 1
                })
            )
            var reloadEditPage = function() {
                return httpGet('/hakemus/latest-application-secret').then(function(newSecret) {
                    console.log("Updated secret is " + newSecret)
                    return httpGet('/hakemus/alter-application-to-hakuaikaloppu-for-secret/' + newSecret).then(function(response) {
                        loadInFrame('/hakemus?modify=' + newSecret)
                    })
                })
            }
            before(
                reloadEditPage,
                wait.until(function() { return formSections().length == 2 }, 10000)
            )

            it('check that components are disabled when hakuaika is over (and enabled when some hakuaika is on going)', function() {
                expect(testFrame().find('.application__hakukohde-row-button-container').length).to.equal(2)
                expect(testFrame().find('.application__hakukohde-row-button-container[disabled]').length).to.equal(1)

                expect(testFrame().find("#hakuajat-ohi").prop('disabled')).to.equal(true)
                expect(testFrame().find("#osa-hakuajoista-ohi").prop('disabled')).to.equal(false)
                expect(testFrame().find("#kaikki-hakuajat-voimassa").prop('disabled')).to.equal(false)

            })
        })


    })
})()
