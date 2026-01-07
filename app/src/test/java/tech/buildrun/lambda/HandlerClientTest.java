//package tech.buildrun.lambda;
//
//import com.amazonaws.services.lambda.runtime.Context;
//import com.amazonaws.services.lambda.runtime.LambdaLogger;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
//import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
//import org.junit.jupiter.api.Test;
//import org.mockito.MockedStatic;
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
//import software.amazon.awssdk.services.dynamodb.model.*;
//
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//class HandlerClientTest {
//
//    /* ===================== TESTE 1 ===================== */
//    /* POST /clientes → 201 */
//
//    @Test
//    void deveCriarClienteComSucesso() {
//
//        DynamoDbClient dynamoMock = mock(DynamoDbClient.class);
//        Context context = mock(Context.class);
//        LambdaLogger logger = mock(LambdaLogger.class);
//
//        when(context.getLogger()).thenReturn(logger);
//
//        // cliente não existe
//        when(dynamoMock.query(any(QueryRequest.class)))
//                .thenReturn(QueryResponse.builder().count(0).build());
//
//        // mock static do create()
//        try (MockedStatic<DynamoDbClient> mocked =
//                     mockStatic(DynamoDbClient.class)) {
//
//            mocked.when(DynamoDbClient::create)
//                    .thenReturn(dynamoMock);
//
//            HandlerClient handler = new HandlerClient();
//
//            APIGatewayV2HTTPEvent request = criarRequest(
//                    "POST",
//                    "/clientes",
//                    """
//                    {
//                      "document": "123",
//                      "name": "Maria",
//                      "email": "maria@email.com"
//                    }
//                    """
//            );
//
//            APIGatewayProxyResponseEvent response =
//                    handler.handleRequest(request, context);
//
//            assertEquals(201, response.getStatusCode());
//            assertTrue(response.getBody()
//                    .contains("Cliente criado com sucesso"));
//
//            verify(dynamoMock, times(1))
//                    .putItem(any(PutItemRequest.class));
//        }
//    }
//
//    /* ===================== TESTE 2 ===================== */
//    /* GET /clientes/{document} → 404 */
//
//    @Test
//    void deveRetornar404QuandoClienteNaoExiste() {
//
//        DynamoDbClient dynamoMock = mock(DynamoDbClient.class);
//        Context context = mock(Context.class);
//        LambdaLogger logger = mock(LambdaLogger.class);
//
//        when(context.getLogger()).thenReturn(logger);
//
//        when(dynamoMock.query(any(QueryRequest.class)))
//                .thenReturn(QueryResponse.builder().count(0).build());
//
//        try (MockedStatic<DynamoDbClient> mocked =
//                     mockStatic(DynamoDbClient.class)) {
//
//            mocked.when(DynamoDbClient::create)
//                    .thenReturn(dynamoMock);
//
//            HandlerClient handler = new HandlerClient();
//
//            APIGatewayV2HTTPEvent request =
//                    criarRequest("GET", "/clientes/999", null);
//
//            APIGatewayProxyResponseEvent response =
//                    handler.handleRequest(request, context);
//
//            assertEquals(404, response.getStatusCode());
//            assertTrue(response.getBody()
//                    .contains("Cliente não encontrado"));
//        }
//    }
//
//    /* ===================== UTIL ===================== */
//
//    private APIGatewayV2HTTPEvent criarRequest(
//            String method,
//            String path,
//            String body) {
//
//        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
//        event.setRawPath(path);
//        event.setBody(body);
//
//        APIGatewayV2HTTPEvent.RequestContext ctx =
//                new APIGatewayV2HTTPEvent.RequestContext();
//
//        APIGatewayV2HTTPEvent.Http http =
//                new APIGatewayV2HTTPEvent.Http();
//
//        http.setMethod(method);
//        ctx.setHttp(http);
//
//        event.setRequestContext(ctx);
//        return event;
//    }
//}
