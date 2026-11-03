import { deployConfig } from './deploy-config.js';

window.APP_CONFIG = {
  // Deployment config is authoritative by default; localStorage remains fallback for local testing.
  apiBaseUrl: deployConfig.apiBaseUrl || localStorage.getItem('spendsmart.apiBaseUrl') || 'http://localhost:8082/api/v1',
  cognitoDomain: deployConfig.cognitoDomain || localStorage.getItem('spendsmart.cognitoDomain') || '',
  cognitoClientId: deployConfig.cognitoClientId || localStorage.getItem('spendsmart.cognitoClientId') || '',
  cognitoRedirectUri: deployConfig.cognitoRedirectUri || localStorage.getItem('spendsmart.cognitoRedirectUri') || '',
  cognitoLogoutUri: deployConfig.cognitoLogoutUri || localStorage.getItem('spendsmart.cognitoLogoutUri') || '',
  cognitoScopes: deployConfig.cognitoScopes || localStorage.getItem('spendsmart.cognitoScopes') || 'openid email profile'
};

export const config = window.APP_CONFIG;
