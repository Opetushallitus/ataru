(ns ataru.middleware.filename-normalizer-middleware-spec
  (:require [ataru.fixtures.string-normalizer-test-fixtures :as fixtures]
            [ataru.middleware.filename-normalizer-middleware :as normalizer]
            [speclj.core :refer :all]))

(defn- ->multipart-params-happy-path-test [{expected :expected
                                            input    :input}]
  (it (format "normalizes filename \"%s\" to \"%s\" from a multipart request" input expected)
    (let [request-before-mw {:multipart-params {"file-part" {:filename input :other-param "other-value"}}}
          request-after-mw  ((normalizer/wrap-multipart-filename-normalizer identity) request-before-mw)]
      (should= {:multipart-params {"file-part" {:filename expected :other-param "other-value"}}}
               request-after-mw))))

(defn- ->query-params-happy-path-test [{expected :expected
                                        input    :input}]
  (it (format "normalizes filename \"%s\" to \"%s\" from a request with query params" input expected)
    (let [request-before-mw {:query-params {"file-name" input "other-param" "other-value"}}
          request-after-mw  ((normalizer/wrap-query-params-filename-normalizer identity) request-before-mw)]
      (should= {:query-params {"file-name" expected "other-param" "other-value"}}
               request-after-mw))))

(describe "filename-normalizer-middleware/wrap-multipart-filename-normalizer"
  (tags :unit)

  (describe "successfully normalizing a multipart request with file"
    (map ->multipart-params-happy-path-test fixtures/string-normalizer-fixtures))

  (describe "wrapping around a request without a file part inside multipart params"
    (it "should not attempt to normalize a non-existing filename"
      (let [response-after-mw ((normalizer/wrap-multipart-filename-normalizer identity) {:body {:lol :bal}})]
        (should= {:body {:lol :bal}}
                 response-after-mw)))))

(describe "filename-normalizer-middleware/wrap-query-params-filename-normalizer"
  (tags :unit)

  (describe "successfully normalizing a request with filename in query params"
    (map ->query-params-happy-path-test fixtures/string-normalizer-fixtures))

  (describe "wrapping around a request without filename as query param"
    (it "should not attempt to normalize a non-existing filename"
      (let [response-after-mw ((normalizer/wrap-query-params-filename-normalizer identity) {:body         {:lol :bal}
                                                                                            :query-params {"foo" "bar"}})]
        (should= {:body         {:lol :bal}
                  :query-params {"foo" "bar"}}
                 response-after-mw)))))
