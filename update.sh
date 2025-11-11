#!/bin/bash
# update.sh - Deploy new version of the application without recreating infrastructure

set -e
set -u
set -o pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

TERRAFORM_DIR="./terraform"
MAX_WAIT_TIME=1200
CHECK_INTERVAL=30
NEW_VERSION_LABEL=""
export AWS_PROFILE="default"
export AWS_REGION="us-west-2"

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_environment_exists() {
    log_info "Checking if environment exists..."

    cd "$TERRAFORM_DIR"
    ENV_NAME=$(terraform output -raw environment_name 2>/dev/null || echo "")
    APP_NAME=$(terraform output -raw application_name 2>/dev/null || echo "")
    cd - > /dev/null

    if [[ -z "$ENV_NAME" || -z "$APP_NAME" ]]; then
        log_error "Environment not found. Run './deploy.sh' first to create the environment."
        exit 1
    fi

    log_info "Environment found: $ENV_NAME"
}

create_new_version() {
    log_info "Building new application version..."

    # Build gradlew image
    ./gradlew build

    cd "$TERRAFORM_DIR"

    # Force recreation of the application version
    terraform taint aws_s3_object.app_version 2>/dev/null || true
    terraform taint aws_elastic_beanstalk_application_version.app_version 2>/dev/null || true

    # Apply only the version-related resources
    terraform apply -auto-approve \
        -target=data.archive_file.app_source \
        -target=aws_s3_object.app_version \
        -target=aws_elastic_beanstalk_application_version.app_version

    NEW_VERSION_LABEL=$(terraform output -raw application_version_label 2>/dev/null || echo "")

    cd - > /dev/null

    if [[ -z "$NEW_VERSION_LABEL" ]]; then
        log_error "Failed to create and get new application version label from Terraform."
        exit 1
    fi

    log_info "New version created: $NEW_VERSION_LABEL ✓"
}

deploy_new_version() {
    # Get the latest version label
    log_info "Deploying new version to environment..."

    # REMOVE THE OLD AWS CLI QUERY and use the variable directly.
    log_info "Deploying version: $NEW_VERSION_LABEL"

    aws elasticbeanstalk update-environment \
        --application-name "$APP_NAME" \
        --environment-name "$ENV_NAME" \
        --version-label "$NEW_VERSION_LABEL"

    cd - > /dev/null
}

wait_for_deployment() {
    log_info "Waiting for deployment to complete..."

    local elapsed=0
    local status=""
    local prev_status=""

    while [ $elapsed -lt $MAX_WAIT_TIME ]; do
        status=$(aws elasticbeanstalk describe-environments \
            --application-name "$APP_NAME" \
            --environment-names "$ENV_NAME" \
            --query 'Environments[0].Status' \
            --output text 2>/dev/null || echo "Unknown")

        health=$(aws elasticbeanstalk describe-environments \
            --application-name "$APP_NAME" \
            --environment-names "$ENV_NAME" \
            --query 'Environments[0].Health' \
            --output text 2>/dev/null || echo "Unknown")

        if [[ "$status" != "$prev_status" ]]; then
            log_info "Status: $status | Health: $health"
            prev_status="$status"
        fi

        case "$status" in
            "Ready")
                case "$health" in
                    "Green")
                        log_info "Deployment successful! Environment is healthy ✓"
                        return 0
                        ;;
                    "Yellow")
                        log_warn "Deployment complete but environment health is Yellow"
                        get_recent_events
                        return 0
                        ;;
                    "Red")
                        log_error "Deployment failed - environment health is Red"
                        get_recent_events
                        return 1
                        ;;
                esac
                ;;
            "Updating")
                log_info "Deploying... ($elapsed/$MAX_WAIT_TIME seconds)"
                ;;
            "Terminated"|"Terminating")
                log_error "Environment is being terminated"
                return 1
                ;;
        esac

        sleep $CHECK_INTERVAL
        elapsed=$((elapsed + CHECK_INTERVAL))
    done

    log_error "Timeout waiting for deployment"
    return 1
}

get_recent_events() {
    log_info "Recent events:"

    aws elasticbeanstalk describe-events \
        --application-name "$APP_NAME" \
        --environment-name "$ENV_NAME" \
        --max-records 10 \
        --query 'Events[*].[EventDate,Severity,Message]' \
        --output table
}

show_logs() {
    log_info "Fetching latest logs..."

    aws elasticbeanstalk request-environment-info \
        --environment-name "$ENV_NAME" \
        --info-type tail

    sleep 5

    aws elasticbeanstalk retrieve-environment-info \
        --environment-name "$ENV_NAME" \
        --info-type tail \
        --query 'EnvironmentInfo[0].Message' \
        --output text
}

rollback() {
    log_error "Deployment failed!"

    read -p "Do you want to rollback to the previous version? (yes/no): " -r REPLY
    echo

    if [[ $REPLY =~ ^[Yy]es$ ]]; then
        log_info "Rolling back..."

        PREV_VERSION=$(aws elasticbeanstalk describe-application-versions \
            --application-name "$APP_NAME" \
            --max-records 3 \
            --query 'ApplicationVersions[2].VersionLabel' \
            --output text)

        if [[ -n "$PREV_VERSION" && "$PREV_VERSION" != "None" ]]; then
            log_info "Rolling back to version: $PREV_VERSION"

            aws elasticbeanstalk update-environment \
                --application-name "$APP_NAME" \
                --environment-name "$ENV_NAME" \
                --version-label "$PREV_VERSION"

            log_info "Rollback initiated. Waiting for completion..."
            wait_for_deployment
        else
            log_error "Could not find previous version to rollback to"
            exit 1
        fi
    else
        log_info "Rollback cancelled"
        exit 1
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
    # IMPORTANT: This is a simple parser. It won't handle complex values with spaces.
    eb setenv $(grep -v '^#' .env | grep -v '^$' | xargs) --environment "$ENV_NAME"

    log_info "Environment variables synced ✓"
}

main() {
    log_info "Starting application update..."
    echo ""

    check_environment_exists
    #sync_env_variables
    create_new_version
    deploy_new_version

    if wait_for_deployment; then
        # REMOVED: cd "$TERRAFORM_DIR"
        log_info "=== Update Complete ==="
        # This command will work because you're already in the terraform directory
        log_info "Environment URL: $(terraform output -raw environment_url)"
        log_info "Check your application at: https://api.pagematch.ca"
        # REMOVED: cd - > /dev/null
        log_info "Update successful! 🎉"
    else
        rollback
    fi
}

main "$@"