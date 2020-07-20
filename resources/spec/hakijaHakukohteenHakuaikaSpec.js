;(() => {
  before(() => {
    loadInFrame('/hakemus/hakukohde/1.2.246.562.20.49028100003')
  })

  afterEach(() => {
    expect(window.uiError || null).to.be.null
  })

  describe('hakutoiveita eri hakuajoilla', () => {
    describe('form loads', () => {
      before(
        wait.until(() => {
          return formSections().length == 2
        }),
        clickElement(addHakukohdeLink, 'hakukohdeLink'),
        clickElement(() => {
          return nthHakukohdeSearchResultButton(0)
        }, 'searchResult1'),
        setNthFieldInputValue(1, 'Etunimi Tokanimi'),
        blurField(() => {
          return formFields().eq(1).find('input')
        }),
        setNthFieldInputValue(3, 'Lastname'),
        setNthFieldInputValue(5, '020202A0202'),
        setNthFieldInputValue(6, 'test@example.com'),
        setFieldInputValue('#verify-email', 'test@example.com'),
        setNthFieldInputValue(7, '0123456789'),
        setNthFieldInputValue(9, 'Katutie 12 B'),
        setNthFieldInputValue(10, '40100'),
        setNthFieldOption(12, '179'),
        setNthFieldOption(13, 'FI'),
        setNthFieldInputValue(14, 'Jee'),
        wait.until(() => {
          return formFields().eq(11).find('input').val() !== ''
        }),
        wait.until(submitButtonEnabled),
        clickElement(() => {
          return submitButton()
        }),
        wait.until(() => {
          return (
            testFrame().find('.application__sent-placeholder-text').length == 1
          )
        })
      )
      const reloadEditPage = () => {
        return httpGet('/hakemus/latest-application-secret').then(
          (newSecret) => {
            console.log('Updated secret is ' + newSecret)
            return httpGet(
              '/hakemus/alter-application-to-hakuaikaloppu-for-secret/' +
                newSecret
            ).then((response) => {
              loadInFrame('/hakemus?modify=' + newSecret)
            })
          }
        )
      }
      before(
        reloadEditPage,
        wait.until(() => {
          return formSections().length == 2
        })
      )

      it('check that components are disabled when hakuaika is over (and enabled when some hakuaika is on going)', () => {
        expect(
          testFrame().find('.application__selected-hakukohde-row--remove')
            .length
        ).to.equal(2)
        expect(
          testFrame().find(
            '.application__selected-hakukohde-row--remove[disabled]'
          ).length
        ).to.equal(1)

        expect(testFrame().find('#hakuajat-ohi').prop('disabled')).to.equal(
          true
        )
        expect(
          testFrame().find('#osa-hakuajoista-ohi').prop('disabled')
        ).to.equal(false)
        expect(
          testFrame().find('#kaikki-hakuajat-voimassa').prop('disabled')
        ).to.equal(false)
        expect(
          testFrame()
            .find('#assosiaatio-hakukohderyhman-kautta')
            .prop('disabled')
        ).to.equal(false)

        const kysymysKoskeeHakukohteitaFinder = (id) => {
          return testFrame()
            .find(id)
            .parent()
            .find('.application__question_hakukohde_names_container')
            .text()
        }

        expect(kysymysKoskeeHakukohteitaFinder('#hakuajat-ohi')).to.equal(
          'Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (1)'
        )
        expect(
          kysymysKoskeeHakukohteitaFinder('#osa-hakuajoista-ohi')
        ).to.equal('Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (2)')
        expect(
          kysymysKoskeeHakukohteitaFinder('#kaikki-hakuajat-voimassa')
        ).to.equal('Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (1)')
        expect(
          kysymysKoskeeHakukohteitaFinder('#assosiaatio-hakukohderyhman-kautta')
        ).to.equal('Kysymys kuuluu hakukohteisiin: Näytä hakukohteet (1)')
      })
    })

    describe('priorisoivat hakukohderyhmat', () => {
      before(
        wait.until(() => {
          return addHakukohdeLink().length == 1
        }),
        clickElement(addHakukohdeLink, 'hakukohdeLink'),
        clickElement(() => {
          return nthHakukohdeSearchResultButton(1)
        }, 'searchResult2'),
        clickElement(invalidFieldsStatus),
        wait.until(submitButtonDisabled),
        wait.until(() => {
          return invalidFieldsStatus().text() === 'Tarkista 1 tietoa'
        })
      )

      it('doesnt allow to add hakutoive in wrong priority order', () => {
        expect(
          testFrame().find(
            '.application__selected-hakukohde-row--offending-priorization'
          ).length
        ).to.equal(2)
        expect(invalidFieldNames().join(';')).to.equal('Hakukohteet')
      })
      it('doesnt allow adding hakutoive when limit is reached', () => {
        expect(
          testFrame().find(
            '.application__search-hit-hakukohde-row--limit-reached'
          ).length
        ).to.equal(1)
      })
    })
  })
})()
