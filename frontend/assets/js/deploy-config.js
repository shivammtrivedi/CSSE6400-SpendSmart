// Deployment-time config.
// Set these values during deployment so frontend pages do not rely on localStorage setup.
export const deployConfig = Object.freeze({
  apiBaseUrl: '',
  cognitoDomain: '',
  cognitoClientId: '',
  cognitoRedirectUri: '',
  cognitoLogoutUri: '',
  cognitoScopes: 'openid email profile'
});
