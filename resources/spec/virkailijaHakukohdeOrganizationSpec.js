(function() {
    function addNewFormLink() {
        return testFrame().find('.editor-form__control-button--enabled')
    }

    function formTitleField () {
        return testFrame().find('.editor-form__form-name-input')
    }

    function formList() {
        return testFrame().find('.editor-form__list')
    }

    function editorPageIsLoaded() {
        return elementExists(formList().find('a'))
    }

    function formListItems(n) {
        if ($.isNumeric(n)) {
            return formList().find('a').eq(n)
        } else {
            return formList().find('a')
        }
    }

    function personInfoModule() {
        return testFrame()
            .find(".editor-form__module-wrapper header:contains('Henkilötiedot')");
    }

    function formComponents() {
        return testFrame().find('.editor-form__component-wrapper')
        // exclude followup question components
            .not('.editor-form__followup-question-overlay .editor-form__component-wrapper')
    }

    function formSections() {
        return testFrame().find('.editor-form__section_wrapper')
    }

    function clickComponentMenuItem(title) {
        function menuItem() { return testFrame().find('.editor-form > .editor-form__add-component-toolbar a:contains("'+ title +'")') }
        return clickElement(menuItem)
    }

    function clickSubComponentMenuItem(title, element) {
        function menuItem() {
            return element().find('.editor-form__add-component-toolbar a:contains("'+ title +'")') }
        return clickElement(menuItem)
    }

    function clickRepeatingAnswers(question) {
        return function() {
            return testFrame()
                .find("input.editor-form__text-field")
                .filter(function() {
                    return this.value === question
                })
                .parent().parent().parent()
                .find(".editor-form__checkbox-wrapper label:contains('Vastaaja voi')")
                .prev().click()
        }
    }

    function clickNumericAnswer(question) {
        return function() {
            return testFrame()
                .find("input.editor-form__text-field")
                .filter(function() {
                    return this.value === question
                })
                .parent().parent().parent()
                .find(".editor-form__checkbox-wrapper label:contains('Kenttään voi täyttää vain numeroita')")
                .prev().click()
        }
    }


    function clickInfoTextCheckbox(selector) {
        return function() {
            return selector()
                .find(".editor-form__info-addon-checkbox > input")
                .click()
        }
    }

    before(function () {
        loadInFrame('http://localhost:8350/lomake-editori/auth/cas?ticket=USER-WITH-HAKUKOHDE-ORGANIZATION')
    });

    afterEach(function() {
        expect(window.uiError || null).to.be.null
    });

    describe('Editor when user associated by hakukohteen organization', function () {
        before(
            wait.until(editorPageIsLoaded, 10000),
            clickElement(function() { return formListItems(0)}),
            wait.forMilliseconds(1000), // TODO: fix form refresh in frontend so that this isn't required (or check that no AJAX requests are ongoing)
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
