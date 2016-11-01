-- name: yesql-get-application-secret
select secret from applications where id = :id;

-- name: yesql-set-application-secret!
-- Set secret to application
update applications set secret = :secret where id = :id;
