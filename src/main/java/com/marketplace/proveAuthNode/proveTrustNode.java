/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2023 ForgeRock AS.
 */


package com.marketplace.proveAuthNode;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.http.header.GenericHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.thread.listener.ShutdownManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.URI;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.UUID;

/**
 * A node that checks to see if zero-page login headers have specified username and whether that username is in a group
 * permitted to use zero-page login headers.
 */
@Node.Metadata(outcomeProvider  = proveTrustNode.OutcomeProvider.class,
               configClass      = proveTrustNode.Config.class)
public class proveTrustNode extends AbstractDecisionNode {

    private final Logger logger = LoggerFactory.getLogger(proveTrustNode.class);
    private final Config config;
    private final Realm realm;
    private String username = null;
    private final HttpClientHandler handler;
    private String loggerPrefix = "[Prove]";


    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The header name for zero-page login that will contain the identity's username.
         */
        @Attribute(order = 100)
        default String url() {
            return "";
        }
        @Attribute(order = 200)
        default String tokenUrl() {
            return "";
        }

        @Attribute(order = 300)
        default String apiClientId() {
            return "";
        }

        @Attribute(order = 325)
        default String proveUsername() {
            return "";
        }
        @Attribute(order = 350)
        default String provePassword() {
            return "";
        }

        @Attribute(order = 400)
        default int trustScore() {
            return 100;
        }

        @Attribute(order = 500)
        default String identifierSharedState() {
            return "userIdentifier";
        }

    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm The realm the node is in.
     */
    @Inject
    public proveTrustNode(@Assisted Config config, @Assisted Realm realm, ShutdownManager shutdownManager) throws HttpApplicationException {
        this.config = config;
        this.realm = realm;
        this.handler = new HttpClientHandler();
        shutdownManager.addShutdownListener(() -> {
          try {
            handler.close();
          } catch (Exception e) {
            logger.error("Could not close HTTP client", e);
          }
        });
    }

    @Override
    public Action process(TreeContext context) {
        try {

            // Define the URL and data
            String urlProve = config.tokenUrl();
            String formData = "username="+config.proveUsername() +"&password="+config.provePassword()+"&grant_type=password";

            // Create the HTTP client
            HttpClient clientProve = HttpClient.newHttpClient();

            // Create the HTTP request
            HttpRequest requestProve = HttpRequest.newBuilder()
                    .uri(URI.create(urlProve))
                    .header("accept", "application/json")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            // Send the request and get the response
            HttpResponse<String> responseProve = clientProve.send(requestProve, HttpResponse.BodyHandlers.ofString());

            String access_token = null;

            if (responseProve.statusCode() == 200) {
                // Parse the response body as JSON
                JSONObject jsonResponseProve = new JSONObject(responseProve.body());
                
                // Extract the token_type
                access_token = jsonResponseProve.getString("access_token");

                // Print the token_type
            } else {
                logger.error("Failed to get a valid response. Status Code: " + responseProve.statusCode());
            }

            NodeState ns = context.getStateFor(this);
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build();
            HttpRequest.Builder requestBuilder;
            String userIdentifier = context.sharedState.get(config.identifierSharedState()).asString();
            String uuid = UUID.randomUUID().toString();
            requestBuilder = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString("{ \"requestId\": \""+uuid+"\", \"phoneNumber\": \""+userIdentifier+"\"}"));
            
            requestBuilder.header("accept", "application/json");
            requestBuilder.header("api-client-id", config.apiClientId());
            requestBuilder.header("content-type", "application/json");
            requestBuilder.header("Authorization", "Bearer " + access_token);


            String url = config.url() + "trust/v2";
            HttpRequest request = requestBuilder.uri(URI.create(url)).timeout(Duration.ofSeconds(60)).build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            context.getStateFor(this).putTransient("responseBodyProve", response.body());
            JSONObject jo = new JSONObject(response.body());
            JSONObject resp = jo.getJSONObject("response");
            int trustScore = resp.getInt("trustScore");
            if(trustScore > config.trustScore()) {
              return Action.goTo("True").build();
            }

            return Action.goTo("False").build();
            
        } catch(Exception ex) { 
            String stackTrace = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(ex);
            logger.error(loggerPrefix + "Exception occurred: " + stackTrace);
            context.getStateFor(this).putTransient(loggerPrefix + "Exception", new Date() + ": " + ex.getMessage());
            context.getStateFor(this).putTransient(loggerPrefix + "StackTrace", new Date() + ": " + stackTrace);
            return Action.goTo("Error").build();

        }
        
    }

    public static final class OutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        /**
         * Outcomes Ids for this node.
         */
        static final String SUCCESS_OUTCOME = "True";
        static final String ERROR_OUTCOME = "Error";
        static final String FAILURE_OUTCOME = "False";
        private static final String BUNDLE = proveTrustNode.class.getName();

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {

            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, OutcomeProvider.class.getClassLoader());

            List<Outcome> results = new ArrayList<>(
                    Arrays.asList(
                            new Outcome(SUCCESS_OUTCOME, SUCCESS_OUTCOME)
                    )
            );
            results.add(new Outcome(FAILURE_OUTCOME, FAILURE_OUTCOME));
            results.add(new Outcome(ERROR_OUTCOME, ERROR_OUTCOME));

            return Collections.unmodifiableList(results);
        }
    }


}
