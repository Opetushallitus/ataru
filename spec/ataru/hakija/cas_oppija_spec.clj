(ns ataru.hakija.cas-oppija-spec
  (:require [speclj.core :refer [describe it should=]]
            [ataru.cas-oppija.cas-oppija-utils :as cas-oppija-utils]))

(def successful-response-eidas "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
  <cas:authenticationSuccess>
  <cas:user>suomi.fi,asdfgXXXXXYYYYZZZZZ=</cas:user>
  <cas:attributes>
  <cas:firstName>Leon Elias</cas:firstName>
  <cas:clientName>suomi.fi</cas:clientName>
  <cas:vtjVerified>false</cas:vtjVerified>
  <cas:familyName>Germany</cas:familyName>
  <cas:notOnOrAfter>2023-11-06T13:34:26.733Z</cas:notOnOrAfter>
  <cas:personIdentifier>DE/FI/166193B0E55D436B494769486A9284D04E0A1XXXXXF8B9EDA63E5BF4C3CFE6F5</cas:personIdentifier>
  <cas:dateOfBirth>1981-02-06</cas:dateOfBirth>
  <cas:notBefore>2023-11-06T13:29:26.733Z</cas:notBefore>
  </cas:attributes>
  </cas:authenticationSuccess>
  </cas:serviceResponse>")

(def successful-response-strong "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
  <cas:authenticationSuccess>
  <cas:user>suomi.fi,210281-9988</cas:user>
  <cas:attributes>
  <cas:clientName>suomi.fi</cas:clientName>
  <cas:displayName>Nordea Demo</cas:displayName>
  <cas:givenName>Nordea</cas:givenName>
  <cas:VakinainenKotimainenLahiosoitePostitoimipaikkaR>ÅBO</cas:VakinainenKotimainenLahiosoitePostitoimipaikkaR>
  <cas:VakinainenKotimainenLahiosoiteS>Mansikkatie 11</cas:VakinainenKotimainenLahiosoiteS>
  <cas:VakinainenKotimainenLahiosoitePostitoimipaikkaS>TURKU</cas:VakinainenKotimainenLahiosoitePostitoimipaikkaS>
  <cas:VakinainenKotimainenLahiosoiteR>Smultronvägen 11</cas:VakinainenKotimainenLahiosoiteR>
  <cas:cn>Demo Nordea</cas:cn>
  <cas:notBefore>2023-11-06T13:08:09.546Z</cas:notBefore>
  <cas:personOid>1.2.246.562.24.73833272757</cas:personOid>
  <cas:firstName>Nordea</cas:firstName>
  <cas:VakinainenKotimainenLahiosoitePostinumero>20006</cas:VakinainenKotimainenLahiosoitePostinumero>
  <cas:KotikuntaKuntanumero>853</cas:KotikuntaKuntanumero>
  <cas:vtjVerified>true</cas:vtjVerified>
  <cas:KotikuntaKuntaS>Turku</cas:KotikuntaKuntaS>
  <cas:notOnOrAfter>2023-11-06T13:13:09.546Z</cas:notOnOrAfter>
  <cas:KotikuntaKuntaR>Åbo</cas:KotikuntaKuntaR>
  <cas:sn>Demo</cas:sn>
  <cas:nationalIdentificationNumber>210281-9988</cas:nationalIdentificationNumber>
  </cas:attributes>
  </cas:authenticationSuccess>
  </cas:serviceResponse>")

(def successful-response-strong-no-kutsumanimi "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
  <cas:authenticationSuccess>
  <cas:user>suomi.fi,210281-9988</cas:user>
  <cas:attributes>
  <cas:clientName>suomi.fi</cas:clientName>
  <cas:displayName>Nordea Demo</cas:displayName>
  <cas:VakinainenKotimainenLahiosoitePostitoimipaikkaR>ÅBO</cas:VakinainenKotimainenLahiosoitePostitoimipaikkaR>
  <cas:VakinainenKotimainenLahiosoiteS>Mansikkatie 11</cas:VakinainenKotimainenLahiosoiteS>
  <cas:VakinainenKotimainenLahiosoitePostitoimipaikkaS>TURKU</cas:VakinainenKotimainenLahiosoitePostitoimipaikkaS>
  <cas:VakinainenKotimainenLahiosoiteR>Smultronvägen 11</cas:VakinainenKotimainenLahiosoiteR>
  <cas:cn>Demo Nordea</cas:cn>
  <cas:notBefore>2023-11-06T13:08:09.546Z</cas:notBefore>
  <cas:personOid>1.2.246.562.24.73833272757</cas:personOid>
  <cas:firstName>Nordea</cas:firstName>
  <cas:VakinainenKotimainenLahiosoitePostinumero>20006</cas:VakinainenKotimainenLahiosoitePostinumero>
  <cas:KotikuntaKuntanumero>853</cas:KotikuntaKuntanumero>
  <cas:vtjVerified>true</cas:vtjVerified>
  <cas:KotikuntaKuntaS>Turku</cas:KotikuntaKuntaS>
  <cas:notOnOrAfter>2023-11-06T13:13:09.546Z</cas:notOnOrAfter>
  <cas:KotikuntaKuntaR>Åbo</cas:KotikuntaKuntaR>
  <cas:sn>Demo</cas:sn>
  <cas:nationalIdentificationNumber>210281-9988</cas:nationalIdentificationNumber>
  </cas:attributes>
  </cas:authenticationSuccess>
  </cas:serviceResponse>")

(def successful-response-strong-invalid-kutsumanimi
  "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
  <cas:authenticationSuccess>
  <cas:user>suomi.fi,210281-9988</cas:user>
  <cas:attributes>
  <cas:clientName>suomi.fi</cas:clientName>
  <cas:displayName>Nordea Demo</cas:displayName>
  <cas:givenName>Hyperborea</cas:givenName>
  <cas:cn>Demo Nordea</cas:cn>
  <cas:notBefore>2023-11-06T13:08:09.546Z</cas:notBefore>
  <cas:firstName>Nordea</cas:firstName>
  <cas:vtjVerified>true</cas:vtjVerified>
  <cas:sn>Demo</cas:sn>
  <cas:nationalIdentificationNumber>210281-9988</cas:nationalIdentificationNumber>
  </cas:attributes>
  </cas:authenticationSuccess>
  </cas:serviceResponse>")

(def failed-response "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n<cas:authenticationFailure>\n</cas:authenticationFailure>\n</cas:serviceResponse>")

(describe "parse oppija attributes"
          (describe "correctly when all is ok"
                    (it "should parse strong-auth attributes"
                        (let [parsed-attributes (cas-oppija-utils/parse-oppija-attributes-if-successful successful-response-strong)]
                          (should= {:person-oid "1.2.246.562.24.73833272757",
                                    :eidas-id nil,
                                    :auth-type :strong,
                                    :display-name "Nordea",
                                    :fields {:address {:value "Mansikkatie 11", :locked false},
                                             :have-finnish-ssn {:value true, :locked true},
                                             :email {:value nil, :locked false},
                                             :preferred-name {:value "Nordea", :locked true},
                                             :last-name {:value "Demo", :locked true},
                                             :country-of-residence {:value "246", :locked false},
                                             :ssn {:value "210281-9988", :locked true},
                                             :first-name {:value "Nordea", :locked true},
                                             :birth-date {:value nil, :locked false},
                                             :postal-code {:value "20006", :locked false},
                                             :home-town {:value "853", :locked true}}}
                                   parsed-attributes)))
                    (it "should parse eidas attributes"
                        (let [parsed-attributes (cas-oppija-utils/parse-oppija-attributes-if-successful successful-response-eidas)]
                          (should= "DE/FI/166193B0E55D436B494769486A9284D04E0A1XXXXXF8B9EDA63E5BF4C3CFE6F5" (:eidas-id parsed-attributes))
                          (should= :eidas (:auth-type parsed-attributes))
                          (should= "Leon" (:display-name parsed-attributes))
                          (should= {:value "Leon Elias" :locked true} (get-in parsed-attributes [:fields :first-name]))
                          (should= {:value "Germany" :locked true} (get-in parsed-attributes [:fields :last-name]))
                          (should= {:value "06.02.1981" :locked true} (get-in parsed-attributes [:fields :birth-date]))
                          (should= {:value false :locked false} (get-in parsed-attributes [:fields :have-finnish-ssn]))))
                    (it "should not lock preferred-name field if attributes do not contain preferred name (givenName)"
                        (let [parsed-attributes (cas-oppija-utils/parse-oppija-attributes-if-successful successful-response-strong-no-kutsumanimi)]
                          (should= {:person-oid "1.2.246.562.24.73833272757",
                                    :eidas-id nil,
                                    :auth-type :strong,
                                    :display-name "Nordea",
                                    :fields {:address {:value "Mansikkatie 11", :locked false},
                                             :have-finnish-ssn {:value true, :locked true},
                                             :email {:value nil, :locked false},
                                             :preferred-name {:value nil, :locked false},
                                             :last-name {:value "Demo", :locked true},
                                             :country-of-residence {:value "246", :locked false},
                                             :ssn {:value "210281-9988", :locked true},
                                             :first-name {:value "Nordea", :locked true},
                                             :birth-date {:value nil, :locked false},
                                             :postal-code {:value "20006", :locked false},
                                             :home-town {:value "853", :locked true}}}
                                   parsed-attributes)))
                    (it "should not lock preferred-name field if kutsumanimi is invalid (not one of first-names)"
                        (let [parsed-attributes (cas-oppija-utils/parse-oppija-attributes-if-successful successful-response-strong-invalid-kutsumanimi)]
                          (should= {:person-oid nil,
                                    :eidas-id nil,
                                    :auth-type :strong,
                                    :display-name "Hyperborea",
                                    :fields {:address {:value nil, :locked false}
                                             :have-finnish-ssn {:value true, :locked true},
                                             :email {:value nil, :locked false},
                                             :preferred-name {:value nil, :locked false},
                                             :last-name {:value "Demo", :locked true},
                                             :country-of-residence {:value "246", :locked false}
                                             :ssn {:value "210281-9988", :locked true},
                                             :first-name {:value "Nordea", :locked true},
                                             :birth-date {:value nil, :locked false}
                                             :postal-code {:value nil, :locked false}
                                             :home-town {:value nil, :locked false}}}
                                   parsed-attributes))))

          (describe "with empty result when serviceResponse was not successful"
                    (it "should not parse eidas attributes"
                        (let [parsed-attributes (cas-oppija-utils/parse-oppija-attributes-if-successful failed-response)]
                          (should= nil parsed-attributes)))))

