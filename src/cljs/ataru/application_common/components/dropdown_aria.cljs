(ns ataru.application-common.components.dropdown-aria
  "Auki olevan pudotusvalikon taustasisällön piilottaminen ruudunlukijoilta.

  Popup renderöidään Reactin portaalilla document.bodyn loppuun (ks.
  dropdown-component), joten DOM-järjestyksessä vaihtoehdot ovat koko sivun
  viimeisinä. Kosketusnäytön ruudunlukijat (esim. iOS VoiceOver) eivät
  käytä syötekentän aria-activedescendantia, vaan siirtyvät pyyhkäisemällä
  DOM-järjestyksessä seuraavaan elementtiin — ilman tätä pyyhkäisy
  syötekentästä eteenpäin vie lomakkeen seuraavaan kenttään eikä koskaan
  valikon vaihtoehtoihin. Kun muu sivu piilotetaan aria-hiddenillä valikon
  ollessa auki, syötekentän jälkeen seuraavat saavutettavat elementit ovat
  suoraan popupin vaihtoehdot.

  Piilotetaan vain polun (juurielementti -> body) sisarukset, ei koskaan
  polun elementtejä itseään, jotta syötekenttä ja sen label pysyvät
  saavutettavina. Alkuperäiset aria-hidden-arvot palautetaan suljettaessa.")

(defn- keep-visible? [el portal-container]
  (or (= el portal-container)
      (#{"SCRIPT" "STYLE" "TEMPLATE"} (.-tagName el))
      ;; Live-alueiden ilmoitukset (esim. virheilmoitukset) eivät saa mykistyä.
      (.hasAttribute el "aria-live")))

(defn- hide-others! [root portal-container]
  (loop [node     root
         restores []]
    (let [parent (.-parentElement node)]
      (if (or (nil? parent) (= node (.-body js/document)))
        restores
        (recur parent
               (into restores
                     (for [sibling (array-seq (.-children parent))
                           :when   (and (not= sibling node)
                                        (not (keep-visible? sibling portal-container)))]
                       (let [previous (.getAttribute sibling "aria-hidden")]
                         (.setAttribute sibling "aria-hidden" "true")
                         [sibling previous]))))))))

(defn- restore! [restores]
  (doseq [[el previous] restores]
    (if (some? previous)
      (.setAttribute el "aria-hidden" previous)
      (.removeAttribute el "aria-hidden"))))

(defn hide-background!
  "Idempotentti: kutsutaan jokaisella renderöinnillä valikon ollessa auki."
  [{:keys [root-ref portal-container hidden-background]}]
  (when (nil? @hidden-background)
    (when-let [root @root-ref]
      (reset! hidden-background (hide-others! root @portal-container)))))

(defn restore-background!
  "Idempotentti: kutsutaan jokaisella renderöinnillä valikon ollessa kiinni."
  [{:keys [hidden-background]}]
  (when-let [restores @hidden-background]
    (restore! restores)
    (reset! hidden-background nil)))
