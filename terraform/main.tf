# main.tf - Complete Terraform configuration for Elastic Beanstalk with Docker

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }
}

provider "aws" {
  region  = var.aws_region
  profile = "default"
}

# Variables
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "pagematch-api"
}

variable "environment_name" {
  description = "Environment name"
  type        = string
  default     = "pagematch-api-prod"
}

variable "acm_certificate_arn" {
  description = "ACM Certificate ARN for HTTPS"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
  default     = "vpc-0c61e04c25424a09b"
}

variable "public_subnets" {
  description = "List of public subnet IDs (minimum 2 for ALB)"
  type        = list(string)
}

variable "key_pair_name" {
  description = "EC2 Key Pair name for SSH access"
  type        = string
  default     = ""
}

variable "gcp_credentials_s3_key" {
  description = "S3 key for GCP credentials file"
  type        = string
  default     = "gcp-credentials.json"
}

variable "s3_bucket_name" {
  description = "S3 bucket for application versions and credentials"
  type        = string
  default     = "pagematch-private-bucket"
}

variable "jwt_secret" {
  description = "The secret key for signing JWTs"
  type        = string
  sensitive   = true
}

# Data sources
data "aws_caller_identity" "current" {}

data "aws_vpc" "selected" {
  id = "vpc-0c61e04c25424a09b"
}

