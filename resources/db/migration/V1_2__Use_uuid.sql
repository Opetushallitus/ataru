CREATE extension IF NOT EXISTS "uuid-ossp";

ALTER TABLE forms ALTER COLUMN id TYPE UUID USING id::uuid;

ALTER TABLE forms ALTER COLUMN id SET DEFAULT uuid_generate_v4();
