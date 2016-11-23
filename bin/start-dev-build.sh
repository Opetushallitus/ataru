#!/bin/bash

# Starts the test-build in one command
# Note that this uses the unit/browser-test database
# and runs dummy integrations

SESSION=$USER

tmux -2 new-session -d -s $SESSION

tmux new-window -t $SESSION:1 -n 'Ataru'
tmux split-window -h
tmux select-pane -t 0
tmux split-window -v

tmux select-pane -t 2

tmux split-window -v

tmux select-pane -t 0
tmux send-keys "./bin/lein virkailija-dev" C-m

tmux select-pane -t 1
tmux send-keys "./bin/lein figwheel-virkailija" C-m

tmux select-pane -t 2
tmux send-keys "./bin/lein less auto" C-m

tmux select-pane -t 3
tmux send-keys "./bin/lein hakija-dev" C-m

tmux split-window -h

tmux select-pane -t 4
tmux send-keys "./bin/lein figwheel-hakija" C-m

tmux -2 attach-session -t $SESSION
