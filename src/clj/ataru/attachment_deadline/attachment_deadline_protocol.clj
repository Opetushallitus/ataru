(ns ataru.attachment-deadline.attachment-deadline-protocol)

(defprotocol AttachmentDeadlineServiceProtocol
  (get-field-deadlines [this application-key])
  (get-field-deadlines-authorized [_ organization-service tarjonta-service audit-logger session application-key])
  (get-attachment-deadline-days [this ohjausparametrit-service haku])
  (get-attachment-deadline-time [this ohjausparametrit-service haku])
  (attachment-deadline-for-hakuaika [this ohjausparametrit-service application-submitted haku hakuaika]))
