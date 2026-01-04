data "aws_iam_role" "lambda_exec_role" {
  name = "tc-infra-id-lambda-exec-role"
}


data "aws_dynamodb_table" "identification_table" {
  name = "tc-identification-table"
}

resource "aws_iam_role_policy" "lambda_dynamodb_policy" {
  role = data.aws_iam_role.lambda_exec_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:PutItem",
        "dynamodb:Query",
        "dynamodb:GetItem"
      ]
      Resource = [
        data.aws_dynamodb_table.identification_table.arn,
        "${data.aws_dynamodb_table.identification_table.arn}/index/*"
      ]
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = data.aws_iam_role.lambda_exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

data "aws_security_group" "id_lambda" {
  name  = "tc-id-lambda-sg"
  vpc_id = data.aws_vpc.tc_lambda_vpc.id
}

resource "aws_lambda_function" "id_lambda" {
  function_name = "lambda-identification-client"

  role    = data.aws_iam_role.lambda_exec_role.arn
  handler = "tech.buildrun.lambda.HandlerClient::handleRequest"
  runtime = "java17"
  timeout = 30

  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      TABLE_NAME = "tc-identification-table"
    }
  }

  vpc_config {
    subnet_ids         = data.aws_subnets.tc_lambda_subnets.ids
    security_group_ids = [data.aws_security_group.id_lambda.id]
  }

  tags = var.tags
}

resource "aws_lambda_permission" "apigw_invoke_lambda" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.id_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${data.aws_apigatewayv2_api.tc_api.execution_arn}/*/*"
}
