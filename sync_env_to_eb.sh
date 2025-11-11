#!/bin/bash
# Usage: ./sync_env_to_eb.sh <EB_ENV_NAME>

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <EB_ENV_NAME>"
  exit 1
fi

EB_ENV="$1"

# Check if .env exists
if [ ! -f .env ]; then
  echo ".env file not found!"
  exit 1
fi

echo "Updating environment variables for EB environment: $EB_ENV"

# Build a list of KEY=VALUE pairs (skip empty lines/comments)
ENV_VARS=()
while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^#.*$ || -z "$line" ]] && continue
    ENV_VARS+=("$line")
done < .env

# Convert array to space-separated string
ENV_VARS_STR="${ENV_VARS[@]}"

# Apply env vars
echo "Running: eb setenv $ENV_VARS_STR --environment $EB_ENV"
eb setenv $ENV_VARS_STR --environment "$EB_ENV"

echo "Environment variables updated successfully!"
