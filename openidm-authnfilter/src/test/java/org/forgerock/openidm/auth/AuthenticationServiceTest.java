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
 * Copyright 2016 ForgeRock AS
 */
package org.forgerock.openidm.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.Requests.newReadRequest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.openidm.idp.impl.IdentityProviderService;
import org.forgerock.openidm.idp.impl.ProviderConfigMapper;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AuthenticationServiceTest {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final String OPENID_CONNECT = "OPENID_CONNECT";
    private static final String OAUTH = "OAUTH";
    private static final String AUTHENTICATION_PATH = "authentication";
    private static final String CONF_AUTHENTICATION_MODULE = "/config/authenticationModule.json";

    private JsonValue amendedAuthentication;
    private JsonValue googleIdentityProvider;
    private JsonValue authenticationJson;
    private JsonValue authenticationModule;

    @BeforeMethod
    public void setUp() throws Exception {
        // amendedAuthentication.json is what the configuration should look after injection
        amendedAuthentication = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/amendedAuthentication.json"), Map.class));
        // identityProvider-google.json is a sample identityProvider configuration
        googleIdentityProvider = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/identityProvider-google.json"), Map.class));
        // authentication.json is what a sample authentication.json file will look like on the filesystem
        // Note: The authentication.json file here has been modified to include only the minimum config needed to test
        // the functionality of AuthenticationService.java#amendAuthConfig()
        authenticationJson = json(
                OBJECT_MAPPER.readValue(getClass().getResource("/config/authentication.json"), Map.class));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        amendedAuthentication = null;
        googleIdentityProvider = null;
        authenticationJson = null;
    }

    @Test
    public void testAmendAuthConfig() throws Exception {
        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Add the google provider to the list of provider configs
        final List<ProviderConfig> openIdProviderConfigs = new ArrayList<>();
        openIdProviderConfigs.add(ProviderConfigMapper.toProviderConfig(googleIdentityProvider));

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(openIdProviderConfigs);
        when(identityProviderService.getIdentityProviderByType(OPENID_CONNECT)).thenReturn(openIdProviderConfigs);

        // Instantiate the object to be used with proper mocked IdentityProviderService
        AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.bindIdentityProviderService(identityProviderService);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService
        authenticationService.amendAuthConfig(authenticationJson.get("serverAuthContext").get("authModules"));

        // Assert that the authenticationJson in memory has been modified to have the resolver that is shown in
        // the amendedAuthentication configuration
        assertThat(
                amendedAuthentication.get("serverAuthContext").get("authModules").get(0)
                .isEqualTo(authenticationJson.get("serverAuthContext").get("authModules").get(0)))
                .isTrue();

    }

    @Test
    public void testAmendAuthConfigWithTwoAuthTypes() throws Exception {

        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Add the google provider to the list of provider configs
        final List<ProviderConfig> openIdProviderConfigs = new ArrayList<>();
        openIdProviderConfigs.add(ProviderConfigMapper.toProviderConfig(googleIdentityProvider));


        // Add Facebook provider of type OAuth 2
        final List<ProviderConfig> oAuthProviderConfigs = new ArrayList<>();
        oAuthProviderConfigs.add(ProviderConfigMapper.toProviderConfig(
                        json(OBJECT_MAPPER.readValue(getClass()
                                .getResource("/config/identityProvider-facebook.json"), Map.class))));

        // Whenever we call getIdentityByType("OPENID_CONNECT") return the test case configs for openid_connect
        when(identityProviderService.getIdentityProviderByType(OPENID_CONNECT)).thenReturn(openIdProviderConfigs);
        // Whenever we call getIdentityByType("OAUTH") return the test case configs for openid_connect
        when(identityProviderService.getIdentityProviderByType(OAUTH)).thenReturn(oAuthProviderConfigs);

        final List<ProviderConfig> allConfigs = new ArrayList<>();
        allConfigs.addAll(oAuthProviderConfigs);
        allConfigs.addAll(openIdProviderConfigs);

        when(identityProviderService.getIdentityProviders()).thenReturn(allConfigs);

        // Instantiate the object to be used with proper mocked IdentityProviderService
        AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.bindIdentityProviderService(identityProviderService);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService
        authenticationService.amendAuthConfig(authenticationJson.get("serverAuthContext").get("authModules"));

        // Assert that the authenticationJson in memory has been modified to have the resolver that is shown in
        // the amendedAuthentication configuration
        assertThat(amendedAuthentication.isEqualTo(authenticationJson)).isTrue();
    }

    @Test
    public void testNoProviderConfigsToInject() throws Exception {
        // Copy the authenticationJson for later comparison to prove unmodified
        final JsonValue authenticationJsonNoMod = authenticationJson.copy();

        // Mock of IdentityProviderService
        final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

        // Create an empty providerConfigs list to simulate no identityProviders
        final List<ProviderConfig> providerConfigs = new ArrayList<>();

        // Whenever we call getIdentityProviders() return the test case configs
        when(identityProviderService.getIdentityProviders()).thenReturn(providerConfigs);

        // Instantiate the object to be used with proper mocked IdentityProviderService
        AuthenticationService authenticationService = new AuthenticationService();
        authenticationService.bindIdentityProviderService(identityProviderService);

        // Call the amendAuthConfig to see the configuration of authentication.json be modified with
        // the injected identityProvider config from the IdentityProviderService; in this test case
        // we should see no modifications taking place and the config should not have been modified
        authenticationService.amendAuthConfig(authenticationJson.get("serverAuthContext").get("authModules"));

        // Assert that the authenticationJson has not been modified
        assertThat(authenticationJson.isEqualTo(authenticationJsonNoMod)).isTrue();
    }

    @Test
    public void testReadInstance() throws Exception {
        // set up
        authenticationModule = json(
                OBJECT_MAPPER.readValue(getClass().getResource(CONF_AUTHENTICATION_MODULE), Map.class));

        // Instantiate the object to be used
        AuthenticationService authenticationService = new AuthenticationService();

        // Set the config.
        authenticationService.setConfig(amendedAuthentication);

        // Read request
        final ReadRequest readRequest = newReadRequest(ResourcePath.resourcePath(AUTHENTICATION_PATH));

        // when
        final Promise<ResourceResponse, ResourceException> promise =
                authenticationService.readInstance(new RootContext(), readRequest);
        // then
        final ResourceResponse resourceResponse = promise.get();
        assertThat(resourceResponse.getContent().isEqualTo(authenticationModule)).isTrue();

        authenticationModule = null;
    }
}