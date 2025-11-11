# Complete Terraform Deployment Guide

## Project Structure

```
your-project/
├── terraform/
│   ├── main.tf                    # Main Terraform configuration
│   ├── terraform.tfvars           # Your actual values (DO NOT commit)
│   └── terraform.tfvars.example   # Example template
├── .ebextensions/
│   └── 01_app_setup.config
├── .platform/
│   └── nginx/
│       └── conf.d/
│           ├── cors.conf
│           └── elasticbeanstalk/
│               └── 00_application.conf
├── Dockerfile
├── deploy.sh                      # Initial deployment script
├── update.sh                      # Update existing deployment
└── (your application code)
```

---

## Prerequisites

### 1. Install Required Tools

```bash
# Install Terraform
brew install terraform  # macOS
# or
wget https://releases.hashicorp.com/terraform/1.6.0/terraform_1.6.0_linux_amd64.zip
unzip terraform_1.6.0_linux_amd64.zip
sudo mv terraform /usr/local/bin/

# Install AWS CLI
brew install awscli  # macOS
# or
pip install awscli

# Verify installations
terraform --version
aws --version
```

### 2. Configure AWS Credentials

```bash
# Configure AWS CLI
aws configure

# Or set environment variables
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_DEFAULT_REGION="us-east-1"

# Verify
aws sts get-caller-identity
```

### 3. Get Required Information

You need to gather:

1. **ACM Certificate ARN**:
   ```bash
   aws acm list-certificates --region us-east-1
   ```
   Copy the ARN: `arn:aws:acm:us-east-1:123456789:certificate/abc-123...`

2. **VPC ID**:
   ```bash
   aws ec2 describe-vpcs --query 'Vpcs[*].[VpcId,Tags[?Key==`Name`].Value|[0]]' --output table
   ```

3. **Subnet IDs** (need at least 2 in different AZs):
   ```bash
   aws ec2 describe-subnets --filters "Name=vpc-id,Values=vpc-xxxxx" \
     --query 'Subnets[*].[SubnetId,AvailabilityZone,CidrBlock]' --output table
   ```

4. **S3 Bucket** (create if needed):
   ```bash
   aws s3 mb s3://pagematch-private-bucket
   
   # Upload GCP credentials
   aws s3 cp gcp-credentials.json s3://pagematch-private-bucket/
   ```

---

## Setup Steps

### Step 1: Create Terraform Configuration

```bash
# Create terraform directory
mkdir -p terraform
cd terraform

# Copy the main.tf file from the artifact above
# Save it as main.tf
```

### Step 2: Create terraform.tfvars

```bash
# Create terraform.tfvars with your actual values
cat > terraform.tfvars <<EOF
aws_region            = "us-east-1"
app_name              = "pagematch-api"
environment_name      = "pagematch-api-prod"
acm_certificate_arn   = "arn:aws:acm:us-east-1:YOUR-ACCOUNT-ID:certificate/YOUR-CERT-ID"
instance_type         = "t3.small"
vpc_id                = "vpc-xxxxx"
public_subnets        = ["subnet-xxxxx", "subnet-yyyyy"]
key_pair_name         = "your-key-pair"
s3_bucket_name        = "pagematch-private-bucket"
gcp_credentials_s3_key = "gcp-credentials.json"
EOF

# Add to .gitignore
echo "terraform.tfvars" >> ../.gitignore
echo "*.tfstate*" >> ../.gitignore
echo ".terraform/" >> ../.gitignore
```

### Step 3: Initialize Terraform

```bash
cd terraform

# Initialize Terraform (downloads providers)
terraform init

# Validate configuration
terraform validate

# Format configuration
terraform fmt
```

---

## Initial Deployment

### Option A: Using the deploy.sh Script (Recommended)

```bash
# Make scripts executable
chmod +x deploy.sh update.sh

# Run initial deployment
./deploy.sh
```

The script will:
1. ✅ Check prerequisites
2. ✅ Validate Terraform configuration
3. ✅ Show deployment plan
4. ✅ Ask for confirmation
5. ✅ Apply changes
6. ✅ Wait for environment to be healthy
7. ✅ Display deployment info
8. ✅ Offer rollback on failure

### Option B: Manual Terraform Commands

```bash
cd terraform

# Preview changes
terraform plan

# Apply changes
terraform apply

# Wait for completion (check AWS console)
```

---

## Updating Your Application

### Option A: Using update.sh (Recommended)

```bash
# After making code changes
./update.sh
```

The script will:
1. ✅ Package your application
2. ✅ Create new version in S3
3. ✅ Deploy to existing environment
4. ✅ Wait for deployment to complete
5. ✅ Show deployment status
6. ✅ Offer rollback on failure

### Option B: Manual Update

