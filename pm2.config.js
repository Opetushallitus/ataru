'use strict'

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
      script: 'bin/watch-compile-less.sh',
      cwd: __dirname,
      log_file: 'logs/pm2/less.log',
      pid_file: '.less.pid',
      combine_logs: true,
      min_uptime: 30000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_mode: 'fork',
    },
    {
      name: 'ataru-hakija-backend-8351',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['hakija-dev'],
      env: {
        APP: 'ataru-hakija',
        CONFIG: hakijaConfig,
      },
      cwd: __dirname,
      log_file: 'logs/pm2/hakija-dev.log',
      pid_file: '.hakija-dev.pid',
      combine_logs: true,
      min_uptime: 30000,
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
        APP: 'ataru-editori',
        CONFIG: virkailijaConfig,
      },
      cwd: __dirname,
      log_file: 'logs/pm2/virkailija-dev.log',
      pid_file: '.virkailija-dev.pid',
      combine_logs: true,
      min_uptime: 30000,
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
      args: ['start-figwheel'],
      cwd: __dirname,
      log_file: 'logs/pm2/figwheel.log',
      pid_file: '.figwheel.pid',
      combine_logs: true,
      min_uptime: 30000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork',
    },
    {
      name: 'ataru-hakija-cypress-backend-8353',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['with-profile', '+hakija-cypress', 'run', 'hakija'],
      env: {
        APP: 'ataru-hakija',
        ATARU_HTTP_PORT: 8353,
        ATARU_REPL_PORT: 3340,
        CONFIG: 'config/cypress.edn',
      },
      cwd: __dirname,
      log_file: 'logs/pm2/hakija-cypress-dev.log',
      pid_file: '.hakija-cypress-dev.pid',
      combine_logs: true,
      min_uptime: 30000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork',
    },
    {
      name: 'ataru-virkailija-cypress-backend-8352',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['with-profile', '+virkailija-cypress', 'run', 'virkailija'],
      env: {
        APP: 'ataru-editori',
        ATARU_HTTP_PORT: 8352,
        ATARU_REPL_PORT: 3339,
        CONFIG: 'config/cypress.edn',
      },
      cwd: __dirname,
      log_file: 'logs/pm2/virkailija-cypress-dev.log',
      pid_file: '.virkailija-cypress-dev.pid',
      combine_logs: true,
      min_uptime: 30000,
      max_restarts: 5,
      restart_delay: 4000,
      wait_ready: true,
      watch: false,
      exec_interpreter: 'none',
      exec_mode: 'fork',
    },
  ],
}
