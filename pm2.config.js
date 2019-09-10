'use strict'

var path = require('path')
module.exports = {
  apps: [
    {
      name: 'Ataru docker images',
      script: 'docker-compose',
      interpreter: '/bin/sh',
      args: ['up'],
      cwd: __dirname,
      log_file: 'docker-compose.log',
      pid_file: '.docker-compose.pid',
      combine_logs: true,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: "none",
      exec_mode: "fork"
    },
    {
      name: 'Ataru CSS compilation',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['less', 'auto'],
      cwd: __dirname,
      log_file: 'lein-less.log',
      pid_file: '.lein-less.pid',
      combine_logs: true,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: "none",
      exec_mode: "fork"
    },
    {
      name: 'Ataru Virkailija frontend compilation',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['cljsbuild', 'auto', 'virkailija-dev'],
      cwd: __dirname,
      log_file: 'lein-cljsbuild-virkailija-dev.log',
      pid_file: '.lein-cljsbuild-virkailija-dev.pid',
      combine_logs: true,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: "none",
      exec_mode: "fork"
    },
    {
      name: 'Ataru Hakija frontend compilation',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['cljsbuild', 'auto', 'hakija-dev'],
      cwd: __dirname,
      log_file: 'lein-cljsbuild-hakija-dev.log',
      pid_file: '.lein-cljsbuild-hakija-dev.pid',
      combine_logs: true,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: "none",
      exec_mode: "fork"
    },
    {
      name: 'Ataru Hakija backend (8351)',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['hakija-dev'],
      env: {
          "CONFIG": "../ataru-secrets/hakija-local-dev.edn"
      },
      cwd: __dirname,
      log_file: 'lein-hakija-dev.log',
      pid_file: '.lein-hakija-dev.pid',
      combine_logs: true,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: "none",
      exec_mode: "fork"
    },
    {
      name: 'Ataru Virkailija backend (8350)',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['virkailija-dev'],
      env: {
          "CONFIG": "../ataru-secrets/virkailija-local-dev.edn"
      },
      cwd: __dirname,
      log_file: 'lein-virkailija-dev.log',
      pid_file: '.lein-virkailija-dev.pid',
      combine_logs: true,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: "none",
      exec_mode: "fork"
    },
    {
      name: 'Ataru Figwheel',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['figwheel', 'virkailija-dev', 'hakija-dev'],
      cwd: __dirname,
      log_file: 'lein-virkailija-dev.log',
      pid_file: '.lein-virkailija-dev.pid',
      combine_logs: true,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: "none",
      exec_mode: "fork"
    },

  ]
}
