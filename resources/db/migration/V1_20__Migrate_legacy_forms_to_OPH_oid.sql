update forms set organization_oid = '1.2.246.562.10.00000000001'
  where key like '%-system-generated-key' and organization_oid is null;