# IAM Role for Elastic Beanstalk Service
resource "aws_iam_role" "eb_service_role" {
  name = "${var.app_name}-eb-service-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "elasticbeanstalk.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_security_group" "alb" {
  name        = "${var.app_name}-alb-sg"
  description = "Controls access to the Application Load Balancer"
  vpc_id      = data.aws_vpc.selected.id

  # Allow HTTP and HTTPS traffic from the internet (and your CDN IPs)
  ingress {
    protocol    = "tcp"
    from_port   = 80
    to_port     = 80
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443
    cidr_blocks = [ # Add your list of CDN IPs here if needed
      "0.0.0.0/0"
    ]
  }

  # Allow all outbound traffic
  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "instance" {
  name        = "${var.app_name}-instance-sg"
  description = "Security group for the EC2 instances"
  vpc_id      = data.aws_vpc.selected.id

  # --- Inbound Rules (Ingress) ---

  # Allow SSH access from your specific IP
  ingress {
    protocol    = "tcp"
    from_port   = 22
    to_port     = 22
    cidr_blocks = ["38.55.66.116/32"]
    description = "Allow my IP for SSH access"
  }

  # Allow direct access to the Spring Boot application port
  ingress {
    protocol    = "tcp"
    from_port   = 5000
    to_port     = 5000
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow public IPv4 access to Spring"
  }
  ingress {
    protocol         = "tcp"
    from_port        = 5000
    to_port          = 5000
    ipv6_cidr_blocks = ["::/0"]
    description      = "Allow public IPv6 access to Spring"
  }

  # Allow HTTPS from various services (e.g., Cloudflare) on IPv4
  ingress {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443
    cidr_blocks = [
      "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22", "104.16.0.0/13",
      "104.24.0.0/14", "108.162.192.0/18", "131.0.72.0/22", "141.101.64.0/18",
      "162.158.0.0/15", "172.64.0.0/13", "173.245.48.0/20", "188.114.96.0/20",
      "190.93.240.0/20", "197.234.240.0/22", "198.41.128.0/17", "0.0.0.0/0"
    ]
    description = "Allow HTTPS from various IPv4 sources"
  }

  # Allow HTTPS from various services (e.g., Cloudflare) on IPv6
  ingress {
    protocol  = "tcp"
    from_port = 443
    to_port   = 443
    ipv6_cidr_blocks = [
      "2400:cb00::/32", "2405:8100::/32", "2405:b500::/32", "2606:4700::/32",
      "2803:f800::/32", "2a06:98c0::/29", "2c0f:f248::/32"
    ]
    description = "Allow HTTPS from various IPv6 sources"
  }

  # --- Outbound Rules (Egress) ---
  egress {
    protocol    = "-1" # -1 means all protocols
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound IPv4 traffic"
  }
  egress {
    protocol         = "-1"
    from_port        = 0
    to_port          = 0
    ipv6_cidr_blocks = ["::/0"]
    description      = "Allow all outbound IPv6 traffic"
  }
}

resource "aws_iam_role_policy_attachment" "eb_service_health" {
  role       = aws_iam_role.eb_service_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSElasticBeanstalkEnhancedHealth"
}

resource "aws_iam_role_policy_attachment" "eb_service_managed" {
  role       = aws_iam_role.eb_service_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy"
}

# IAM Role for EC2 Instances
resource "aws_iam_role" "eb_ec2_role" {
  name = "${var.app_name}-eb-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "eb_ec2_web_tier" {
  role       = aws_iam_role.eb_ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier"
}

resource "aws_iam_role_policy_attachment" "eb_ec2_worker_tier" {
  role       = aws_iam_role.eb_ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkWorkerTier"
}

resource "aws_iam_role_policy_attachment" "eb_ec2_multicontainer" {
  role       = aws_iam_role.eb_ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkMulticontainerDocker"
}

# Custom policy for S3 access to GCP credentials
resource "aws_iam_role_policy" "eb_ec2_s3_access" {
  name = "${var.app_name}-s3-access"
  role = aws_iam_role.eb_ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::${var.s3_bucket_name}",
          "arn:aws:s3:::${var.s3_bucket_name}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_instance_profile" "eb_ec2_profile" {
  name = "${var.app_name}-eb-ec2-profile"
  role = aws_iam_role.eb_ec2_role.name
}

# Elastic Beanstalk Application
resource "aws_elastic_beanstalk_application" "app" {
  name        = var.app_name
  description = "PageMatch API Application"

  appversion_lifecycle {
    service_role          = aws_iam_role.eb_service_role.arn
    max_count             = 10
    delete_source_from_s3 = true
  }
}

data "archive_file" "app_source" {
  type        = "zip"
  source_dir  = "${path.module}/../"
  output_path = "${path.module}/${var.app_name}.zip" # Using temp directory is best practice

  excludes = toset([
    ".git",
    ".terraform",
    "terraform",
    "*.tfstate",
    "*.tfstate.backup",
    ".DS_Store",
    ".gradle",
    "docker-compose.yml",
    ".idea",
    "*.log",
    "gcp-credentials.json",
    ".env",
    "*.rar",
    "**/*.zip",
    "aws",
    ".kotlin",
    "scripts",
    "build/classes/*",
    "build/kotlin/*"
  ])
}

resource "aws_s3_object" "app_version" {
  bucket = var.s3_bucket_name
  key    = "app-versions/${var.app_name}-${formatdate("YYYYMMDDHHmmss", timestamp())}.zip"
  source = data.archive_file.app_source.output_path
  etag   = filemd5(data.archive_file.app_source.output_path)

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_elastic_beanstalk_application_version" "app_version" {
  name        = "${var.app_name}-${formatdate("YYYYMMDDHHmmss", timestamp())}"
  application = aws_elastic_beanstalk_application.app.name
  description = "Application version deployed via Terraform"
  bucket      = var.s3_bucket_name
  key         = aws_s3_object.app_version.key

  lifecycle {
    create_before_destroy = true
  }
}

# Elastic Beanstalk Environment
resource "aws_elastic_beanstalk_environment" "env" {
  name                = var.environment_name
  application         = aws_elastic_beanstalk_application.app.name
  solution_stack_name = "64bit Amazon Linux 2023 v4.7.4 running Docker"
  version_label       = aws_elastic_beanstalk_application_version.app_version.name

  setting {
    namespace = "aws:ec2:vpc"
    name      = "AssociatePublicIpAddress"
    value     = "false" # Instances in private subnets
  }
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "SecurityGroups"
    value     = aws_security_group.instance.id
  }
  setting {
    namespace = "aws:elbv2:loadbalancer"
    name      = "SecurityGroups"
    value     = aws_security_group.alb.id
  }

  # Service roles
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "ServiceRole"
    value     = aws_iam_role.eb_service_role.arn
  }

  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "IamInstanceProfile"
    value     = aws_iam_instance_profile.eb_ec2_profile.name
  }

  # Environment type - Load balanced
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "EnvironmentType"
    value     = "LoadBalanced"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "LoadBalancerType"
    value     = "application"
  }

  # VPC Configuration
  setting {
    namespace = "aws:ec2:vpc"
    name      = "VPCId"
    value     = var.vpc_id
  }

  setting {
    namespace = "aws:ec2:vpc"
    name      = "Subnets"
    value     = join(",", var.public_subnets)
  }

  setting {
    namespace = "aws:ec2:vpc"
    name      = "ELBScheme"
    value     = "public"
  }

  setting {
    namespace = "aws:ec2:vpc"
    name      = "ELBSubnets"
    value     = join(",", var.public_subnets)
  }

  setting {
    namespace = "aws:ec2:vpc"
    name      = "AssociatePublicIpAddress"
    value     = "true"
  }

  # Instance configuration
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "InstanceType"
    value     = var.instance_type
  }

  dynamic "setting" {
    for_each = var.key_pair_name != "" ? [1] : []
    content {
      namespace = "aws:autoscaling:launchconfiguration"
      name      = "EC2KeyName"
      value     = var.key_pair_name
    }
  }

  # Auto Scaling
  setting {
    namespace = "aws:autoscaling:asg"
    name      = "MinSize"
    value     = "1"
  }

  setting {
    namespace = "aws:autoscaling:asg"
    name      = "MaxSize"
    value     = "1"
  }

  # Health reporting
  setting {
    namespace = "aws:elasticbeanstalk:healthreporting:system"
    name      = "SystemType"
    value     = "enhanced"
  }

  # Process configuration
  setting {
    namespace = "aws:elasticbeanstalk:environment:process:default"
    name      = "HealthCheckPath"
    value     = "/actuator/health"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment:process:default"
    name      = "Port"
    value     = "5000"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment:process:default"
    name      = "Protocol"
    value     = "HTTP"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment:process:default"
    name      = "HealthCheckInterval"
    value     = "30"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment:process:default"
    name      = "HealthCheckTimeout"
    value     = "5"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment:process:default"
    name      = "HealthyThresholdCount"
    value     = "3"
  }

  setting {
    namespace = "aws:elasticbeanstalk:environment:process:default"
    name      = "UnhealthyThresholdCount"
    value     = "3"
  }

  # HTTP Listener (port 80)
  setting {
    namespace = "aws:elbv2:listener:80"
    name      = "Protocol"
    value     = "HTTP"
  }

  setting {
    namespace = "aws:elbv2:listener:80"
    name      = "DefaultProcess"
    value     = "default"
  }

  # HTTPS Listener (port 443) with ACM certificate
  setting {
    namespace = "aws:elbv2:listener:443"
    name      = "Protocol"
    value     = "HTTPS"
  }

  setting {
    namespace = "aws:elbv2:listener:443"
    name      = "SSLCertificateArns"
    value     = var.acm_certificate_arn
  }

  setting {
    namespace = "aws:elbv2:listener:443"
    name      = "DefaultProcess"
    value     = "default"
  }

  # Environment variables
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "GOOGLE_APPLICATION_CREDENTIALS"
    value     = "/etc/gcp/gcp_credentials.json"
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "GOOGLE_AI_PROJECT_ID"
    value     = "gen-lang-client-0644455582"
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "GOOGLE_AI_LOCATION"
    value     = "us-central1"
  }

  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "GOOGLE_AI_MODEL"
    value     = "gemini-1.5-flash"
  }

  # Deployment settings
  setting {
    namespace = "aws:elasticbeanstalk:command"
    name      = "DeploymentPolicy"
    value     = "Rolling"
  }

  setting {
    namespace = "aws:elasticbeanstalk:command"
    name      = "BatchSizeType"
    value     = "Fixed"
  }

  setting {
    namespace = "aws:elasticbeanstalk:command"
    name      = "BatchSize"
    value     = "1"
  }

  # Logging
  setting {
    namespace = "aws:elasticbeanstalk:cloudwatch:logs"
    name      = "StreamLogs"
    value     = "true"
  }

  setting {
    namespace = "aws:elasticbeanstalk:cloudwatch:logs"
    name      = "DeleteOnTerminate"
    value     = "false"
  }

  setting {
    namespace = "aws:elasticbeanstalk:cloudwatch:logs"
    name      = "RetentionInDays"
    value     = "7"
  }

  # Managed updates
  setting {
    namespace = "aws:elasticbeanstalk:managedactions"
    name      = "ManagedActionsEnabled"
    value     = "false"
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "JWT_SECRET"
    value     = var.jwt_secret
  }

  lifecycle {
    create_before_destroy = true
    ignore_changes = [
      version_label
    ]
  }

  depends_on = [
    aws_iam_role_policy_attachment.eb_service_health,
    aws_iam_role_policy_attachment.eb_ec2_web_tier,
    aws_security_group.instance,
    aws_security_group.alb
  ]
}

# Outputs
output "environment_url" {
  description = "Elastic Beanstalk environment URL"
  value       = aws_elastic_beanstalk_environment.env.endpoint_url
}

output "environment_cname" {
  description = "Elastic Beanstalk environment CNAME"
  value       = aws_elastic_beanstalk_environment.env.cname
}

output "load_balancer_dns" {
  description = "Load Balancer DNS name (point your domain here)"
  value       = aws_elastic_beanstalk_environment.env.load_balancers
}

output "application_name" {
  description = "Elastic Beanstalk application name"
  value       = aws_elastic_beanstalk_application.app.name
}

output "environment_name" {
  description = "Elastic Beanstalk environment name"
  value       = aws_elastic_beanstalk_environment.env.name
}

output "application_version_label" {
  description = "The label of the newly created application version."
  value       = aws_elastic_beanstalk_application_version.app_version.name
}