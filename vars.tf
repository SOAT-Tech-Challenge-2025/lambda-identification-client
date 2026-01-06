# Região da AWS
variable "region" {
  default = "us-east-1"
}

# Tags para os recursos
variable "tags" {
  default = {
    Environment = "PRD"
    Project     = "tc-lambda-identification-client"
  }
}

variable "lambda_jar_path" {
  description = "Caminho do fat JAR da Lambda"
  type        = string
  default     = "app/target/lambda-identification-client.jar"
}
