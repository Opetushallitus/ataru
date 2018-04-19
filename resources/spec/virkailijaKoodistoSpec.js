(function () {
    function addNewFormLink() {
        return testFrame().find('.editor-form__control-button--enabled')
    }

    function formTitleField () {
        return testFrame().find('.editor-form__form-name-input')
    }

    function formList() {
        return testFrame().find('.editor-form__list')
    }

    function formListItems(n) {
        if ($.isNumeric(n)) {
            return formList().find('a').eq(n)
        } else {
            return formList().find('a')
        }
    }

    function clickComponentMenuItem(title) {
        function menuItem() { return testFrame().find('.editor-form > .editor-form__add-component-toolbar a:contains("'+ title +'")') }
        return clickElement(menuItem)
    }

    function navigateToEditor() {
        var src = 'http://localhost:8350/lomake-editori/';
        console.log(src)
        loadInFrame(src)
    }

    function personInfoHeader() {
        return testFrame().find('.application__wrapper-heading h2:contains("Henkilötiedot")')
    }

    afterEach(function() {
        expect(window.uiError || null).to.be.null
    })

    function formComponents() {
        return testFrame().find('.editor-form__component-wrapper')
        // exclude followup question components
            .not('.editor-form__followup-question-overlay .editor-form__component-wrapper')
    }

    describe('Virkailija creates koodisto with lisäkysymys', function () {
        before(
            navigateToEditor,
            clickElement(addNewFormLink),
            wait.forMilliseconds(1000), // TODO: fix form refresh in frontend so that this isn't required (or check that no AJAX requests are ongoing)
            setTextFieldValue(formTitleField, 'Koodistotestilomake'),
            wait.until(function() {
                return formListItems(0).find('span:eq(0)').text() === 'Koodistotestilomake'
            }),
            clickComponentMenuItem('Pudotusvalikko'),
            clickElement(function() { return formComponents().eq(0).find('.editor-form__multi-options_wrapper label:contains("Koodisto")')}),
            clickElement(function() { return formComponents().eq(0).find('.editor-form__koodisto-popover a:contains("Pohjakoulutus")') }),
            clickElement(function() { return formComponents().eq(0).find('.editor-form__show-koodisto-values a:contains("vastausvaihtoehdot")') }),
            clickElement(function() { return formComponents().eq(0).find('.editor-form__followup-question:eq(0) a:contains("Lisäkysymykset")') }),
            clickElement(function() { return formComponents().eq(0).find('.editor-form__followup-question-overlay a:contains("Lista, monta valittavissa")') }),
            setTextFieldValue(function() { return formComponents().eq(0).find('.editor-form__followup-question-overlay input.editor-form__text-field') }, "Lisäkysymys koodistolle")
        )
        it('has lisäkymys in first option', function() {
            expect(formComponents().eq(0).find('.editor-form__followup-question-overlay .editor-form__text-field').eq(0).val())
                .to.equal('Lisäkysymys koodistolle');
        })
    })

})();
