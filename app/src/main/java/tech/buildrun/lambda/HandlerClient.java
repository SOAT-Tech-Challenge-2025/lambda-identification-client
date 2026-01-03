package tech.buildrun.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.Map;

public class HandlerClient implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String DB_URL      = System.getenv("DB_URL");
    private static final String DB_USER     = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent request,
            Context context) {

        try {
            String method = request.getHttpMethod();
            String path   = request.getPath();

            context.getLogger().log("METHOD=" + method);
            context.getLogger().log("PATH=" + path);

            Class.forName("org.postgresql.Driver");

            try (Connection conn =
                         DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

                // POST /clientes
                if ("POST".equals(method) && "/clientes".equals(path)) {
                    return criarCliente(request, conn);
                }

                // GET /clientes/{document}
                if ("GET".equals(method) && path.startsWith("/clientes/")) {
                    return consultarCliente(path, conn);
                }

                return response(404, Map.of("message", "Endpoint não encontrado"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return response(500, Map.of("message", "Erro interno"));
        }
    }

    // ===================== CRIAR CLIENTE =====================

    private APIGatewayProxyResponseEvent criarCliente(
            APIGatewayProxyRequestEvent request,
            Connection conn) throws Exception {

        if (request.getBody() == null || request.getBody().isBlank()) {
            return response(400, Map.of("message", "Body obrigatório"));
        }

        Map<String, String> body = mapper.readValue(request.getBody(), Map.class);

        String document = body.get("document");
        String name     = body.get("name");
        String email    = body.get("email");

        if (document == null || name == null || email == null) {
            return response(400, Map.of(
                    "message", "document, name e email são obrigatórios"
            ));
        }

        String sql = """
                INSERT INTO tb_cliente (nr_documento, nm_cliente, ds_email)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, document);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.executeUpdate();

            return response(201, Map.of(
                    "message", "Cliente criado com sucesso",
                    "document", document
            ));

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                return response(409, Map.of("message", "Cliente já existe"));
            }
            throw e;
        }
    }

    // ===================== CONSULTAR CLIENTE =====================

    private APIGatewayProxyResponseEvent consultarCliente(
            String path,
            Connection conn) throws Exception {

        // /clientes/123456
        String document = path.substring("/clientes/".length());

        String sql = """
                SELECT id, nr_documento, nm_cliente, ds_email
                FROM tb_cliente
                WHERE nr_documento = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, document);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return response(200, Map.of(
                        "id", rs.getLong("id"),
                        "document", rs.getString("nr_documento"),
                        "name", rs.getString("nm_cliente"),
                        "email", rs.getString("ds_email")
                ));
            }

            return response(404, Map.of("message", "Cliente não encontrado"));
        }
    }

    // ===================== RESPONSE =====================

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