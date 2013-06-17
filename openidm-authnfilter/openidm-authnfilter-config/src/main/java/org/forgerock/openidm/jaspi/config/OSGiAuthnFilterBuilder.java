package org.forgerock.openidm.jaspi.config;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.forgerock.jaspi.container.config.Configuration;
import org.forgerock.jaspi.container.config.ConfigurationManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.message.AuthException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures the authentication chains based on configuration obtained through OSGi.
 *
 * Example:
 *
 * <pre>
 *     <code>
 * {
 *     "server-auth-config" : {
 *         "iwa-ad-passthrough" : {
 *             "session-module" : {
 *                 "class-name" : "org.forgerock.jaspi.modules.JwtSessionModule"
 *             },
 *             "auth-modules" : [
 *                 {
 *                     "class-name" : "org.forgerock.openidm.jaspi.modules.IWAModule",
 *                     "some-setting" : "some-value"
 *                 },
 *                 {
 *                     "class-name" : "org.forgerock.openidm.jaspi.modules.ADPassthroughModule",
 *                     "some-setting" : "some-value"
 *                 }
 *             ]
 *         },
 *         "ad-passthrough-only" : {
 *             "auth-modules" : [
 *                 {
 *                     "class-name" : "org.forgerock.openidm.jaspi.modules.ADPassthroughModule",
 *                     "passThroughAuth" : "system/AD/account"
 *                 }
 *             ]
 *         }
 *     }
 * }
 *     </code>
 * </pre>
 *
 */
@Component(name = OSGiAuthnFilterBuilder.PID, immediate = true, policy = ConfigurationPolicy.REQUIRE)
public class OSGiAuthnFilterBuilder {

    public static final String PID = "org.forgerock.openidm.authnfilter";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Configures the commons Authentication Filter with the configuration in the authnfilter.json file.
     *
     * @param context The ComponentContext.
     */
    @Activate
    protected void activate(ComponentContext context) {

        EnhancedConfig config = JSONEnhancedConfig.newInstance();
        JsonValue jsonConfig = config.getConfigurationAsJson(context);

        if (jsonConfig == null ) {
            // No configurations found
            logger.warn("Could not find any configurations for the AuthnFilter, filter will not function");
            return;
        }

        Configuration configuration = new Configuration();
        // For each ServerAuthConfig
        for (String s : jsonConfig.get("server-auth-config").required().keys()) {
            Map<String, Object> contextProperties = jsonConfig.get("server-auth-config").required().get(s).asMap();
            configuration.addAuthContext(s, contextProperties);
        }

        try {
            ConfigurationManager.configure(configuration);
        } catch (AuthException e) {
            logger.error("Failed to configure the commons Authentication Filter");
        }
    }
}