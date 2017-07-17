/**
 * Copyright (C) 2012 White Source Ltd.
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
package org.whitesource.agent.client;

import org.whitesource.agent.api.dispatch.*;
import org.whitesource.agent.api.model.AgentProjectInfo;

import java.util.Collection;

/**
 * A facade to the communication layer with the White Source service.
 *
 * @author Edo.Shor
 */
public class WhitesourceService {

    /* --- Members --- */

    private WssServiceClient client;

    private RequestFactory requestFactory;

    /* --- Constructors --- */

    public WhitesourceService() {
        this("generic", "1.0", "1.0");
    }

    public WhitesourceService(final String agent, final String agentVersion, String pluginVersion) {
        this(agent, agentVersion, pluginVersion, null);
    }

    public WhitesourceService(final String agent, final String agentVersion, String pluginVersion, final String serviceUrl) {
        this(agent, agentVersion, pluginVersion, serviceUrl, true);
    }

    public WhitesourceService(final String agent, final String agentVersion, String pluginVersion, final String serviceUrl, boolean setProxy) {
        this(agent, agentVersion, pluginVersion, serviceUrl, setProxy, ClientConstants.DEFAULT_CONNECTION_TIMEOUT_MINUTES);
    }

    public WhitesourceService(final String agent, final String agentVersion, String pluginVersion, final String serviceUrl, boolean setProxy,
                              int connectionTimeoutMinutes) {
        requestFactory = new RequestFactory(agent, agentVersion, pluginVersion);

        String url = serviceUrl;
        if (url == null || url.trim().length() == 0) {
            url = System.getProperty(ClientConstants.SERVICE_URL_KEYWORD, ClientConstants.DEFAULT_SERVICE_URL);
        }

        if (connectionTimeoutMinutes <= 0) {
            connectionTimeoutMinutes = Integer.parseInt(System.getProperty(ClientConstants.CONNECTION_TIMEOUT_KEYWORD,
                    String.valueOf(ClientConstants.DEFAULT_CONNECTION_TIMEOUT_MINUTES)));
        }
        client = new WssServiceClientImpl(url, setProxy, connectionTimeoutMinutes);
    }

    // backward compatibility methods (plugin version is not a parameter)

    public WhitesourceService(final String agent, final String agentVersion, final String serviceUrl, boolean setProxy) {
        this(agent, agentVersion, serviceUrl, setProxy, ClientConstants.DEFAULT_CONNECTION_TIMEOUT_MINUTES);
    }

    public WhitesourceService(final String agent, final String agentVersion, final String serviceUrl, boolean setProxy,
                              int connectionTimeoutMinutes){
        this(agent, agentVersion, null, serviceUrl, setProxy, connectionTimeoutMinutes);
    }

    /* --- Public methods --- */

    /**
     * Updates the White Source organization account with the given OSS information.
     *
     * @param orgToken       Organization token uniquely identifying the account at white source.
     * @param product        The product name or token to update.
     * @param productVersion The product version.
     * @param projectInfos   OSS usage information to send to white source.
     * @return Result of updating white source.
     * @throws WssServiceException In case of errors while updating white source.
     */
    public UpdateInventoryResult update(String orgToken,
                                        String product,
                                        String productVersion,
                                        Collection<AgentProjectInfo> projectInfos)
            throws WssServiceException {
        return update(orgToken, null, product, productVersion, projectInfos);
    }

    /**
     * Updates the White Source organization account with the given OSS information.
     *
     * @param orgToken          Organization token uniquely identifying the account at white source.
     * @param requesterEmail    Email of the WhiteSource user that requests to update WhiteSource.
     * @param product           The product name or token to update.
     * @param productVersion    The product version.
     * @param projectInfos      OSS usage information to send to white source.
     * @return Result of updating white source.
     * @throws WssServiceException
     */
    public UpdateInventoryResult update(String orgToken,
                                        String requesterEmail,
                                        String product,
                                        String productVersion,
                                        Collection<AgentProjectInfo> projectInfos)
            throws WssServiceException {
        return client.updateInventory(
                requestFactory.newUpdateInventoryRequest(orgToken, requesterEmail, product, productVersion, projectInfos));
    }

