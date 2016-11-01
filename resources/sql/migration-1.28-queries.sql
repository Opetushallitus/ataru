-- name: yesql-get-application-secret
select secret from applications where id = :id;
