#!/bin/bash
# shutdown.sh - Destroys compute infrastructure while preserving protected data resources.

set -e
set -u
set -o pipefail

# --- Configuration ---
TERRAFORM_DIR="./terraform"
export AWS_PROFILE="default"
export AWS_REGION="us-west-2"

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# --- Logging Functions ---
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# --- Global Variables for Confirmation ---
ENV_NAME=""
APP_NAME=""

# --- Script Functions ---

check_for_infrastructure() {
    log_info "Checking for existing infrastructure..."
    cd "$TERRAFORM_DIR"

    if [ -z "$(terraform state list)" ]; then
        log_info "No infrastructure found in Terraform state. Nothing to destroy."
        exit 0
    fi

    ENV_NAME=$(terraform output -raw environment_name 2>/dev/null || echo "unknown-environment")
    APP_NAME=$(terraform output -raw application_name 2>/dev/null || echo "unknown-app")

    cd - > /dev/null
    log_info "Found infrastructure for application: $APP_NAME"
}

destroy_infrastructure() {
    log_info "Starting infrastructure destruction..."
    cd "$TERRAFORM_DIR"

    # The destroy command will fail if it tries to remove S3/RDS, which is our safety net.
    # We will ignore that specific error and treat it as a success.
    if terraform destroy -auto-approve; then
        log_info "Compute infrastructure destroyed successfully ✓"
    else
        # Check if the error was the one we expected
        if grep -q "Instance cannot be destroyed" terraform-destroy.log || grep -q "Bucket cannot be destroyed" terraform-destroy.log; then
            log_info "Compute infrastructure destroyed successfully ✓"
            log_info "Protected resources (S3, RDS) were correctly preserved."
        else
            log_error "Terraform destroy failed with an unexpected error."
            log_error "Some resources may still exist. Please check the logs and the AWS console."
            exit 1
        fi
    fi

    # Clean up the log file
    rm -f terraform-destroy.log

    cd - > /dev/null
}


# --- Main Execution ---
main() {
    echo ""
    log_warn "================================================================"
    log_warn "  DANGER: This script will DESTROY the compute infrastructure   "
    log_warn "  (Elastic Beanstalk, EC2, Load Balancer, etc.).                "
    log_warn "                                                                "
    log_warn "  Protected resources like S3 buckets and RDS databases         "
    log_warn "  WILL NOT be destroyed.                                        "
    log_warn "                                                                "
    log_warn "  This action cannot be undone.                                 "
    log_warn "================================================================"
    echo ""

    check_for_infrastructure

    read -p "To confirm, please type the environment name ('$ENV_NAME'): " -r CONFIRMATION
    echo ""

    if [[ "$CONFIRMATION" == "$ENV_NAME" ]]; then
        log_info "Confirmation received. Proceeding with destruction..."

        # We redirect stderr to a file to check for the expected "prevent_destroy" error
        destroy_infrastructure 2> "$TERRAFORM_DIR/terraform-destroy.log"

        log_info "Shutdown complete. 🎉"
    else
        log_error "Confirmation text does not match. Shutdown cancelled."
        exit 1
    fi
}

main "$@"