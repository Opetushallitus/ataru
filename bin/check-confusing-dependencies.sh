#!/usr/bin/env bash

echo "Checking for confusing dependencies..."
output=$(lein deps :tree 2>&1 | sed -n '/confusing dependencies/,$p')
if [ -n "$output" ]; then
    echo "$output"
    exit 1
else
    echo "No confusing dependencies found."
fi