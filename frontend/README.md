# SpendSmart Frontend (Static MVP)

This frontend is structured around product views rather than developer utility screens.

## Site Map (View Model)
1. Auth Landing -> `index.html`
2. Home -> `home.html`
3. Group List + Create Group -> `groups.html`
4. Group Detail / Edit Members -> `groups-members.html`
5. Group Expenses View -> `expenses.html`
6. Create/Edit Expense -> `expenses-create.html`, `expenses-edit.html`
7. Expense Detail + Settlements -> `expense-detail.html`
8. Bank Provider Selection -> `bank-connect.html`
9. Provider Login / Reconnect -> `bank-login.html`
10. Dashboard / Statistics -> `dashboard.html`
11. Search / Transactions -> `search.html`
12. Alerts / Budget Settings -> `alerts.html`
13. Global Error/Empty States -> `errors.html`

## Route List
- `/frontend/index.html` (auth landing)
- `/frontend/auth-callback.html` (Cognito code callback)
- `/frontend/home.html`
- `/frontend/groups.html`
- `/frontend/groups-members.html?groupId=<uuid>`
- `/frontend/expenses.html`
- `/frontend/expenses-create.html?groupId=<uuid>`
- `/frontend/expenses-edit.html?expenseId=<uuid>`
- `/frontend/expense-detail.html?expenseId=<uuid>`
- `/frontend/bank-connect.html`
- `/frontend/bank-login.html?provider=obp|bankapi`
- `/frontend/dashboard.html`
- `/frontend/search.html`
- `/frontend/alerts.html`
- `/frontend/errors.html`

## Implemented UX Flow
1. Login/signup from Auth Landing.
2. Home hub with shortcuts.
3. Group path:
   - Groups list/create -> Group members -> Expenses
   - Expenses list uses group picker (with member counts)
   - Expense detail shows split + settlements
4. Banking path:
   - Bank provider selection -> provider login/reconnect
5. Analytics path:
   - Dashboard -> Search drilldown
6. Alerts path:
   - Budget/notification settings

## Picker-first UX Decisions
- Group and expense selection is list-driven where possible.
- IDs are carried via query params/localStorage context, not repeatedly typed.
- Core action pages still allow direct deep-linking for demo and testing.

## Quick Preview
From repo root:

```bash
python3 -m http.server 5500
```

Then open:
- `http://localhost:5500/frontend/index.html`

## One-Time Local Setup
In browser devtools console (or using Home page fields):

```js
localStorage.setItem('spendsmart.apiBaseUrl', 'http://localhost:8082/api/v1');
localStorage.setItem('spendsmart.cognitoDomain', 'https://<your-cognito-domain>.auth.<region>.amazoncognito.com');
localStorage.setItem('spendsmart.cognitoClientId', '<app-client-id>');
localStorage.setItem('spendsmart.cognitoRedirectUri', 'http://localhost:5500/frontend/auth-callback.html');
localStorage.setItem('spendsmart.cognitoLogoutUri', 'http://localhost:5500/frontend/index.html');
```

## Deployment-Time Config (Centralized)
Set values in one file:
- `frontend/assets/js/deploy-config.js`

Fields:
- `apiBaseUrl`
- `cognitoDomain`
- `cognitoClientId`
- `cognitoRedirectUri`
- `cognitoLogoutUri`
- `cognitoScopes`

Resolution order used by the app:
1. `deploy-config.js` value (recommended for deployed website)
2. `localStorage` override (useful for local testing)
3. local fallback defaults

## Notes
- Auth is bearer-token based (`Authorization: Bearer ...`).
- Frontend auth uses Cognito Hosted UI with authorization code + PKCE.
- This is a static MVP shell for demoing service capabilities.
- SSE is represented in UI badges/placeholders; production SSE wiring can be added later.
