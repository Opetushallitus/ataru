(ns ataru.util.js-bundle-spec
  (:require [ataru.util.js-bundle :as js-bundle]
            [clojure.java.io :as io]
            [speclj.core :refer :all]))

(def ^:private sha-256-prefix #'ataru.util.js-bundle/sha-256-prefix)

(defn- temp-file-with [content]
  (let [file (java.io.File/createTempFile "ataru-bundle" ".js")]
    (.deleteOnExit file)
    (spit file content)
    file))

(describe "compiled bundle identity"
  (tags :unit)

  (it "identifies a build by the contents of the bundle"
      (let [id (sha-256-prefix (temp-file-with "goog.provide('x');"))]
        (should= 12 (count id))
        (should (re-matches #"[0-9a-f]{12}" id))
        (should= id (sha-256-prefix (temp-file-with "goog.provide('x');")))
        (should-not= id (sha-256-prefix (temp-file-with "goog.provide('y');")))))

  (it "does not look up resources outside the compiled bundle directory"
      (should-be-nil (js-bundle/resource "../../../etc/passwd"))
      (should-be-nil (js-bundle/resource "foo/bar.js"))
      (should-be-nil (js-bundle/resource "1.2.246.562.29.00000000000000101055"))
      (should-be-nil (js-bundle/resource nil)))

  (it "has no build id for a bundle that is not there"
      (should-be-nil (js-bundle/build-id "no-such-app.js"))
      (should-be-nil (js-bundle/build-id "../../../etc/passwd"))))
