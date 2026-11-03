# Fill in your team's values before running terraform init -backend-config=backend.hcl
# See the Terraform Collaboration Guide for bucket naming convention:
#   csse6400-t{TEAM}-{ACCOUNT_ID}-{REGION}-tfstate

bucket       = "csse6400-t00-058264189730-us-east-1-tfstate"
key          = "spendsmart/terraform.tfstate"
region       = "us-east-1"
encrypt      = true
use_lockfile = true
profile      = "058264189730_CSSE6400-LabRole"
shared_credentials_file = "./credentials"
