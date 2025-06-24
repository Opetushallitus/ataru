(ns ataru.attachment-deadline.attachment-deadline-protocol)

(defprotocol AttachmentDeadlineServiceProtocol
  (get-field-deadlines [this application-key])
  (get-field-deadlines-authorized [_ organization-service tarjonta-service audit-logger session application-key])
  (attachment-deadline-for-hakuaika [this application-submitted haku hakuaika]))
