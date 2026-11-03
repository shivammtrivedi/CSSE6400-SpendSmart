# SpendSmart

A cloud-based personal finance and expense-sharing web application for university students. SpendSmart lets you connect your bank accounts, track transactions, split expenses with friends, set budgets, and view spending trends — all in one place.

---

## What is SpendSmart?

SpendSmart is a microservices-based financial management platform with the following core features:

- **Expense Sharing** : Create groups, log shared expenses, and split costs across members
- **Settlement Tracking**:Link real bank transactions to expenses to mark them as settled
- **Bank Connectivity**: Connect via OBP (Open Bank Project) or BankApi to pull live transaction data
- **Budget Management**: Set spending limits by category and period (weekly/monthly)
- **Search & Filtering**:Filter transactions by date, amount, currency, and provider
- **Dashboard**:Visualise spending trends over time with interactive charts
- **Real-time Updates**: Live expense updates via Server-Sent Events (SSE) when group members make changes

---



## Using the Application

### Getting Started

1. **Sign In** L Navigate to the app and click **Sign In / Sign Up** on the landing page. Authentication is handled via AWS Cognito with a secure PKCE flow.

2. **Connect Your Bank**: Go to **Banking** in the sidebar and choose a provider:
   - **OBP**: Enter your Open Bank Project username, password, and consumer key
   - **BankApi**: Enter your BankApi email and password
   
   Once connected, SpendSmart can fetch your real transaction data.

3. **Create a Group** : Go to **Groups**, give it a name (e.g. "Flatmates", "Road Trip"), and add members by their user ID.

4. **Log Expenses**: Go to **Expenses**, select your group, and click **Create Expense**. Enter the total amount and split it across group members.

5. **Settle Up**: When someone pays their share, go to **Settlements**, pick the expense, and link it to a real bank transaction. SpendSmart fetches your transactions from your connected bank so you can select the matching one.

6. **Track Spending** :Use the **Dashboard** to view daily spend charts filtered by time period. Use **Search** to drill into transactions by date range, amount, currency, or provider.

7. **Set Budgets**:Go to **Budgets** to define spending limits per category (e.g. Groceries: $500/month).

### Navigation

| Page | Purpose |
|------|---------|
| Home | Overview, API config, quick shortcuts |
| Groups | Create groups, manage members |
| Expenses | View and create group expenses |
| Settlements | Record and review expense settlements |
| Dashboard | Spending charts and summaries |
| Search | Filter and persist bank transactions |
| Alerts | Budget and notification settings |
| Budgets | Set category spending limits |
| Banking | Connect/reconnect bank providers |

---


## Main Project Structure

```
services/
  alerts-service/         # Alert notifications and budget evaluation
  bank-api-nodejs/        # Mock banking API (Node.js + MongoDB)
  bank-auth-service/      # Bank credential storage (DynamoDB)
  banking-service/        # Transaction fetching via adapter pattern
  budget-service/         # Budget CRUD
  expense-service/        # Expense/settlement CRUD + SSE events
  group-service/          # Group and member management
  query-service/          # Transaction search and filtering
frontend/
  assets/                 # CSS, JS modules
  *.html                  # Application pages
common/
  shared-schema/          # Shared JPA entities (User, Group, Expense, etc.)
lambdas/
  signup-function/        # Cognito post-confirmation Lambda (creates user row in DB)
terraform/                # AWS infrastructure as code
docker-compose.yml        # Local dev databases
```

---


## Deploying to AWS

SpendSmart is deployed on AWS using Terraform. The infrastructure includes ECS Fargate (services), RDS PostgreSQL (database), DynamoDB (bank auth tokens), Cognito (authentication), an Application Load Balancer (API gateway), and CloudFront + S3 (static frontend).

### Prerequisites

- [Terraform](https://developer.hashicorp.com/terraform/install) v1.5+
- [Docker Desktop](https://www.docker.com/products/docker-desktop) (must be running — images are built and pushed during `terraform apply`)
- AWS credentials configured in `terraform/credentials` with a profile named `058264189730_CSSE6400-LabRole` (or update `terraform/variables.tf` to match your profile)
- An S3 bucket for Terraform state (see `terraform/backend.hcl`)

### 1. Configure backend state

Edit `terraform/backend.hcl` with your team's S3 bucket details:

```hcl
bucket       = "your-tfstate-bucket"
key          = "spendsmart/terraform.tfstate"
region       = "us-east-1"
encrypt      = true
use_lockfile = true
profile      = "your-aws-profile"
```


### 2. Initialise Terraform


```bash
cd terraform
terraform init -backend-config=backend.hcl
```

### 3. Configure variables

Optionally edit `terraform/variables.tf` to set:

- `cognito_domain_prefix` — must be globally unique (e.g. `spendsmart-yourteam-2026`)
- `db_password` — production database password
- `bank_api_token_key` — JWT secret for the BankApi service
- `image_tag` — Docker image tag (default: `latest`)




### 4. Deploy

```bash
terraform apply
```

This will:
1. Build all service Docker images locally
2. Push images to ECR
3. Provision RDS, DynamoDB, Cognito, ECS cluster, ALB, CloudFront, and S3
4. Upload the frontend to S3 with auto-generated `deploy-config.js` (Cognito + API URLs injected)
5. Register the Cognito post-confirmation Lambda (creates a user row in Postgres on first sign-up)

After apply completes, Terraform outputs the key URLs:

| Output | Description |
|--------|-------------|
| `frontend_website_url` | Public HTTPS URL for the app (CloudFront) |
| `gateway_url` | ALB DNS name (API backend) |
| `cognito_hosted_ui_url` | Direct Cognito login link |
| `cognito_client_id` | Cognito app client ID |
| `rds_endpoint` | PostgreSQL endpoint |

### 5. Access the app


Open `frontend_website_url` in your browser. Sign up for an account — Cognito will send a verification email, and the Lambda will automatically create your user record in the database.

### Tearing down

```bash
terraform destroy
```

---



## Service Documentation

Each service has its own README with API docs and local dev instructions:

- `services/alerts-service/README.md`
- `services/bank-api-nodejs/README.md`
- `services/bank-auth-service/README.md`
- `services/banking-service/README.md`
- `services/expense-service/README.md`
- `services/group-service/README.md`
- `services/query-service/README.md`

---



## Project Status

| Service | Status | Last Commit |
|---------|--------|-------------|
| alerts-service | Done | 4 days ago |
| bank-api-nodejs | Done | 2 days ago |
| bank-auth-service | Done | 2 days ago |
| banking-service | Done | 2 days ago |
| expense-service | Done | 2 days ago |
| group-service | Done | 3 days ago |
| query-service | Done | 4 days ago |

---

## Notes for Windows Users

Run commands from **WSL** (Windows Subsystem for Linux), not PowerShell.