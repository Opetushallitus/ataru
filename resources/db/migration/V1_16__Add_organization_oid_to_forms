-- Add the link to organization which owns this form

alter table forms add column organization_oid varchar(50);
comment on column forms.organization_oid is 'User who created this form originally had this organization OID';
create index forms_organization_oid_idx on forms (organization_oid);
