import { provideKeycloak } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

export const provideKeycloakAngular = () =>
  provideKeycloak({
    config: {
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId
    },
    initOptions: {
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: `${window.location.origin}/assets/silent-check-sso.html`,
      pkceMethod: 'S256',
      checkLoginIframe: false
    }
    // No INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG needed —
    // AuthInterceptor handles token injection directly via keycloak-js updateToken()
  });
