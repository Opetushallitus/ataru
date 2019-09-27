'use strict'

const path = require('path')

// Allow local overriding of hakija and virkailija configs via environment variables
const virkailijaConfig =
  process.env['VIRKAILIJA_CONFIG'] ||
  '../ataru-secrets/virkailija-local-dev.edn'

const hakijaConfig =
  process.env['HAKIJA_CONFIG'] || '../ataru-secrets/hakija-local-dev.edn'

module.exports = {
  apps: [
    {
      name: 'ataru-css-compilation',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['less', 'auto'],
      cwd: __dirname,
      log_file: 'less.log',
      pid_file: '.less.pid',
      combine_logs: true,
      min_uptime: 3000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork',
    },
    {
      name: 'ataru-hakija-backend-8351',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['hakija-dev'],
      env: {
        CONFIG: hakijaConfig,
      },
      cwd: __dirname,
      log_file: 'hakija-dev.log',
      pid_file: '.hakija-dev.pid',
      combine_logs: true,
      min_uptime: 3000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork',
    },
    {
      name: 'ataru-virkailija-backend-8350',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['virkailija-dev'],
      env: {
        CONFIG: virkailijaConfig,
      },
      cwd: __dirname,
      log_file: 'virkailija-dev.log',
      pid_file: '.virkailija-dev.pid',
      combine_logs: true,
      min_uptime: 3000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork',
    },
    {
      name: 'ataru-figwheel',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['figwheel', 'virkailija-dev', 'hakija-dev'],
      cwd: __dirname,
      log_file: 'figwheel.log',
      pid_file: '.figwheel.pid',
      combine_logs: true,
      min_uptime: 3000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork',
    },
  ],
}
