(ns ataru.component-data.base-education-module-2nd
  (:require [ataru.util :as util]
    [ataru.component-data.component :as component :refer [harkinnanvaraisuus-wrapper-id]]))

(defn- base-education-language-question
  [metadata]
  (merge (component/dropdown metadata)
    {:label
     {:fi "Millä opetuskielellä olet suorittanut perusopetuksen?",
      :sv "På vilket språk har du avlagt grundutbildningen?"},
     :validators ["required"],
     :options
     [{:label {:fi "suomi", :sv "finska"}, :value "0"}
      {:label {:fi "ruotsi", :sv "svenska"}, :value "1"}
      {:label {:fi "saame", :sv "samiska"}, :value "2"}
      {:label {:fi "englanti", :sv "engelska"}, :value "3"}
      {:label {:fi "saksa", :sv "tyska"}, :value "4"}]}))

(defn- suorittanut-tutkinnon-question
  [metadata]
  (merge (component/single-choice-button metadata)
  {:label
   {:fi
    "Oletko suorittanut Suomessa tai ulkomailla ammatillisen tutkinnon, lukion oppimäärän tai korkeakoulututkinnon?",
    :sv
    "Har du avlagt en yrkesutbildning, gymnasiets lärokurs eller en högskoleexamen i Finland eller utomlands?"},
   :validators ["required" "invalid-values"],
   :params {:invalid-values ["0"]},
   :options
   [{:label {:fi "Kyllä", :sv "Ja"},
     :value "0",
     :followups
     [{:label {:fi ""},
       :text
       {:fi
        "Koska olet suorittanut ammatillisen tutkinnon, lukion oppimäärän tai korkeakoulututkinnon, et voi hakea perusopetuksen jälkeisen koulutuksen yhteishaussa. \n",
        :sv
        "Eftersom du redan har avlagt en yrkesinriktad examen, gymnasiets lärokurs eller en högskoleexamen kan du inte söka i gemensamma ansökan.\n\n"},
       :button-text {:fi "Sulje", :sv "Stäng"},
       :fieldClass "modalInfoElement",
       :id (util/component-id),
       :params {},
       :fieldType "p"
       :metadata metadata}]}
    {:label {:fi "Ei", :sv "Nej"} :value "1"}]}))

