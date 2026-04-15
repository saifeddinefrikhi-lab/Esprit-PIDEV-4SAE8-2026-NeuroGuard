import packageInfo from '../../package.json';

export const environment = {
  appVersion: packageInfo.version,
  production: true,
  apiUrl: 'http://localhost:8083', // gateway URL (change in production)
  pharmacyApiUrl: 'http://localhost:8085',
  wsUrl: 'http://localhost:8089',
  carePlanWsUrl: 'http://localhost:8084'
};
