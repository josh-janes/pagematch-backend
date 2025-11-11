#!/bin/bash
# deploy.sh - Robust deployment script with error handling

APP_NAME=pagematch-api
ENV_NAME=pagematch-api-prod

# +++ START DEBUGGING BLOCK +++
echo "==============================================="
echo "               DEBUGGING INFO                  "
echo "==============================================="
echo "Current User: $(whoami)"
echo "AWS_PROFILE variable is: '$AWS_PROFILE'"
echo "Checking AWS Identity from within the script:"
aws sts get-caller-identity || echo "ERROR: aws sts get-caller-identity FAILED"
echo "==============================================="
# +++ END DEBUGGING BLOCK +++


set -e  # Exit on error
set -u  # Exit on undefined variable
set -o pipefail  # Exit on pipe failure

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
TERRAFORM_DIR="./terraform"
MAX_WAIT_TIME=1200  # 20 minutes
CHECK_INTERVAL=30   # 30 seconds

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v terraform &> /dev/null; then
        log_error "Terraform is not installed. Please install it first."
        exit 1
    fi

    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI is not installed. Please install it first."
        exit 1
    fi

    if ! aws sts get-caller-identity &> /dev/null; then
        log_error "AWS credentials are not configured properly."
        exit 1
    fi

    log_info "Prerequisites check passed ✓"
}

validate_terraform() {
    log_info "Validating Terraform configuration..."
    cd "$TERRAFORM_DIR"

    if ! terraform fmt -check; then
        log_warn "Terraform files need formatting. Running terraform fmt..."
        terraform fmt
    fi

    if ! terraform validate; then
        log_error "Terraform validation failed"
        exit 1
    fi

    log_info "Terraform configuration is valid ✓"
    cd - > /dev/null
}

plan_deployment() {
    log_info "Creating deployment plan..."
    cd "$TERRAFORM_DIR"

    terraform plan -out=tfplan

    log_info "Deployment plan created ✓"
    log_warn "Review the plan above carefully before proceeding."
    read -p "Do you want to apply this plan? (yes/no): " -r REPLY
    echo

    if [[ ! $REPLY =~ ^[Yy]es$ ]]; then
        log_warn "Deployment cancelled by user"
        rm -f tfplan
        cd - > /dev/null
        exit 0
    fi

    cd - > /dev/null
}

apply_deployment() {
    log_info "Applying deployment..."
    cd "$TERRAFORM_DIR"

    if ! terraform apply tfplan; then
        log_error "Terraform apply failed"
        rm -f tfplan
        cd - > /dev/null
        exit 1
    fi

    rm -f tfplan
    log_info "Terraform apply completed ✓"
    cd - > /dev/null
}

get_environment_info() {
    cd "$TERRAFORM_DIR"
    ENV_NAME=$(terraform output -raw environment_name 2>/dev/null || echo "")
    APP_NAME=$(terraform output -raw application_name 2>/dev/null || echo "")
    cd - > /dev/null

    if [[ -z "$ENV_NAME" || -z "$APP_NAME" ]]; then
        log_error "Could not get environment information from Terraform outputs"
        exit 1
    fi
}

wait_for_environment() {
    log_info "Waiting for environment to become healthy..."

    local elapsed=0
    local status=""

    while [ $elapsed -lt $MAX_WAIT_TIME ]; do
        status=$(aws elasticbeanstalk describe-environments \
            --application-name "$APP_NAME" \
            --environment-names "$ENV_NAME" \
            --query 'Environments[0].Health' \
            --output text 2>/dev/null || echo "Unknown")

        case "$status" in
            "Green")
                log_info "Environment is healthy ✓"
                return 0
                ;;
            "Yellow")
                log_warn "Environment status: Yellow (Warning)"
                ;;
            "Red")
                log_error "Environment status: Red (Error)"
                log_error "Check the AWS console for details"
                return 1
                ;;
            "Grey")
                log_info "Environment is updating..."
                ;;
            *)
                log_warn "Environment status: $status"
                ;;
        esac

        sleep $CHECK_INTERVAL
        elapsed=$((elapsed + CHECK_INTERVAL))
        log_info "Waiting... ($elapsed/$MAX_WAIT_TIME seconds)"
    done

    log_error "Timeout waiting for environment to become healthy"
    return 1
}

get_recent_events() {
    log_info "Fetching recent events..."

    aws elasticbeanstalk describe-events \
        --application-name "$APP_NAME" \
        --environment-name "$ENV_NAME" \
        --max-records 20 \
        --query 'Events[*].[EventDate,Severity,Message]' \
        --output table
}

show_deployment_info() {
    cd "$TERRAFORM_DIR"

    log_info "=== Deployment Complete ==="
    echo ""
    log_info "Environment URL: $(terraform output -raw environment_url)"
    log_info "Environment CNAME: $(terraform output -raw environment_cname)"
    echo ""
    log_info "Load Balancer DNS (point your domain here):"
    terraform output load_balancer_dns
    echo ""
    log_info "To check logs:"
    echo "  aws elasticbeanstalk retrieve-environment-info --environment-name $ENV_NAME --info-type tail"
    echo ""
    log_info "To SSH into instance:"
    echo "  eb ssh $ENV_NAME"

    cd - > /dev/null
}

rollback_deployment() {
    log_error "Deployment failed. Do you want to rollback?"
    read -p "Rollback to previous version? (yes/no): " -r REPLY
    echo

    if [[ $REPLY =~ ^[Yy]es$ ]]; then
        log_info "Rolling back..."

        # Get previous version
        PREV_VERSION=$(aws elasticbeanstalk describe-application-versions \
            --application-name "$APP_NAME" \
            --max-records 2 \
            --query 'ApplicationVersions[1].VersionLabel' \
            --output text)

        if [[ -n "$PREV_VERSION" && "$PREV_VERSION" != "None" ]]; then
            aws elasticbeanstalk update-environment \
                --application-name "$APP_NAME" \
                --environment-name "$ENV_NAME" \
                --version-label "$PREV_VERSION"

            log_info "Rollback initiated. Waiting for completion..."
            wait_for_environment
        else
            log_error "Could not find previous version to rollback to"
        fi
    fi
}

cleanup() {
    if [ $? -ne 0 ]; then
        log_error "Deployment failed!"
        get_recent_events
        rollback_deployment
    fi
}


sync_env_variables() {
    log_info "Syncing environment variables from .env file..."

    # Check if .env file exists in the parent directory
    if [ ! -f ".env" ]; then
        log_warn ".env file not found, skipping sync."
        return
    fi

    # Read the .env file line by line, filter out comments and empty lines
    # and pass to the eb setenv command.
    eb setenv $(grep -v '^#' .env | grep -v '^$' | xargs) --environment "$ENV_NAME"

    log_info "Environment variables synced ✓"
}

# Main execution
main() {
    trap cleanup EXIT

    log_info "Starting deployment process..."
    echo ""

    check_prerequisites
    validate_terraform
    plan_deployment
    apply_deployment

    get_environment_info

    if wait_for_environment; then
        show_deployment_info
        log_info "Deployment successful! 🎉"
    else
        log_error "Deployment verification failed"
        get_recent_events
        exit 1
    fi

    #sync_env_variables
}

# Run main function
main "$@"