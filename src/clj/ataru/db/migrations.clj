; This namespace contains minimal AOT compiled proxies for Flyway. Actual implementation is called using
; requiring-resolve. This is to prevent errors (at least random ClassNotFoundExceptions) that can come up
; when the codebase is partly AOT compiled.
(ns ataru.db.migrations
  (:import [org.flywaydb.core.api.migration.jdbc JdbcMigration]
           [org.flywaydb.core.api.migration MigrationInfoProvider]
           [org.flywaydb.core.api MigrationVersion]))

(defmacro defmigration [name version description & body]
  `(deftype ~name []
     JdbcMigration
     (migrate [~'this ~'connection]
       ~@body)

     MigrationInfoProvider
     (getDescription [~'this] ~description)
     (getVersion [~'this] (MigrationVersion/fromVersion ~version))))

(defmigration
  migrate-person-info-module "1.13"
  "Update person info module structure in existing forms"
  ((requiring-resolve 'ataru.db.migration-implementations/refresh-person-info-modules)))

(defmigration
  migrate-person-info-module "1.22"
  "Update person info module structure in existing forms"
  ((requiring-resolve 'ataru.db.migration-implementations/refresh-person-info-modules)))

(defmigration
  migrate-application-versioning "1.25"
  "Change references to applications.id to be references to applications.key"
  ((requiring-resolve 'ataru.db.migration-implementations/application-id->application-key)))

(defmigration
  migrate-application-secrets "1.28"
  "Add a secret key to each application in database"
  ((requiring-resolve 'ataru.db.migration-implementations/populate-application-secrets)))

(defmigration
  migrate-application-haku-ids "1.36"
  "Add haku oids to applications (from tarjonta-service) with hakukohde data"
  ((requiring-resolve 'ataru.db.migration-implementations/add-haku-details-for-applications)))

(defmigration
  migrate-followups-to-vectored-followups "1.38"
  "Wrap all existing followups with vector"
  ((requiring-resolve 'ataru.db.migration-implementations/followups-to-vectored-followups)))

(defmigration
  migrate-followups-to-vectored-followups "1.39"
  "Wrap all existing followups with vector, like really all of them ever."
  ((requiring-resolve 'ataru.db.migration-implementations/followups-to-vectored-followups-like-all-of-them)))

(defmigration
  migrate-application-reviews "1.64"
  "Migrate old per-application reviews to application + hakukohde specific ones"
  ((requiring-resolve 'ataru.db.migration-implementations/application-reviews->new-model)))

(defmigration
  migrate-birth-date-placeholders "1.70"
  "Add multi lang placeholder texts to birth date question"
  ((requiring-resolve 'ataru.db.migration-implementations/update-birth-date-place-holder)))

(defmigration
  migrate-dob-into-dd-mm-yyyy-format "1.71"
  "Update date of birth from application answers to dd.mm.yyyy format"
  ((requiring-resolve 'ataru.db.migration-implementations/dob->dd-mm-yyyy-format) connection))

(defmigration
  migrate-camel-case-content-keys "1.72"
  "Camel case application content keys"
  ((requiring-resolve 'ataru.db.migration-implementations/camel-case-content-keys)))

(defmigration
  migrate-person-info-module "1.74"
  "Update person info module structure in existing forms"
  ((requiring-resolve 'ataru.db.migration-implementations/refresh-person-info-modules)))

(defmigration
  migrate-person-info-module "1.75"
  "Update person info module structure in existing forms"
  ((requiring-resolve 'ataru.db.migration-implementations/refresh-person-info-modules)))

(defmigration
  migrate-application-review-notes-to-own-table "1.77"
  "Migrate application review notes to application_review_notes table"
  ((requiring-resolve 'ataru.db.migration-implementations/review-notes->own-table)))

(defmigration
  migrate-application-states-to-hakukohteet "1.80"
  "Move (most) application states to be hakukohde specific"
  ((requiring-resolve 'ataru.db.migration-implementations/application-states-to-hakukohteet)))

(defmigration
  migrate-start-attachment-finalizer-jobs "1.82"
  "Start attachment finalizer job for all applications"
  ((requiring-resolve 'ataru.db.migration-implementations/start-attachment-finalizer-job-for-all-applications) connection))

(defmigration
  migrate-kotikunta-from-text-to-a-code "1.86"
  "Migrate kotikunta from text to a code"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-kotikunta-from-text-to-code) connection))

(defmigration
  update-forms-metadata "1.88"
  "Migrate creator to form elements"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-element-metadata-to-forms) connection))

(defmigration
  migrate-legacy-forms-to-include-hakukohteet-module "1.90"
  "Migrate legacy form content to contain hakukohteet module"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-legacy-form-content-to-contain-hakukohteet-module) connection))

(defmigration
  update-forms-metadata "1.92"
  "Migrate attachment states for applications"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-attachment-states-to-applications) connection))

(defmigration
  add-subject-and-content-finish "1.96"
  "Migrate email templates to contain subject and finishing content"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-add-subject-and-content-finish)))

(defmigration
  migrate-person-info-module "1.100"
  "Add multiple nationality support to forms & applications"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-nationality-to-question-group) connection))

(defmigration
  add-harkinnanvaraisuus-checks "20230125094000"
  "Infinite harkinnanvaraisuus job that checks harkinnanvaraisuus of 2 asteen yhteishaun applikaatiot"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-add-harkinnanvaraisuus-checks) connection))

(defmigration
  add-harkinnanvaraisuus-rechecks "20230201150700"
  "Infinite harkinnanvaraisuus job that rechecks harkinnanvaraisuus of 2 asteen yhteishaun applikaatiot"
  ((requiring-resolve 'ataru.db.migration-implementations/migrate-add-harkinnanvaraisuus-rechecks) connection))