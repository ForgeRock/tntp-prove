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

import org.apache.commons.text.RandomStringGenerator;
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


/**
 * A node that checks to see if zero-page login headers have specified username and whether that username is in a group
 * permitted to use zero-page login headers.
 */
@Node.Metadata(outcomeProvider  = AbstractDecisionNode.OutcomeProvider.class,
               configClass      = proveTrustNode.Config.class)
public class proveTrustNode extends AbstractDecisionNode {

    private final Pattern DN_PATTERN = Pattern.compile("^[a-zA-Z0-9]=([^,]+),");
    private final Logger logger = LoggerFactory.getLogger(proveTrustNode.class);
    private final Config config;
    private final Realm realm;
    private String username = null;
    private final HttpClientHandler handler;


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
        default String authToken() {
            return "";
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
          ActionBuilder action;
          RandomStringGenerator generator = new RandomStringGenerator.Builder()
             .withinRange('0', 'z').build();
          String correlationId = generator.generate(20);
          String requestId = generator.generate(20);
          NodeState nodeState = context.getStateFor(this);
          Request request = new Request();
          URI uri = URI.create(config.url());

          // Create the request data body
          JsonValue parameters = new JsonValue(new LinkedHashMap<String, String>(1));
          parameters.put("requestId", requestId);
          parameters.put("consentStatus", "optedOut");
          parameters.put("phoneNumber", nodeState.get("telephoneNumber"));
          parameters.put("details", "true");

          request.setUri(uri);
          request.setMethod("POST");
          request.addHeaders(new GenericHeader("Authorization", "Bearer " + config.authToken()));
          request.addHeaders(new GenericHeader("Accept", "application/json"));
          request.addHeaders(new GenericHeader("Content-Type", "application/json"));
          request.addHeaders(new GenericHeader("x-correlation-id", correlationId));
          
          request.setEntity(parameters);

          
          Response response = handler.handle(new RootContext(), request).getOrThrow();
          JsonValue jsonResponse = new JsonValue(response.getEntity().getJson());
          String status = jsonResponse.get("decision").asString();
            if(status == "0") {
              action = Action.goTo("True");
              return action.build();
            } else {
              action = Action.goTo("False");
              return action.build();
            }
          } catch (Exception e) {
            ActionBuilder action;
            action = Action.goTo("Error");
            return action.build();
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