    /**
     * Generates a file with json representation of the update request.
     *
     * @param orgToken          Organization token uniquely identifying the account at white source.
     * @param product           The product name or token to update.
     * @param productVersion    The product version.
     * @param projectInfos      OSS usage information to send to white source.
     * @return UpdateInventoryRequest.
     */
    public UpdateInventoryRequest offlineUpdate(String orgToken,
                              String product,
                              String productVersion,
                              Collection<AgentProjectInfo> projectInfos) {
        return requestFactory.newUpdateInventoryRequest(orgToken, product, productVersion, projectInfos);
    }

    /**
     * Checks the policies application of the given OSS information.
     *
     * @param orgToken          Organization token uniquely identifying the account at white source.
     * @param product           The product name or token to update.
     * @param productVersion    The product version.
     * @param projectInfos      OSS usage information to send to white source.
     * @return Potential result of applying the currently defined policies.
     * @throws WssServiceException In case of errors while checking the policies with white source.
     * @deprecated Use {@link WhitesourceService#checkPolicyCompliance(String, String, String, Collection, boolean)}.
     */
    public CheckPoliciesResult checkPolicies(String orgToken,
                                             String product,
                                             String productVersion,
                                             Collection<AgentProjectInfo> projectInfos)
            throws WssServiceException {
        return client.checkPolicies(
                requestFactory.newCheckPoliciesRequest(orgToken, product, productVersion, projectInfos));
    }

    /**
     * Checks the policies application of the given OSS information.
     *
     * @param orgToken          Organization token uniquely identifying the account at white source.
     * @param product           The product name or token to update.
     * @param productVersion    The product version.
     * @param projectInfos      OSS usage information to send to white source.
     * @param forceCheckAllDependencies  Boolean to check new data only or not.
     * @return Potential result of applying the currently defined policies.
     * @throws WssServiceException In case of errors while checking the policies with white source.
     */
    public CheckPolicyComplianceResult checkPolicyCompliance(String orgToken,
                                             String product,
                                             String productVersion,
                                             Collection<AgentProjectInfo> projectInfos,
                                             boolean forceCheckAllDependencies)
            throws WssServiceException {
        return client.checkPolicyCompliance(
                requestFactory.newCheckPolicyComplianceRequest(orgToken, product, productVersion, projectInfos, forceCheckAllDependencies));
    }

    /**
     * Gets additional data for given dependencies.
     *
     * @param orgToken          Organization token uniquely identifying the account at white source.
     * @param product           The product name or token to update.
     * @param productVersion    The product version.
     * @param projectInfos      OSS usage information to send to white source.
     * @return Potential result of the dependencies additional data(license, description, homePageUrl, vulnerabilities, sha1 and displayName).
     * @throws WssServiceException In case of errors while getting additional dependency data with white source.
     */
    public GetDependencyDataResult getDependencyData(String orgToken,
                                                     String product,
                                                     String productVersion,
                                                     Collection<AgentProjectInfo> projectInfos)
            throws WssServiceException {
        return client.getDependencyData(
                requestFactory.newDependencyDataRequest(orgToken, product, productVersion, projectInfos));
    }

    /**
     * The method close the underlying client to the White Source service.
     *
     * @see org.whitesource.agent.client.WssServiceClient#shutdown()
     */
    public void shutdown() {
        client.shutdown();
    }

    /* --- Getters / Setters --- */

    public WssServiceClient getClient() {
        return client;
    }

    public void setClient(WssServiceClient client) {
        this.client = client;
    }

    public RequestFactory getRequestFactory() {
        return requestFactory;
    }

    public void setRequestFactory(RequestFactory requestFactory) {
        this.requestFactory = requestFactory;
    }

}
