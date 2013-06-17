package org.forgerock.openidm.filter;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openidm.jaspi.modules.AuthData;

public interface AuthFilterService {

    /**
     * Method for re-authenticating requests.  The request's security context
     *  will need to include a "X-OpenIDM-Reauth-Password" header.
     *  to succeed
     * 
     * @param request   The request object.
     * @return          The AuthData response object.
     * @throws AuthException
     */
    public AuthData reauthenticate(JsonValue request) throws AuthException;
}