```bash
cd terraform

# Taint the version resources to force recreation
terraform taint aws_s3_object.app_version
terraform taint aws_elastic_beanstalk_application_version.app_version

# Apply changes
terraform apply -auto-approve \
  -target=data.archive_file.app_source \
  -target=aws_s3_object.app_version \
  -target=aws_elastic_beanstalk_application_version.app_version

# Get the new version label
VERSION_LABEL=$(terraform output -json | jq -r '.application_name.value')

# Deploy new version
aws elasticbeanstalk update-environment \
  --environment-name pagematch-api-prod \
  --version-label "$VERSION_LABEL"
```

---

## Monitoring and Troubleshooting

### Check Environment Status

```bash
# Get environment health
aws elasticbeanstalk describe-environments \
  --environment-names pagematch-api-prod \
  --query 'Environments[0].[EnvironmentName,Status,Health]' \
  --output table

# Get recent events
aws elasticbeanstalk describe-events \
  --environment-name pagematch-api-prod \
  --max-records 20 \
  --query 'Events[*].[EventDate,Severity,Message]' \
  --output table
```

### View Logs

```bash
# Request logs
aws elasticbeanstalk request-environment-info \
  --environment-name pagematch-api-prod \
  --info-type tail

# Wait a few seconds, then retrieve
aws elasticbeanstalk retrieve-environment-info \
  --environment-name pagematch-api-prod \
  --info-type tail

# Or use EB CLI
eb logs pagematch-api-prod --stream
```

### SSH into Instance

```bash
# Get instance ID
INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:elasticbeanstalk:environment-name,Values=pagematch-api-prod" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text)

# SSH using SSM (no key needed)
aws ssm start-session --target "$INSTANCE_ID"

# Or using key pair
ssh -i your-key.pem ec2-user@$(aws ec2 describe-instances \
  --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)
```

---

## Rollback Procedures

### Automatic Rollback (via update.sh)

The update script offers automatic rollback on failure.

### Manual Rollback

```bash
# List versions
aws elasticbeanstalk describe-application-versions \
  --application-name pagematch-api \
  --query 'ApplicationVersions[*].[VersionLabel,DateCreated]' \
  --output table

# Deploy previous version
aws elasticbeanstalk update-environment \
  --environment-name pagematch-api-prod \
  --version-label "pagematch-api-20241107120000"
```

---

## Cost Management

### View Current Costs

```bash
# Estimate monthly cost
# - ALB: ~$16/month
# - t3.small: ~$15/month (24/7)
# - Data transfer: Variable
# Total: ~$31+/month

# To reduce costs:
# 1. Use t3.micro instead of t3.small (~$7.5/month)
# 2. Set up auto-scaling to scale down during off-hours
# 3. Use Savings Plans or Reserved Instances
```

### Destroy Environment (to stop costs)

```bash
cd terraform

# Destroy everything
terraform destroy

# Or just the environment (keep application)
terraform destroy -target=aws_elastic_beanstalk_environment.env
```

---

## DNS Configuration

After deployment, update your DNS:

```bash
# Get ALB DNS name
cd terraform
terraform output load_balancer_dns

# In your DNS provider (e.g., Route 53):
# Create CNAME record:
# Name: api.pagematch.ca
# Type: CNAME (or ALIAS if using Route 53)
# Value: your-env-123.us-east-1.elb.amazonaws.com
```

---

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Deploy to EB

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: us-east-1
      
      - name: Setup Terraform
        uses: hashicorp/setup-terraform@v2
      
      - name: Deploy
        run: |
          chmod +x update.sh
          ./update.sh
```

---

## Best Practices

1. **Always test in staging first**
   ```bash
   # Create staging environment
   terraform workspace new staging
   terraform apply -var="environment_name=pagematch-api-staging"
   ```

2. **Use remote state for team collaboration**
   ```hcl
   # In main.tf
   backend "s3" {
     bucket = "pagematch-terraform-state"
     key    = "pagematch-api/terraform.tfstate"
     region = "us-east-1"
   }
   ```

3. **Version your deployments**
    - Git tag each deployment
    - Keep application versions in S3

4. **Monitor your application**
    - Set up CloudWatch alarms
    - Configure SNS notifications
    - Use AWS X-Ray for tracing

5. **Regular backups**
    - Backup database regularly
    - Keep multiple application versions
    - Document rollback procedures

---

## Quick Reference

```bash
# Initial deployment
./deploy.sh

# Update application
./update.sh

# Check status
aws elasticbeanstalk describe-environments --environment-names pagematch-api-prod

# View logs
aws elasticbeanstalk retrieve-environment-info --environment-name pagematch-api-prod --info-type tail

# Rollback
aws elasticbeanstalk update-environment --environment-name pagematch-api-prod --version-label PREVIOUS_VERSION

# Destroy
cd terraform && terraform destroy
```