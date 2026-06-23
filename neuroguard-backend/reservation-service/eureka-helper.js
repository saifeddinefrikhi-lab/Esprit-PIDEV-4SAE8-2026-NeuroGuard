const Eureka = require('eureka-js-client').Eureka;

exports.registerWithEureka = function (appName, PORT) {
    const hostname = process.env.EUREKA_INSTANCE_HOSTNAME || 'localhost';
    const ipAddr = process.env.EUREKA_INSTANCE_IP || hostname;
    const eurekaHost = process.env.EUREKA_HOST || 'localhost';
    const eurekaPort = parseInt(process.env.EUREKA_PORT || '8761', 10);

    const client = new Eureka({
        instance: {
            app: appName,
            hostName: hostname,
            ipAddr: ipAddr,
            statusPageUrl: `http://${hostname}:${PORT}/info`,
            healthCheckUrl: `http://${hostname}:${PORT}/info`,
            homePageUrl: `http://${hostname}:${PORT}/`,
            port: {
                '$': PORT,
                '@enabled': 'true',
            },
            vipAddress: appName,
            dataCenterInfo: {
                '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
                name: 'MyOwn',
            },
        },
        eureka: {
            host: eurekaHost,
            port: eurekaPort,
            servicePath: '/eureka/apps/',
            maxRetries: 10,
            requestRetryDelay: 2000,
        },
    });

    client.logger.level('debug');

    client.start(error => {
        console.log(error || `NodeJS Service registered with Eureka at ${eurekaHost}:${eurekaPort}`);
    });

    function exitHandler(options, exitCode) {
        if (exitCode || exitCode === 0) console.log(exitCode);
        if (options.exit) {
            client.stop();
        }
    }

    client.on('deregistered', () => {
        process.exit();
        console.log('deregistered');
    });

    client.on('started', () => {
        console.log(`Eureka registration started (host=${eurekaHost}, instance=${hostname}:${PORT})`);
    });

    process.on('SIGINT', exitHandler.bind(null, { exit: true }));
};
