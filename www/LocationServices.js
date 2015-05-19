var exec = require("cordova/exec");
module.exports = {
    /**
    * @property {Object} stationaryRegion
    */
    /**
    * @property {Object} config
    */
    config: {},
    init: function(config) {
        console.log('BG INIT BABEH!');
        this.config = config;
        var params              = JSON.stringify(config.params || {}),
            headers             = JSON.stringify(config.headers || {}),
            url                 = config.url || 'BackgroundGeoLocation_url',
            stationaryRadius    = (config.stationaryRadius >= 0) ? config.stationaryRadius :  50, // meters
            distanceFilter      = (config.distanceFilter   >= 0) ? config.distanceFilter   : 500, // meters
            locationTimeout     = (config.locationTimeout  >= 0) ? config.locationTimeout  :  60, // seconds
            desiredAccuracy     = (config.desiredAccuracy  >= 0) ? config.desiredAccuracy  : 100, // meters
            debug               = config.debug || false,
            notificationTitle   = config.notificationTitle || "Background tracking",
            notificationText    = config.notificationText  || "ENABLED",
            activityType        = config.activityType      || "OTHER",
            stopOnTerminate     = config.stopOnTerminate   || false,
            interval            = (config.interval         >= 0) ? config.interval        : 900000, // milliseconds
            fastestInterval     = (config.fastestInterval  >= 0) ? config.fastestInterval : 120000; // milliseconds
            fences              = config.fences || null;

        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'init',
             [params, headers, url, stationaryRadius, distanceFilter, locationTimeout, desiredAccuracy, debug, notificationTitle, notificationText, activityType, stopOnTerminate, interval, fastestInterval, fences]
        );
    },
    start: function(success, failure, config) {
        exec(success || function() {},
             failure || function() {},
             'BackgroundGeoLocation',
             'start',
             []);
    },
    stop: function(success, failure, config) {
        exec(success || function() {},
            failure || function() {},
            'BackgroundGeoLocation',
            'stop',
            []);
    }
};
