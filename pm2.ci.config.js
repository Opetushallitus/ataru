'use strict'

module.exports = {
  apps: [
    {
      name: 'ataru-hakija-cypress-ci-backend-8353',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['with-profile', '+hakija-cypress', 'run', 'hakija'],
      env: {
        APP: 'ataru-hakija',
        ATARU_HTTP_PORT: 8353,
        ATARU_REPL_PORT: 3340,
        CONFIG: 'config/cypress.ci.edn',
      },
      cwd: __dirname,
      log_file: 'logs/pm2/hakija-cypress-ci-dev.log',
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
      name: 'ataru-virkailija-cypress-ci-backend-8352',
      script: 'lein',
      interpreter: '/bin/sh',
      args: ['with-profile', '+virkailija-cypress', 'run', 'virkailija'],
      env: {
        APP: 'ataru-editori',
        ATARU_HTTP_PORT: 8352,
        ATARU_REPL_PORT: 3339,
        CONFIG: 'config/cypress.ci.edn',
      },
      cwd: __dirname,
      log_file: 'logs/pm2/virkailija-cypress-ci-dev.log',
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
