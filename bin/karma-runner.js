const puppeteer = require('puppeteer');
const { spawn } = require("child_process");
const args = process.argv;
const app = args[2];
const cookie = args[3];
const resultPrefix = '*** TEST';
const successMsg = ' SUCCESS';
const failMsg = ' FAIL';
const TIMEOUT_MINS = 5;
const timeoutMs = TIMEOUT_MINS * 60 * 1000;
const startTime = new Date().getTime();
let testsSuccessful = undefined;

const startsWith = (haystack, needle) => {
    return haystack.substring(0, needle.length) === needle
};

const takeScreenshot = page => {
    const filename = '/tmp/ataru-fail-' + new Date().getTime() + '.png';
    console.log('Taking screenshot', filename);
    return page.screenshot({path: filename, fullPage: true}).then(done => {
        console.log('Moving screenshot to S3');
        const {
            TRAVIS_REPO_SLUG: slugName,
            TRAVIS_BUILD_NUMBER: buildNumber,
            TRAVIS_JOB_NUMBER: jobNumber,
            TRAVIS_BUILD_DIR: buildDir
        } = process.env;
        const upload = spawn("artifacts",
            ["upload",
                "--target-paths",
                `artifacts/${slugName}/${buildNumber}/${jobNumber}/`,
                filename],
            {cwd: buildDir, env: process.env});
        upload.stdout.on('data', function(msg){
            console.log(msg.toString())
        });
        upload.on('error', (error) => {
                console.log(error);
        });
        console.log(`The screenshots can be found at: https://s3.console.aws.amazon.com/s3/buckets/opintopolku-utility-travis-artifacts/artifacts/${slugName}/${buildNumber}/${jobNumber}/tmp/`);
    });
};

const onConsoleMessage = (message, page) => {
    console.log(message);
    if (startsWith(message, resultPrefix)) {
        if (startsWith(message, resultPrefix + successMsg)) {
            testsSuccessful = true;
        } else if (startsWith(message, resultPrefix + failMsg)) {
            takeScreenshot(page).then(function() {
                testsSuccessful = false;
            });
        } else {
            console.error("Unknown result:", message);
            testsSuccessful = false;
        }
    }
};

const stopWhenFinished = () => {
    if (new Date().getTime() > startTime + timeoutMs) {
        console.log('Tests timed out after', timeoutMs, 'milliseconds');
        process.exit(1)
    } else if (typeof testsSuccessful === 'undefined') {
        setTimeout(stopWhenFinished, 1000);
    } else {
        process.exit(testsSuccessful ? 0 : 1);
    }
};

const addCookie = page => {
    page.setCookie({
        'name': 'ring-session',
        'value': cookie,
        'domain': 'localhost'
    });
};

const getUrl = () => {
    switch (app) {
        case 'virkailija':
            return 'http://localhost:8350/lomake-editori/virkailija-test.html';
        case 'virkailija-question-group':
            return 'http://localhost:8350/lomake-editori/virkailija-question-group-test.html';
        case 'virkailija-selection-limit':
            return 'http://localhost:8350/lomake-editori/virkailija-selection-limit-test.html';
        case 'virkailija-with-hakukohde-organization':
            return 'http://localhost:8350/lomake-editori/virkailija-with-hakukohde-organization-test.html';
        case 'hakija-form':
            return 'http://localhost:8351/hakemus/hakija-form-test.html';
        case 'hakija-question-group-form':
            return 'http://localhost:8351/hakemus/hakija-question-group-form-test.html';
        case 'hakija-selection-limit':
            return 'http://localhost:8351/hakemus/hakija-selection-limit-test.html';
        case 'hakija-haku':
            return 'http://localhost:8351/hakemus/hakija-haku-test.html';
        case 'hakija-hakukohde':
            return 'http://localhost:8351/hakemus/hakija-hakukohde-test.html';
        case 'hakija-hakukohteen-hakuaika':
            return 'http://localhost:8351/hakemus/hakija-hakukohteen-hakuaika-test.html';
        case 'hakija-ssn':
            return 'http://localhost:8351/hakemus/hakija-ssn-test.html';
        case 'hakija-edit':
            return 'http://localhost:8351/hakemus/hakija-edit-test.html';
        case 'virkailija-haku':
            return 'http://localhost:8351/hakemus/virkailija-haku-test.html';
        case 'virkailija-hakemus-edit':
            /* To run this test individually, run hakija edit-test, create a fake virkailija with update secret
             *  to the same application as hakija-edit-test uses and use the fake secret in the url. Easiest way to do it
             *  is to add the credentials directly in to the db
             * */
            return 'http://localhost:8351/hakemus/virkailija-hakemus-edit-test.html';
        case 'virkailija-question-group-application-handling':
            return 'http://localhost:8350/lomake-editori/virkailija-question-group-application-handling-test.html';
        default:
            console.log('invalid app: ' + app);
            process.exit(1);
    }
};

console.log('starting puppeteer')

puppeteer.launch({
    devtools: false,
    headless: true,
    args: [
      '--disable-gpu',
      '--disable-software-rasterizer',
      '--no-sandbox',
      '--use-gl'
    ]
    /* slowMo: 500 */
}).then(browser => {
    browser.newPage()
        .then(page => {
            const url = getUrl();
            if (cookie) addCookie(page);
            console.log("running browser tests for", app, url, cookie);
            page.on('console', msg => onConsoleMessage(msg.text(), page));
            setTimeout(stopWhenFinished, 1000);
            page.goto(url)
                .catch(err => {
                    console.error(err);
                    process.exit(1);
                });
        });
});
