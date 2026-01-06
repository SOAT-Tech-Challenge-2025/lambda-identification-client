package tech.buildrun.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class HandlerClient implements
        RequestHandler<APIGatewayV2HTTPEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = "tc-identification-table";

    private final ObjectMapper mapper = new ObjectMapper();
    private final DynamoDbClient dynamo = DynamoDbClient.create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayV2HTTPEvent request,
            Context context) {

        try {
            String path = request.getRawPath();
            String method = request.getRequestContext()
                    .getHttp()
                    .getMethod();

            context.getLogger().log("METHOD=" + method);
            context.getLogger().log("PATH=" + path);

            // POST /clientes
            if ("POST".equalsIgnoreCase(method) && "/clientes".equals(path)) {
                return criarCliente(request);
            }

            // GET /clientes/{document}
            if ("GET".equalsIgnoreCase(method) && path.startsWith("/clientes/")) {
                return consultarCliente(path);
            }

            return response(404, Map.of("message", "Endpoint não encontrado"));

        } catch (Exception e) {
            context.getLogger().log("ERRO: " + e.getMessage());
            return response(500, Map.of("message", "Erro interno"));
        }
    }

    /* ===================== CRIAR CLIENTE ===================== */

    private APIGatewayProxyResponseEvent criarCliente(
            APIGatewayV2HTTPEvent request) throws Exception {

        if (request.getBody() == null || request.getBody().isBlank()) {
            return response(400, Map.of("message", "Body obrigatório"));
        }

        Map<String, String> body =
                mapper.readValue(request.getBody(), Map.class);

        String document = body.get("document");
        String name = body.get("name");
        String email = body.get("email");

        if (document == null || name == null || email == null) {
            return response(400, Map.of(
                    "message", "document, name e email são obrigatórios"
            ));
        }

        if (clienteExistePorDocumento(document)) {
            return response(409, Map.of("message", "Cliente já existe"));
        }

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder()
                .s(UUID.randomUUID().toString()).build());
        item.put("nr_documento", AttributeValue.builder().s(document).build());
        item.put("nm_cliente", AttributeValue.builder().s(name).build());
        item.put("ds_email", AttributeValue.builder().s(email).build());

        dynamo.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());

        return response(201, Map.of(
                "message", "Cliente criado com sucesso",
                "document", document
        ));
    }

    /* ===================== CONSULTAR CLIENTE ===================== */

    private APIGatewayProxyResponseEvent consultarCliente(String path)
            throws Exception {

        // /clientes/123456
        String document = path.substring("/clientes/".length());

        QueryRequest query = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("DocumentoIndex")
                .keyConditionExpression("nr_documento = :doc")
                .expressionAttributeValues(Map.of(
                        ":doc", AttributeValue.builder().s(document).build()
                ))
                .limit(1)
                .build();

        QueryResponse result = dynamo.query(query);

        if (result.count() == 0) {
            return response(404, Map.of("message", "Cliente não encontrado"));
        }

        Map<String, AttributeValue> item = result.items().get(0);

        return response(200, Map.of(
                "id", item.get("id").s(),
                "document", item.get("nr_documento").s(),
                "name", item.get("nm_cliente").s(),
                "email", item.get("ds_email").s()
        ));
    }

    /* ===================== UTIL ===================== */

    private boolean clienteExistePorDocumento(String document) {

        QueryRequest query = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .indexName("DocumentoIndex")
                .keyConditionExpression("nr_documento = :doc")
                .expressionAttributeValues(Map.of(
                        ":doc", AttributeValue.builder().s(document).build()
                ))
                .limit(1)
                .build();

        return dynamo.query(query).count() > 0;
    }

    private APIGatewayProxyResponseEvent response(int status, Object body) {
        try {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(status)
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*",
                            "Access-Control-Allow-Headers", "Content-Type,Authorization",
                            "Access-Control-Allow-Methods", "GET,POST,OPTIONS"
                    ))
                    .withBody(mapper.writeValueAsString(body));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"message\":\"Erro ao serializar resposta\"}");
        }
    }
}
