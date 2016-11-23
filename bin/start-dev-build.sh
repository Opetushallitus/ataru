#!/bin/bash

set -e

VIRKAILIJA_CONFIG="config/dev.edn"
HAKIJA_CONFIG="config/dev.edn"

while getopts ":v::h:" opt; do
  case $opt in
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

tmux -2 new-session -d -s $SESSION

tmux new-window -t $SESSION:1 -n 'Ataru'
tmux split-window -h
tmux select-pane -t 0
tmux split-window -v

tmux select-pane -t 2

tmux split-window -v

tmux select-pane -t 0
tmux send-keys "CONFIG=$VIRKAILIJA_CONFIG ./bin/lein virkailija-dev" C-m

tmux select-pane -t 1
tmux send-keys "./bin/lein figwheel-virkailija" C-m

tmux select-pane -t 2
tmux send-keys "./bin/lein less auto" C-m

tmux select-pane -t 3
tmux send-keys "CONFIG=$HAKIJA_CONFIG ./bin/lein hakija-dev" C-m

tmux split-window -v

tmux select-pane -t 4
tmux send-keys "./bin/lein figwheel-hakija" C-m

tmux -2 attach-session -t $SESSION