(defn- jos-olet-suorittanut-question
  [metadata]
  (merge (component/single-choice-button metadata)
  {:label
   {:fi "Jos olet suorittanut jonkun seuraavista, valitse koulutus",
    :sv "Om du har avlagt någon av följande, välj utbildning"},
   :options
   [{:label
     {:fi
      "Kymppiluokka (perusopetuksen lisäopetus, vähintään 1100 tuntia) ",
      :sv
      "Tionde klassen (den grundläggande utbildningens påbyggnadsundervisning, minst 1 100 timmar)"},
     :value "0",
     :followups
     [{:label
       {:fi "Suoritusvuosi",
        :sv "År då undervisningen har avlagts"},
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"}
       :metadata metadata
       :fieldType "textField"}]}
    {:label
     {:fi
      "Ammatilliseen koulutukseen valmentava koulutus VALMA (vähintään 30 osaamispistettä)",
      :sv
      "Utbildning som handleder för yrkesutbildning (VALMA) (minst 30 kompetenspoäng)"},
     :value "1",
     :followups
     [{:label
       {:fi "Suoritusvuosi",
        :sv "År då utbildningen har avlagts"},
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}
    {:label
     {:fi
      "Maahanmuuttajien lukiokoulutukseen valmistava koulutus LUVA (vähintään 25 kurssia)",
      :sv
      "Utbildning som förbereder för gymnasieutbildning som ordnas för invandrare (minst 25 kurser)"},
     :value "2",
     :followups
     [{:label
       {:fi "Suoritusvuosi",
        :sv "År då utbildningen har avlagts"},
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}
    {:label
     {:fi
      "Kansanopiston lukuvuoden mittainen linja (vähintään 28 opiskelijaviikkoa)",
      :sv
      "Ett år lång studielinje vid en folkhögskola (minst 28 studerandeveckor)"},
     :value "3",
     :followups
     [{:label
       {:fi "Suoritusvuosi",
        :sv "År då studierna har avlagts"},
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}
    {:label
     {:fi
      "Oppivelvollisille suunnattu vapaan sivistystyön koulutus (vähintään 17 opiskelijaviikkoa)",
      :sv
      "Utbildning inom det fria bildningsarbetet som riktar sig till läropliktiga (minst 17 studerandeveckor)"},
     :value "4",
     :followups
     [{:label
       {:fi "Suoritusvuosi",
        :sv "År då utbildningen har avlagts"},
       :validators ["required"],
       :fieldClass "formField",
       :id (util/component-id)
       :params {:size "S"},
       :metadata metadata
       :fieldType "textField"}]}]}))

(defn- suoritusvuosi-question
  [metadata]
  (merge (component/text-field metadata)
  {:validators ["numeric" "required"],
   :label {:fi "Suoritusvuosi",
           :sv "År för avläggande av den grundläggande utbildningen"},
   :params {:size "S",
            :max-value "2022",
            :numeric true,
            :min-value "2000"},                             ; yhdessä kysymyksessä tämän arvo oli 1990
   :section-visibility-conditions
   [{:section-name "arvosanat-peruskoulu",
     :condition
     {:comparison-operator ">", :answer-compared-to 2017}}],
   :options
   [{:label {:fi "", :sv ""},
     :value "0",
     :followups
     [(suorittanut-tutkinnon-question metadata)
      (jos-olet-suorittanut-question metadata)]
     :condition {:comparison-operator "<", :answer-compared-to 2022}}]}))

(defn- yksilollistetty-question
  [metadata id]
  (merge (component/single-choice-button metadata)
  {
   :label {:fi "Oletko opiskellut sekä matematiikan että äidinkielen yksilöllistetyn oppimäärän mukaisesti?",
           :sv "Har du studerat både matematik och modersmål enligt individualiserad lärokurs?"}
   :validators ["required"],
   :params {
            :info-text {
                        :label {:fi "Jos sinulla on jo perusopetuksen päättötodistus, yksilöllistettyjen oppiaineiden arvosanat on merkitty tähdellä (*). Jos et ole vielä saanut päättötodistusta, voit kysyä neuvoa opinto-ohjaajalta.",
                                :sv "Om du redan har den grundläggande utbildningens avgångsbetyg, är vitsorden för individualiserade lärokurser märkt med en stjärna (*). Om du inte ännu har fått avgångsbetyget, kan du be elevhandledaren om råd.",
                                :en ""}}},
   :id id,
   :section-visibility-conditions
   [{:section-name harkinnanvaraisuus-wrapper-id,
     :condition
     {:comparison-operator "=",
      :data-type "str",
      :answer-compared-to "1"}}],
   :options
   [{:label {:fi "Ei", :sv "Nej"}, :value "0"}
    {:label {:fi "Kyllä", :sv "Ja"},
     :value "1",
     :followups
     [(merge (component/info-element metadata)
             {:text {:fi
                      "Koska olet opiskellut sekä matematiikan että äidinkielen yksilöllistetyn oppimäärän mukaisesti, hakemuksesi käsitellään harkinnanvaraisesti, jos haet ammatilliseen koulutukseen tai lukioon. \n\nJos haluat lähettää liitteitä harkinnanvaraisen haun tueksi, tarkista palautusosoite oppilaitoksista, joihin haet. ",
                     :sv
                      "Eftersom du har studerat både matematik och modersmål enligt individualiserad lärokurs, behandlas din ansökan via antagning enligt prövning om du söker till yrkesutbildning eller till gymnasium.\n\nOm du vill skicka in bilagor som stöd för din ansökan via prövning, kontrollera leveransadressen från de läroanstalter som du söker till."}
              })]}]}))

(defn- ulkomailla-harkinnanvarainen-info
  [metadata]
  (merge (component/info-element metadata)
         {:text {:fi "Koska olet suorittanut tutkintosi ulkomailla, haet automaattisesti harkintaan perustuvassa valinnassa. Toimitathan kopion tutkintotodistuksestasi oppilaitoksiin.",
                 :sv "Eftersom du har avlagt din examen utomlands, söker du automatiskt via antagning enligt prövning. Skicka en kopia av ditt examensbetyg till läroanstalterna."}}))

(defn- kopio-tutkintotodistuksesta-attachment
  [metadata]
  (merge (component/attachment metadata)
  {:label
   {:fi "Kopio tutkintotodistuksesta ",
    :sv "Kopia av examensbetyget"},
   :validators [],
   :params {:deadline "29.3.2022 15:00",
            :mail-attachment? false,
            :info-text {:enabled? true,
                        :value {:fi "Tallenna tutkintotodistuksesi joko pdf-muodossa tai kuvatiedostona (esim. png tai jpeg). \n\n",
                                :sv "Spara ditt examensbetyg antingen i pdf-format eller som bildfil (t.ex. png eller jpeg)."}}}}))

(defn- kopio-todistuksesta-attachment
  [metadata]
  (merge (component/attachment metadata)
         {:label {:fi "Todistus, jolla haet "
                  :sv "Betyg som du söker med"},
          :params {:deadline "29.3.2022 15:00",
                   :info-text {:enabled? true,
                               :value {:fi "Tallenna todistuksesi joko pdf-muodossa tai kuvatiedostona (esim. png tai jpeg). ",
                                       :sv "Spara ditt betyg antingen i pdf-format eller som bildfil (t.ex. png eller jpeg)."}}}}))

(defn- ei-paattotodistusta-info
  [metadata]
  (assoc (component/info-element metadata)
         :text {:fi "Valitse tämä vain silloin, kun olet keskeyttänyt perusopetuksen. \n\nHaet automaattisesti harkintaan perustuvassa valinnassa. ",
                :sv "Välj den här endast om du har avbrutit den grundläggande utbildningen.\n\nDu söker automatiskt via antagning enligt prövning."}))

(defn- perusopetus-option
  [metadata]
  {:label
   {:fi "Perusopetuksen oppimäärä",
    :sv "Den grundläggande utbildningens lärokurs"},
   :value "1",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)]})

(defn- perusopetuksen-osittain-yksilollistetty-option
  [metadata]
  {:label
   {:fi "Perusopetuksen osittain yksilöllistetty oppimäärä",
    :sv
    "Delvis individualiserad lärokurs inom den grundläggande utbildningen"},
   :value "2",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)
    (yksilollistetty-question metadata "matematiikka-ja-aidinkieli-yksilollistetty_1")]})

(defn- perusopetuksen-yksilollistetty-option
  [metadata]
  {:label
   {:fi
    "Perusopetuksen pääosin tai kokonaan yksilöllistetty oppimäärä",
    :sv
    "Helt eller i huvudsak individualiserad lärokurs inom den grundläggande utbildningen"},
   :value "6",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)
    (yksilollistetty-question metadata "matematiikka-ja-aidinkieli-yksilollistetty_2")]})

