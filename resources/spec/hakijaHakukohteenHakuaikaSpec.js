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
                clickElement(addHakukohdeLink, "hakukohdeLink"),
                clickElement(function() { return nthHakukohdeSearchResultButton(0) }, "searchResult1"),
                setNthFieldInputValue(1, 'Etunimi Tokanimi'),
                selectNthField(2),
                setNthFieldInputValue(3, 'Lastname'),
                setNthFieldInputValue(5, '020202A0202'),
                setNthFieldInputValue(6, 'test@example.com'),
                setNthFieldInputValue(7, '0123456789'),
                setNthFieldInputValue(9, 'Katutie 12 B'),
                setNthFieldInputValue(10, '40100'),
                setNthFieldOption(12, '179'),
                setNthFieldOption(13, 'FI'),
                setNthFieldInputValue(14, 'Jee'),
                wait.until(function() {
                    return formFields().eq(11).find('input').val() !== ''
                }),
                wait.until(submitButtonEnabled),
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
                expect(testFrame().find("#assosiaatio-hakukohderyhman-kautta").prop('disabled')).to.equal(false)

                var kysymysKoskeeHakukohteitaFinder = function(id) {
                    return testFrame().find(id).parent().siblings(".application__question_hakukohde_names_container").text()
                };

                expect(kysymysKoskeeHakukohteitaFinder("#hakuajat-ohi")).to.equal("Kysymys kuuluu hakukohteisiin (1)")
                expect(kysymysKoskeeHakukohteitaFinder("#osa-hakuajoista-ohi")).to.equal("Kysymys kuuluu hakukohteisiin (2)")
                expect(kysymysKoskeeHakukohteitaFinder("#kaikki-hakuajat-voimassa")).to.equal("Kysymys kuuluu hakukohteisiin (1)")
                expect(kysymysKoskeeHakukohteitaFinder("#assosiaatio-hakukohderyhman-kautta")).to.equal("Kysymys kuuluu hakukohteisiin (1)")
            })
        })


    })
})()
