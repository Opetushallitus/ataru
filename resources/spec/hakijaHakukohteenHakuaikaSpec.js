(function() {
    before(function () {
        loadInFrame('/hakemus/hakukohde/1.2.246.562.20.49028196523')
    })

    afterEach(function() {
        expect(window.uiError || null).to.be.null
    })

    describe('hakutoiveita eri hakuajoilla', function() {

        describe('form loads', function () {
            before(
                wait.until(function() { return formSections().length == 3 }, 10000),
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
                setNthFieldInputValue(11, 'Jyväskylä'),
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
                wait.until(function() { return formSections().length == 3 }, 10000)
            )

            it('reloads and edits hakutoiveita', function() {
                expect(hakukohdeTexts()).to.equal('Aikaloppu 4 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne DTestihakukohde 2 –\xa0Koulutuskeskus Sedu, Ilmajoki, IlmajoentieTarkenne B')
            })
        })


    })
})()
