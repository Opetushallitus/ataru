(ns ataru.files.file-store-spec
  (:require [speclj.core :refer :all]
            [ataru.files.file-store :refer [extract-filename generate-filename]]))

(describe "extract-filename"
          (tags :unit)

          (it "extracts filename from simple Content-Disposition header"
              (should= "example.pdf"
                       (extract-filename "attachment; filename=\"example.pdf\"")))

          (it "returns nil for filename without quotes"
              (should= nil
                       (extract-filename "attachment; filename=image.png")))

          (it "return nil for filename with additional whitespace"
              (should= nil
                       (extract-filename "attachment;    filename=\"foo.txt\"")))

          (it "returns nil when Content-Disposition missing"
              (should= nil
                       (extract-filename nil)))

          (it "returns nil when header exists but does not match filename"
              (should= nil
                       (extract-filename "attachment; size=12345"))))


(describe "generate-filename"
          (tags :unit)

          (it "generates filename with no counter"
              (should= "myfile.pdf"
                       (generate-filename "myfile.pdf" "")))

          (it "generates filename with numeric counter"
              (should= "myfile1.pdf"
                       (generate-filename "myfile.pdf" 1)))

          (it "handles filenames with multiple dots"
              (should= "archive.backup.tar1.gz"
                       (generate-filename "archive.backup.tar.gz" 1)))

          (it "truncates long names to 240 chars"
              (let [long-name (apply str (repeat 300 "a"))
                    filename  (str long-name ".jpg")
                    result    (generate-filename filename "")]
                (should= 244 (count result)) ; 240 'a's + "." + "jpg"
                (should=
                 (apply str (repeat 240 "a"))
                 (.substring result 0 240))))

          (it "handles a filename with only one dot"
              (should= "file2.txt"
                       (generate-filename "file.txt" 2))))
