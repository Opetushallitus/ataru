CREATE VIEW latest_applications_new AS
SELECT a.*
FROM applications AS a
         LEFT JOIN applications AS newer_a ON a.key = newer_a.key AND newer_a.id > a.id
where newer_a.id IS NULL
