#!/bin/bash

set -e

VIRKAILIJA_CONFIG="../ataru-secrets/virkailija-dev.edn"
HAKIJA_CONFIG="../ataru-secrets/hakija-dev.edn"

type -p rlwrap > /dev/null && RLWRAP="rlwrap" || RLWRAP=""

while getopts ":v::h:i" opt; do
  case $opt in
    i)
      echo "Using iTerm2 integration"
      ITERM_FLAG="-CC"
      ;;
    v)
      echo "Using virkailija-config: $OPTARG" >&2
      VIRKAILIJA_CONFIG="$OPTARG"
      ;;
    h)
      echo "Using hakija-config: $OPTARG" >&2
      HAKIJA_CONFIG="$OPTARG"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

SESSION=$USER

./bin/lein clean

tmux -2 -u new-session -d -s $SESSION

tmux new-window -t $SESSION:1 -n 'Ataru'
tmux split-window -h
tmux select-pane -t 0
tmux split-window -v

tmux select-pane -t 2

tmux split-window -v

tmux select-pane -t 0
tmux send-keys "CONFIG=$VIRKAILIJA_CONFIG ./bin/lein virkailija-dev" C-m

tmux select-pane -t 1
tmux send-keys "CONFIG=$HAKIJA_CONFIG ./bin/lein hakija-dev" C-m

tmux select-pane -t 2
tmux send-keys "./bin/lein less auto" C-m

tmux select-pane -t 3
tmux send-keys "$RLWRAP ./bin/lein figwheel-virkailija" C-m

tmux split-window -v

tmux select-pane -t 4
tmux send-keys "$RLWRAP ./bin/lein figwheel-hakija" C-m

if [ -n $ITERM_FLAG ]
then
    echo "Attaching session."
    echo "To kill detached session: tmux kill-session -t $SESSION"
fi

tmux $ITERM_FLAG -u -2 attach-session -t $SESSION
