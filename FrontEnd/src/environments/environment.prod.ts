import packageInfo from '../../package.json';

export const environment = {
  appVersion: packageInfo.version,
  production: true,
  apiUrl: 'http://localhost:8083', // gateway URL (change in production)
  pharmacyApiUrl: 'http://localhost:8083', // Use gateway for CORS support (change in production)
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
