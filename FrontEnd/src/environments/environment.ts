// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

import packageInfo from '../../package.json';

export const environment = {
  appVersion: packageInfo.version,
  production: false,
  apiUrl: 'http://localhost:8083', // Replace with your production gateway URL
  pharmacyApiUrl: 'http://localhost:8083', // Use gateway for CORS support
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'neuroguard',
    clientId: 'neuroguard-frontend'
  },
  googleClientId: '550789921754-tdpg2nso52gvhr2mgdhk0ra01hk79kt8.apps.googleusercontent.com',
  wsUrl: 'http://localhost:8089',
  carePlanWsUrl: 'http://localhost:8084',
  usersApi: 'http://localhost:8083/users',
  monitoringApi: 'http://localhost:8083/api/monitoring',
  wellbeingApi: 'http://localhost:8083/api/wellbeing'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.

// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
