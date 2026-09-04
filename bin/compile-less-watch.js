#!/usr/bin/env node
'use strict'

// Watches resources/less for changes and reruns compile-less.sh.
// Polls mtimes instead of using fs.watch/inotify because the dev
// environment's filesystem (e.g. Docker bind mounts) doesn't reliably
// deliver native file-change events.

const { execFileSync } = require('node:child_process')
const fs = require('node:fs')
const path = require('node:path')

const ROOT_DIR = path.join(__dirname, '..')
const LESS_DIR = path.join(ROOT_DIR, 'resources', 'less')
const COMPILE_SCRIPT = path.join(__dirname, 'compile-less.sh')
const POLL_INTERVAL_MS = 200

function compile() {
  try {
    execFileSync(COMPILE_SCRIPT, { stdio: 'inherit', cwd: ROOT_DIR })
  } catch (err) {
    console.error('LESS compilation failed:', err.message)
  }
}

function snapshot() {
  return Object.fromEntries(
    fs.readdirSync(LESS_DIR)
      .filter(name => name.endsWith('.less'))
      .map(name => [name, fs.statSync(path.join(LESS_DIR, name)).mtimeMs])
  )
}

function changed(previous, current) {
  const names = new Set([...Object.keys(previous), ...Object.keys(current)]).values()
  return names.some(name => previous[name] !== current[name])
}

compile()

let previous = snapshot()
setInterval(() => {
  const current = snapshot()
  if (changed(previous, current)) {
    previous = current
    compile()
  }
}, POLL_INTERVAL_MS)
