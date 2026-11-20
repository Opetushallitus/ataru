#!/usr/bin/env sh
# Päivittää clj-kondon välimuistin taustalla, kun git on muuttanut työhakemiston koodia (pull, haaran vaihto,
# rebase). Pre-commit-hook lintaa vain stagetut tiedostot, ja clj-kondo tuntee muiden nimiavaruuksien muuttujat
# välimuistista. Ilman päivitystä toisessa nimiavaruudessa lisätty muuttuja näkyy lintterille tuntemattomana.

command -v clj-kondo >/dev/null 2>&1 || exit 0

cache_dir=.clj-kondo/.cache
lock_dir=$cache_dir/refresh.lock
rerun_file=$cache_dir/refresh.rerun
mkdir -p "$cache_dir"
# Keskeytyneen päivityksen lukko ei saa estää myöhempiä päivityksiä
find "$lock_dir" -maxdepth 0 -mmin +5 -exec rmdir {} \; 2>/dev/null

# Kun päivitys on jo käynnissä (esim. rebasen alussa), se ajetaan uudelleen valmistuttuaan, jotta välimuisti vastaa
# lopullista työhakemistoa
if ! mkdir "$lock_dir" 2>/dev/null; then
  touch "$rerun_file"
  exit 0
fi

nohup sh -c "
  while :; do
    rm -f '$rerun_file'
    clj-kondo --parallel --dependencies --lint src spec dev
    [ -e '$rerun_file' ] || break
  done
  rmdir '$lock_dir'
  # Pyyntö lukon vapautumisen hetkellä
  if [ -e '$rerun_file' ]; then '$0'; fi
" >/dev/null 2>&1 &
