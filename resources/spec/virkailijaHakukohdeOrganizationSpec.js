(function() {
    afterEach(function() {
        expect(window.uiError || null).to.be.null
    });

    function editorPageIsLoaded() {
        return elementExists(formList().find('a'))
    }

    describe('Editor when user associated by hakukohteen organization', function () {
        before(
            loadInFrame('http://localhost:8350/lomake-editori/auth/cas?ticket=USER-WITH-HAKUKOHDE-ORGANIZATION'),
            wait.until(editorPageIsLoaded, 10000),
            clickElement(function() { return formListItems(0)}),
            clickComponentMenuItem('Tekstikenttä'),
            setTextFieldValue(function() { return formComponents().eq(0).find('.editor-form__text-field') }, 'Ensimmäinen kysymys'),
            clickElement(function() { return formComponents().eq(0).find('.editor-form__info-addon-checkbox label') }),
            setTextFieldValue(function() { return formComponents().eq(0).find('.editor-form__info-addon-inputs textarea') }, 'Ensimmäisen kysymyksen ohjeteksti')
        );
        it('has 1 fixture forms', function () {
            expect(formListItems()).to.have.length(1)
            expect(formComponents()).to.have.length(1)
        });
    });

})();