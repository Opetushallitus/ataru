#!/bin/bash

set -e

if [ $# -ne 2 ]
then
    echo "OY-400 manual update of latest form 6838d6a4-c847-45ab-9af8-d2b590475ead"
    echo
    echo "Usage: $0 <username> <password>\n"
    echo
    echo "Uses localhost to access the ataru database. Make sure to have tunnel open for wanted destination, eg:"
    echo
    echo "ssh -F ~/.opintopolku/pallero.ssh.config -L 5432:ataru.db.testiopintopolku.fi:5432 USENAME@bastion.testiopintopolku.fi"
    exit 1
fi

USERNAME=$1
PASSWORD=$2
conninfo="host=localhost port=5432 dbname=ataru user=$USERNAME password=$PASSWORD"

while read -r command
do
    sedcommands+="$command;"
done <<EOF
s/"e3d86c7b-1488-49cf-8c61-29509a16c790",/"higher-completed-base-education","rules":{"pohjakoulutusristiriita":null},/g
s/f175072e-302b-45ef-a2f1-ed43482012e0/pohjakoulutus_am--year-of-completion/g
s/f0280133-adcd-41c3-94f4-61607258ff1f/pohjakoulutus_amt--year-of-completion/g
s/5f87865d-07dd-4eff-8eb5-7b407ccaf987/pohjakoulutus_avoin--year-of-completion/g
s/54c0639a-d069-4f74-b12e-9b52362dd1bd/pohjakoulutus_kk--completion-date/g
s/87dfe3b7-c5d3-4f4d-9ab5-9cdca127e1cd/pohjakoulutus_kk_ulk--year-of-completion/g
s/d0c182cd-47fd-4d2b-9692-2fba636234d9/pohjakoulutus_lk--year-of-completion/g
s/ee03ba0f-3d73-46fb-afe4-d734c9b9f8f7/pohjakoulutus_muu--year-of-completion/g
s/3edc1005-c1f8-42c7-92c5-1f2bee1333bf/pohjakoulutus_ulk--year-of-completion/g
s/016c0edb-ed82-4a0e-aeb8-15600d1c4739/pohjakoulutus_yo--yes-year-of-completion/g
s/4929578e-f018-4cd6-87c0-d15062e0e976/pohjakoulutus_yo_ammatillinen--vocational-completion-year/g
s/962ff7b5-c7fb-4fb1-9f4f-b4c94dbaaea4/pohjakoulutus_yo_kansainvalinen_suomessa--year-of-completion/g
s/4c148fc3-ec29-4003-a282-42fdb4a5e38d/pohjakoulutus_yo_ulkomainen--year-of-completion/g
s/ec8dbf34-1a54-4c34-b0e0-abe58972d9b2/secondary-completed-base-education/g
s/3f795b1b-e717-402f-b21f-e3698d4e166c/secondary-completed-base-education–country/g
s/0cf13391-7356-4188-bc26-c337610b7230/finnish-vocational-before-1995/g
s/0b6940ae-88a0-43ec-9732-82f679242b85/finnish-vocational-before-1995--year-of-completion/g
s/creator-of-the-original-form-MAGIC-STRING/OY-400-patch/g
EOF

columns="content, key, languages, organization_oid, deleted, name, locked, locked_by"
form_to_stdout="COPY \
(SELECT $columns, current_timestamp, 'oy-400-patch' FROM forms WHERE key = '6838d6a4-c847-45ab-9af8-d2b590475ead' order by created_time desc limit 1)\
 TO STDOUT"
stdin_to_form="COPY forms($columns, created_time, created_by) FROM STDIN"

psql "$conninfo" -c "$form_to_stdout" | sed -e "$sedcommands" | psql "$conninfo" -c "$stdin_to_form"