(defn- opetus-jarjestetty-toiminta-alueittan-option
  [metadata]
  {:label
   {:fi
    "Perusopetuksen yksilöllistetty oppimäärä, opetus järjestetty toiminta-alueittain",
    :sv
    "Individualiserad lärokurs inom den grundläggande utbildningen, som utgår från verksamhetsområden"},
   :value "3",
   :followups
   [(base-education-language-question metadata)
    (suoritusvuosi-question metadata)]})

(defn- ulkomailla-suoritettu-option
  [metadata]
  {:label
   {:fi "Ulkomailla suoritettu koulutus",
    :sv "Utbildning utomlands"},
   :value "0",
   :followups
   [(ulkomailla-harkinnanvarainen-info metadata)
    (suorittanut-tutkinnon-question metadata)
    (kopio-tutkintotodistuksesta-attachment metadata)]})

(defn- ei-paattotodistusta-option
  [metadata]
  {:label {:fi "Ei päättötodistusta", :sv "Inget avgångsbetyg"},
   :value "7",
   :followups
   [(ei-paattotodistusta-info metadata)
    (suorittanut-tutkinnon-question metadata)
    (kopio-todistuksesta-attachment metadata)]})

(defn- base-education-question
  [metadata]
  (assoc (component/single-choice-button metadata)
    :id "base-education-2nd"
    :label {:fi "Valitse yksi pohjakoulutus, jolla haet koulutukseen",
            :sv "Välj den grundutbildning med vilken du söker till utbildningen"}
    :koodisto-source {
                      :uri "2asteenpohjakoulutus2021"
                      :version 1
                      :title "2. asteen pohjakoulutus (2021)"
                      :allow-invalid? false}
    :koodisto-ordered-by-user true
    :validators ["required"]
    :params
     {:info-text
      {:label
       {:fi
        "Jos saat perusopetuksen päättötodistuksen tänä keväänä (olet ysiluokkalainen), valitse se oppimäärä, jonka perusteella suoritat perusopetusta. \n \n\nJos sinulla on ainoastaan ulkomailla suoritettu koulutus, niin valitse Ulkomailla suoritettu koulutus. Perusopetuksen oppimäärällä tarkoitetaan Suomessa suoritettua tai suoritettavaa oppimäärää. \n\n\n\n\n",
        :sv
        "Om du får avgångsbetyg från den grundläggande utbildningen den här våren (du går på nian), välj den lärokurs med vilken du avlägger din grundutbildning.\n\n\nOm du endast har en utbildning som du avlagt utomlands, välj då ”Utbildning utomlands”. Den grundläggande utbildningens lärokurs betyder en lärokurs som du avlagt eller avlägger i Finland.",
        :en ""}}}
    :section-visibility-conditions
      [{:section-name harkinnanvaraisuus-wrapper-id
        :condition
        {:comparison-operator "=",
         :data-type "str",
         :answer-compared-to "0"}}
       {:section-name harkinnanvaraisuus-wrapper-id
        :condition
        {:comparison-operator "=",
         :data-type "str",
         :answer-compared-to "7"}}]
      :options [(perusopetus-option metadata)
                (perusopetuksen-osittain-yksilollistetty-option metadata)
                (perusopetuksen-yksilollistetty-option metadata)
                (opetus-jarjestetty-toiminta-alueittan-option metadata)
                (ulkomailla-suoritettu-option metadata)
                (ei-paattotodistusta-option metadata)]
    ))

(defn base-education-2nd-module [metadata]
  (assoc (component/form-section metadata)
         :id "pohjakoulutus-2nd-wrapper"
         :label {:fi "Pohjakoulutuksesi" :sv "Grundutbildning"}
         :children [(base-education-question metadata)]))

