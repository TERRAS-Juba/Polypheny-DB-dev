/*
 * Copyright 2019-2022 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.http;


import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonSyntaxException;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.*;
import io.javalin.plugin.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jetbrains.annotations.NotNull;
import org.polypheny.db.StatusService;
import org.polypheny.db.catalog.Catalog;
import org.polypheny.db.catalog.Catalog.QueryLanguage;
import org.polypheny.db.iface.AuthenticationException;
import org.polypheny.db.iface.Authenticator;
import org.polypheny.db.iface.QueryInterface;
import org.polypheny.db.information.InformationGroup;
import org.polypheny.db.information.InformationManager;
import org.polypheny.db.information.InformationPage;
import org.polypheny.db.information.InformationTable;
import org.polypheny.db.transaction.TransactionManager;
import org.polypheny.db.util.Util;
import org.polypheny.db.webui.HttpServer;
import org.polypheny.db.webui.TemporalFileManager;
import org.polypheny.db.webui.crud.LanguageCrud;
import org.polypheny.db.webui.models.Result;
import org.polypheny.db.webui.models.requests.QueryRequest;
import org.polypheny.security.admin.CrudManager;
import org.polypheny.security.admin.dto.AddRoleRequest;
import org.polypheny.security.admin.dto.AddUserRequest;
import org.polypheny.security.authentication.AuthenticatorDb;
import org.polypheny.security.authentication.dto.AuthenticationRequest;
import org.polypheny.security.authentication.dto.AuthenticationResponse;
import org.polypheny.security.authentication.model.User;
import org.polypheny.security.policyengine.PolicyEngine;
import org.polypheny.security.tokenmanagement.JwtManager;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.post;

@Slf4j
public class HttpInterface extends QueryInterface {
    @SuppressWarnings("WeakerAccess")
    public static final String INTERFACE_NAME = "HTTP Interface";
    @SuppressWarnings("WeakerAccess")
    public static final String INTERFACE_DESCRIPTION = "HTTP-based query interface, which supports all available languages via specific routes.";
    @SuppressWarnings("WeakerAccess")
    public static final List<QueryInterfaceSetting> AVAILABLE_SETTINGS = ImmutableList.of(new QueryInterfaceSettingInteger("port", false, true, false, 13137), new QueryInterfaceSettingInteger("maxUploadSizeMb", false, true, true, 10000));

    private Set<String> xIds = new HashSet<>();

    private final int port;
    private final String uniqueName;

    // Counters
    private final Map<QueryLanguage, AtomicLong> statementCounters = new HashMap<>();

    private final MonitoringPage monitoringPage;

    private Javalin server;

    // Add support for authentication
    AuthenticatorDb authenticator = new AuthenticatorDb();

    // Add support for jwt authentication
    JwtManager manager = new JwtManager();

    //Add support for admin management
    CrudManager crudManager = new CrudManager();

    // Add support for control access policy
    PolicyEngine engine = PolicyEngine.getInstance();

    public HttpInterface(TransactionManager transactionManager, Authenticator authenticator, int ifaceId, String uniqueName, Map<String, String> settings) {
        super(transactionManager, authenticator, ifaceId, uniqueName, settings, true, false);
        this.uniqueName = uniqueName;
        this.port = Integer.parseInt(settings.get("port"));
        if (!Util.checkIfPortIsAvailable(port)) {
            // Port is already in use
            throw new RuntimeException("Unable to start " + INTERFACE_NAME + " on port " + port + "! The port is already in use.");
        }
        // Add information page
        monitoringPage = new MonitoringPage();
    }

    @Override
    public void run() {
        JsonMapper gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Class<T> targetType) {
                return HttpServer.gson.fromJson(json, targetType);
            }


            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj) {
                return HttpServer.gson.toJson(obj);
            }

        };
        //***************************************
        // Add support for SSL encryption
        //***************************************
        server = Javalin.create(config -> {
            config.server(() -> {
                // Access keystore for self signed certificat
                Path keystorePath = Paths.get(System.getenv("KEYSTORE_PATH")).toAbsolutePath();
                if (!Files.exists(keystorePath)) try {
                    throw new FileNotFoundException(keystorePath.toString());
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                //***************************************
                // Add support for SSL encryption
                //***************************************
                // HTTP Configuration
                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.setSecureScheme("https");
                httpConfig.setSendServerVersion(true);
                httpConfig.setSendDateHeader(true);
                HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
                httpsConfig.addCustomizer(new SecureRequestCustomizer());
                // SSL factory
                SslContextFactory sslContextFactory = new SslContextFactory.Server();
                sslContextFactory.setKeyStorePath(keystorePath.toString());
                sslContextFactory.setKeyStorePassword(System.getenv("KEYSTORE_KEY"));
                sslContextFactory.setTrustStorePath(keystorePath.toString());
                sslContextFactory.setTrustStorePassword(System.getenv("KEYSTORE_KEY"));
                // Create ssl server
                Server sslServer = new Server();
                // Add connectors
                ServerConnector sslConnector = new ServerConnector(sslServer, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
                sslConnector.setPort(443);
                sslServer.setConnectors(new Connector[]{sslConnector});
                return sslServer;
            });
            config.jsonMapper(gsonMapper);
            config.enableCorsForAllOrigins();
        }).start();
        server.exception(Exception.class, (e, ctx) -> {
            log.warn("Caught exception in the HTTP interface", e);
            if (e instanceof JsonSyntaxException) {
                ctx.result("Malformed request: " + e.getCause().getMessage());
            } else {
                ctx.result("Error: " + e.getMessage());
            }
        });
        //***************************************
        // HTTP server endpoints
        //***************************************
        server.routes(() -> {
            // Authentication
            post("/auth/user/login", login);
            post("/auth/admin/login", loginAdmin);
            // Query operations
            post("/query/mongo", ctx -> anyQuery(QueryLanguage.MONGO_QL, ctx));
            post("/query/mql", ctx -> anyQuery(QueryLanguage.MONGO_QL, ctx));
            post("/query/sql", ctx -> {
                validatePolySqlQuery(ctx);
                anyQuery(QueryLanguage.SQL, ctx);
            });
            post("/query/piglet", ctx -> anyQuery(QueryLanguage.PIG, ctx));
            post("/query/pig", ctx -> anyQuery(QueryLanguage.PIG, ctx));
            post("/query/cql", ctx -> anyQuery(QueryLanguage.CQL, ctx));
            post("/query/cypher", ctx -> anyQuery(QueryLanguage.CYPHER, ctx));
            post("/query/opencypher", ctx -> anyQuery(QueryLanguage.CYPHER, ctx));
            // admin operations
            post("/dashboard/add_user", addUser);
            delete("/dashboard/delete_user/{username}", deleteUser);
            post("/dashboard/add_role", addRole);
            delete("/dashboard/delete_role/{name}", deleteRole);
            StatusService.printInfo(String.format("%s started and is listening on port %d.", INTERFACE_NAME, port));
        });
        // Configure response to be sent in json format
        server.before(("*"), ctx -> {
            ctx.res.setHeader("Content-Type", "application/json");
        });
        // Validate user tokens
        server.before("/query/*", validateUserToken);
        server.before("/dashboard/*", validateAdminToken);
    }

    //***************************************
    // User login
    //***************************************
    private final Handler login = ctx -> {
        User user = getUser(ctx);
        String token = manager.generateToken(user);
        ctx.json(new AuthenticationResponse(token, "Bearer", Date.from(Instant.now()).toString()));
    };
    private final Handler loginAdmin = ctx -> {
        User user = getUser(ctx);
        if (user.getRoles().toString().contains("ADMIN")) {
            String token = manager.generateToken(user);
            ctx.json(new AuthenticationResponse(token, "Bearer", Date.from(Instant.now()).toString()));
        } else {
            throw new NotFoundResponse();
        }
    };
    //***************************************
    // Validate user token
    //***************************************
    private final Handler validateUserToken = ctx -> {
        manager.extractTokenInformations(ctx);
    };
    private final Handler validateAdminToken = (ctx) -> {
        Optional<DecodedJWT> decodedJWT = manager.extractTokenInformations(ctx);
        manager.validateUserRole(ctx, decodedJWT.get(), "ADMIN");
    };
    //***************************************
    // Admin crud operations
    //***************************************
    private final Handler addUser = (ctx -> {
        AddUserRequest request = ctx.bodyAsClass(AddUserRequest.class);
        crudManager.addUser(request);
        ctx.status(201);
    });
    private final Handler deleteUser = (ctx -> {
        String username = ctx.pathParam("username");
        if (username != null && !username.equals("")) {
            crudManager.deleteUser(username);
        } else {
            throw new BadRequestResponse();
        }
        ctx.status(204);
    });
    private final Handler addRole = (ctx -> {
        AddRoleRequest request = ctx.bodyAsClass(AddRoleRequest.class);
        crudManager.addRole(request);
        ctx.status(201);
    });
    private final Handler deleteRole = (ctx -> {
        String name = ctx.pathParam("name");
        if (name != null && !name.equals("")) {
            crudManager.deleteRole(name);
        } else {
            throw new BadRequestResponse();
        }
        ctx.status(204);
    });

    //***************************************
    // Retrieve user from database
    //***************************************
    private User getUser(Context ctx) {
        AuthenticationRequest request = ctx.bodyAsClass(AuthenticationRequest.class);
        try {
            return authenticator.authenticateDomain(request.getUsername(), request.getPassword());
        } catch (AuthenticationException e) {
            throw new NotFoundResponse();
        }
    }

    //***************************************
    // Valide polySql query before executing it
    //***************************************
    private void validatePolySqlQuery(Context ctx) {
        QueryRequest query = ctx.bodyAsClass(QueryRequest.class);
        Optional<DecodedJWT> decodedJWT = manager.extractTokenInformations(ctx);
        String id = String.valueOf(decodedJWT.get().getClaim("id"));
        String roles = String.valueOf(decodedJWT.get().getClaim("roles"));
        roles = roles.replace("\"", "");
        if (roles.contains("ADMIN")) return;
        boolean validate;
        validate = engine.validatePolySqlQuery(query.query, id, roles);
        if (!validate) {
            throw new ForbiddenResponse("Access violation");
        }

    }

    //***************************************
    // Execute sql query on polystore
    //***************************************
    public void anyQuery(QueryLanguage language, final Context ctx) {
        QueryRequest query = ctx.bodyAsClass(QueryRequest.class);

        cleanup();

        List<Result> results = LanguageCrud.anyQuery(language, null, query, transactionManager, Catalog.defaultUserId, Catalog.defaultDatabaseId, null);
        ctx.json(results.toArray(new Result[0]));

        if (!statementCounters.containsKey(language)) {
            statementCounters.put(language, new AtomicLong());
        }
        statementCounters.get(language).incrementAndGet();
        xIds.addAll(results.stream().map(Result::getXid).filter(Objects::nonNull).collect(Collectors.toSet()));
    }


    private void cleanup() {
        // todo change this also in websocket logic, rather hacky
        for (String xId : xIds) {
            InformationManager.close(xId);
            TemporalFileManager.deleteFilesOfTransaction(xId);
        }
    }


    @Override
    public List<QueryInterfaceSetting> getAvailableSettings() {
        return AVAILABLE_SETTINGS;
    }


    @Override
    public void shutdown() {
        server.stop();
        monitoringPage.remove();
    }


    @Override
    public String getInterfaceType() {
        return "Http Interface";
    }


    @Override
    protected void reloadSettings(List<String> updatedSettings) {

    }

    private class MonitoringPage {

        private final InformationPage informationPage;
        private final InformationGroup informationGroupRequests;
        private final InformationTable statementsTable;


        public MonitoringPage() {
            InformationManager im = InformationManager.getInstance();

            informationPage = new InformationPage(uniqueName, INTERFACE_NAME).fullWidth().setLabel("Interfaces");
            informationGroupRequests = new InformationGroup(informationPage, "Requests");

            im.addPage(informationPage);
            im.addGroup(informationGroupRequests);

            statementsTable = new InformationTable(informationGroupRequests, Arrays.asList("Language", "Percent", "Absolute"));
            statementsTable.setOrder(2);
            im.registerInformation(statementsTable);

            informationGroupRequests.setRefreshFunction(this::update);
        }


        public void update() {
            double total = 0;
            for (AtomicLong counter : statementCounters.values()) {
                total += counter.get();
            }

            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
            symbols.setDecimalSeparator('.');
            DecimalFormat df = new DecimalFormat("0.0", symbols);
            statementsTable.reset();
            for (Map.Entry<QueryLanguage, AtomicLong> entry : statementCounters.entrySet()) {
                statementsTable.addRow(entry.getKey().name(), df.format(total == 0 ? 0 : (entry.getValue().longValue() / total) * 100) + " %", entry.getValue().longValue());
            }
        }


        public void remove() {
            InformationManager im = InformationManager.getInstance();
            im.removeInformation(statementsTable);
            im.removeGroup(informationGroupRequests);
            im.removePage(informationPage);
        }

    }

}
