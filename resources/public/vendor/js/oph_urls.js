"use strict";

/**
 * window.urls.load("sure-url_properties.json.json", {overrides: "rest/v1/properties", properties: .., defaults: ...}).then(appInit, appError)
 * window.urls.addProperties({"service.info": "/service/info/$1/$2"})
 *
 * window.urls.debug = true
 * window.urls.debugLog()
 *
 * window.url("service.info", param1, param2, {key3: value}) // extra named values are added to querystring
 *
 * window.urls("myscope").url(key, param)
 * window.urls("myscope").noEncode().url(key, param)
 * window.urls("myscope").omitEmptyValuesFromQuerystring().url(key, param)
 * window.urls().omitEmptyValuesFromQuerystring().url(key, param)
 *
 * Default scope: window.url, window.urls.* and window.urls() use a scope named "default"
 *
 * Config lookup order: window.urls("myscope").override, window.urls("myscope").properties, window.urls("myscope").defaults
 * Lookup key order:
 * * for main url window.url's first parameter: "service.info" from all configs
 * * baseUrl: "service.baseUrl" from all configs and "baseUrl" from all configs
 *
 */

(function() {
    var version="2.1"

    var exportDests = []

    function addExportDest(exportDest, urlsFN) {
        if(exportDest.urls) {
            if(exportDest.urls.version !== version)   {
                logError("'Mismatching oph_urls.js. First loaded (and in use):", exportDest.urls.version, " second loaded (not in use): ", version, ". Export destination is ", exportDest)
            }
            return
        }
        exportDests.push(exportDest)
        exportDest.urls = urlsFN
    }

    var urlsFN = function(scopeName) {
        if(!scopeName) {
            scopeName="default"
        }
        if(!urlsFN.scopes[scopeName]) {
            var scope = createScope(scopeName);
            urlsFN.scopes[scopeName] = scope
            if(scopeName == "default") {
                // link default scope's functions directly under urlsFN and export url() function to all scopes
                exportDests.forEach(function (exportDest) {
                    exportDest.url = scope.url
                })
                urlsFN.url = scope.url
                urlsFN.omitEmptyValuesFromQuerystring = scope.omitEmptyValuesFromQuerystring
                urlsFN.noEncode = scope.noEncode
                urlsFN.addOverrides = scope.addOverrides
                urlsFN.addProperties = scope.addProperties
                urlsFN.addDefaults = scope.addDefaults
                urlsFN.load = scope.load
            }
        }
        return urlsFN.scopes[scopeName]
    }
    urlsFN.scopes = {}
    urlsFN.version = version
    urlsFN.debug = false
    urlsFN.debugLog = function() {
        urlsFN.debug = true;
        return urlsFN;
    }

    // bind to window.urls and module.export.urls
    if(typeof window !== 'undefined') {
        addExportDest(window, urlsFN)
    }
    if(typeof module !== 'undefined' && module.exports) {
        addExportDest(module.exports, urlsFN)
    }

    // initialize default scope and its functions
    urlsFN()

    // scope has isolated properties and configs
    function createScope(name) {
        var scopeConfig = {
            overrides: {},
            properties: {},
            defaults: {},
            omitEmptyValuesFromQuerystring: false,
            encode: true,
            name: name
        }

        var resolveConfig = function(key, defaultValue) {
            var configs = [scopeConfig.overrides, scopeConfig.properties, scopeConfig.defaults]
            for (var i = 0; i < configs.length; i++) {
                var c = configs[i]
                if(c.hasOwnProperty(key)) {
                    return c[key]
                }
            }
            if(typeof defaultValue == 'function') {
                return defaultValue()
            }
            if(typeof defaultValue == 'undefined') {
                throw new Error("Could not resolve value for '"+key+"'")
            }
            return defaultValue
        }

        var encodeParameter = function(arg) {
            arg = [undefined, null].indexOf(arg) > -1 ? "" : arg
            if(scopeConfig.encode) {
                arg = encodeURIComponent(arg)
            }
            return arg
        }

        var includeToQuerystring = function(v) {
            if(scopeConfig.omitEmptyValuesFromQuerystring) {
                return [undefined, null, ""].indexOf(v) === -1
            } else {
                return [undefined, null].indexOf(v) === -1
            }
        }

        var ret = {}
        ret.url = function () {
            var key = Array.prototype.shift.apply(arguments)
            var args = Array.prototype.slice.call(arguments)
            var queryString = "";
            if (!key) {
                throw new Error("first parameter 'key' not defined!");
            }
            var url = resolveConfig(key)
            // reverse iteration because $10 needs to be handled first
            for (var i = args.length; i > 0; i--) {
                var arg = args[i - 1];
                if (typeof arg === "object") {
                    Object.keys(arg).forEach(function (k) {
                        var originalValue = arg[k];
                        var tmpUrl = url;
                        if(!isArray(originalValue)) {
                            tmpUrl = url.replace("$" + k, encodeParameter(originalValue))
                        }
                        if (tmpUrl == url && includeToQuerystring(originalValue)) {
                            var values = isArray(originalValue) ? originalValue : [originalValue];
                            for(var j = 0; j < values.length; j++) {
                                var separator = (queryString.length > 0) ? "&" : "?";
                                var encodedKeyValue = encodeParameter(k) + "=" + encodeParameter(values[j]);
                                queryString = queryString + separator + encodedKeyValue
                            }
                        }
                        url = tmpUrl
                    })
                } else {
                    url = url.replace("$" + i, encodeParameter(arg))
                }
            }
            var baseUrl = resolveConfig(parseService(key) + ".baseUrl", function () {
                return resolveConfig("baseUrl", null)
            })
            if (baseUrl) {
                url = joinUrl(baseUrl, url)
            }
            url = url + queryString
            debug("url:", key, "->", url, "scope:", scopeConfig.name)
            return url
        }

        // scope's exposed variables and functions
        ret.scopeConfig = scopeConfig

        ret.omitEmptyValuesFromQuerystring = function () {
            var newScope = copyScope();
            newScope.scopeConfig.omitEmptyValuesFromQuerystring = true
            return newScope
        }
        ret.noEncode = function() {
            var newScope = copyScope();
            newScope.scopeConfig.encode = false
            return newScope
        }
        ret.addOverrides = function (props) {
            merge(props, scopeConfig.overrides)
            return ret
        }
        ret.addProperties = function (props) {
            mergePropertiesWithWarning(props, scopeConfig.properties)
            return ret
        }
        ret.addDefaults = function (props) {
            mergePropertiesWithWarning(props, scopeConfig.defaults)
            return ret
        }
        ret.load = function () {
            // parse arguments from strings and maps: "url", {overrides: ["", ""], properties: "", defaults}
            var overridesUrls = [], propertiesUrls=[], defaultsUrls=[]
            for (var i = 0; i < arguments.length;  i++) {
                var arg = arguments[i]
                if(typeof arg === "string") {
                    propertiesUrls.push(arg)
                } else {
                    overridesUrls.push(arg.overrides || [])
                    propertiesUrls.push(arg.properties || [])
                    defaultsUrls.push(arg.defaults || [])
                }
            }
            var p = promise()
            overridesUrls = flatten(overridesUrls)
            propertiesUrls = flatten(propertiesUrls)
            defaultsUrls = flatten(defaultsUrls)

            var maxCount = overridesUrls.length + propertiesUrls.length + defaultsUrls.length;
            debug("loading " + maxCount + " json files. scope:", scopeConfig.name)
            // wait until all GETs complete and process jsons or reject
            var counterP = counterPromise(maxCount, function(){
                overridesUrls.forEach(function(urlJson){
                    merge(urlJson.json, scopeConfig.overrides)
                })
                propertiesUrls.forEach(function(urlJson){
                    mergePropertiesWithWarning(urlJson.json, scopeConfig.properties)
                })
                defaultsUrls.forEach(function(urlJson){
                    mergePropertiesWithWarning(urlJson.json, scopeConfig.defaults)
                })
                debug("loaded " + maxCount + " files successfully. scope:", scopeConfig.name)
                p.fulfill()
            }, function(err) {
                logError("failed to load json files. scope:", scopeConfig.name, err)
                p.reject(err)
            })
            overridesUrls = loadUrls(overridesUrls, counterP)
            propertiesUrls = loadUrls(propertiesUrls, counterP)
            defaultsUrls = loadUrls(defaultsUrls, counterP)

            return p
        }
        function copyScope() {
            var newScope = createScope("copy of " + scopeConfig.name)
            var conf = newScope.scopeConfig
            merge(scopeConfig.overrides, conf.overrides)
            merge(scopeConfig.properties, conf.properties)
            merge(scopeConfig.defaults, conf.defaults)
            conf.omitEmptyValuesFromQuerystring = scopeConfig.omitEmptyValuesFromQuerystring
            conf.encode = scopeConfig.encode
            return newScope;
        }

        debug("created scope:", scopeConfig.name)
        return ret
    }

    function mergePropertiesWithWarning(props, destProps) {
        var existsAlready = Object.keys(props).filter(function (k) {
            return k in destProps && destProps[k] !== props[k]
        })
        if(existsAlready.length == 0) {
            merge(props, destProps)
        } else {
            logError("Properties already contains following keys:", existsAlready, "existing properties:", destProps, "new properties:", props)
            alert("Url properties conflict. Check console log")
        }
    }

    function log(logType, args) {
        var args = Array.prototype.slice.call(args)
        args.unshift("OphProperties")
        if(console) {
            var logFn = console[logType] || console.log
            if(logFn) {
                logFn.apply(console, args)
            }
        }
    }

    function debug() {
        if(urlsFN.debug) {
            log("log", arguments)
        }
    }

    function logError() {
        log("error", arguments)
    }

    function parseService (key) {
        return key.substring(0, key.indexOf("."))
    }

    // ajax loading

    function ajaxJson(method, url, onload, onerror) {
        var oReq = new XMLHttpRequest();
        oReq.open(method, url, true);
        oReq.onreadystatechange = function() {
            if (oReq.readyState == 4) {
                if(oReq.status == 200) {
                    if(onload) {
                        onload(JSON.parse(oReq.responseText))
                    }
                } else {
                    if(onerror) {
                        onerror(url + " status " +oReq.status + ": " + oReq.responseText)
                    }
                }
            }
        }
        oReq.send(null);
    }

    // minimalist A+/angular Promise implementation, returns Promise object with .then(fulfill, reject) and .success(cb) support
    function promise() {
        var thens = []
        var failReason = undefined
        var completed = false
        var failed = false

        function complete() {
            if(!completed) {
                completed = true
                thens.forEach(function (then) {
                    if(failed) {
                        if(then.onReject) {
                            then.onReject(failReason)
                        }
                    } else {
                        if(then.onFulfill) {
                            then.onFulfill()
                        }
                    }
                })
            }
        }

        return {
            fulfill: function() {
                complete()
            },
            reject: function(fail) {
                failReason = fail
                failed = true
                complete()
            },
            then: function(onFulfill, onReject) {
                if(completed) {
                    if(failed) {
                        onReject(failReason)
                    } else {
                        onFulfill()
                    }
                } else {
                    thens.push({onFulfill: onFulfill, onReject: onReject})
                }
            }
        }
    }

    // minimalist counter promise, call fulfill and reject only after fulfill or reject is called maxCount times
    function counterPromise(maxCount, onFulfill, onReject) {
        var count=0;
        var fails = [];

        function complete() {
            if(count < maxCount) {
                count = count + 1
            }
            if(maxCount === count) {
                if(fails.length > 0) {
                    onReject(fails)
                } else {
                    onFulfill()
                }
            }
        }
        return {
            fulfill: function() {
                complete()
            },
            reject: function(fail) {
                fails.push(fail)
                complete()
            }
        }
    }

    function loadUrls(urls, promise) {
        return urls.map(function (url) {
            var ret = {url: url};
            ajaxJson("GET", url, function (data) {
                ret.json = data
                promise.fulfill()
            }, promise.reject)
            return ret
        });
    }

    // helper functions

    function merge(from, dest) {
        Object.keys(from).forEach(function(key){
            dest[key]=from[key];
        })
    }

    function joinUrl() {
        var args = Array.prototype.slice.call(arguments)
        if(args.length === 0) {
            throw new Error("no arguments");
        }
        var url = null
        args.forEach(function(arg) {
            if(!url) {
                url = arg
            } else {
                var endsWithBool = endsWith(url, "/");
                var startsWithBool = startsWith(url, "/");
                if (url === '/') {
                    url = url + arg
                }
                else if (endsWithBool && startsWithBool) {
                    url = url + arg.substring(1)
                } else if(endsWithBool || startsWithBool) {
                    url = url + arg
                } else {
                    url = url + "/" + arg
                }
            }
        })
        return url
    }

    function isArray(arr) {
        if(Array.isArray) {
            return Array.isArray(arr);
        } else {
            return arr && arr.constructor === Array;
        }
    }

    function flatten(item, dest) {
        if (dest === undefined) {
            dest = []
        }
        if (Array.isArray(item)) {
            item.forEach(function (i) {
                flatten(i, dest)
            })
        } else {
            dest.push(item)
        }
        return dest
    }

    function startsWith(txt, searchString, position){
        position = position || 0;
        return txt.substr(position, searchString.length) === searchString;
    }

    function endsWith(txt, searchString, position) {
        var subjectString = txt.toString();
        if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
            position = subjectString.length;
        }
        position -= searchString.length;
        var lastIndex = subjectString.indexOf(searchString, position);
        return lastIndex !== -1 && lastIndex === position;
    }

})();
