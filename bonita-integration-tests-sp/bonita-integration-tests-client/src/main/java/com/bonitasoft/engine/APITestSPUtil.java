/*******************************************************************************
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.flownode.TaskPriority;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.connectors.TestConnector;
import org.bonitasoft.engine.connectors.TestConnector3;
import org.bonitasoft.engine.connectors.TestConnectorLongToExecute;
import org.bonitasoft.engine.connectors.TestConnectorThatThrowException;
import org.bonitasoft.engine.connectors.TestConnectorWithOutput;
import org.bonitasoft.engine.connectors.TestExternalConnector;
import org.bonitasoft.engine.connectors.VariableStorage;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.io.IOUtil;
import org.bonitasoft.engine.platform.StartNodeException;
import org.bonitasoft.engine.platform.StopNodeException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.PlatformSession;
import org.bonitasoft.engine.test.APITestUtil;
import org.bonitasoft.engine.test.BuildTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bonitasoft.engine.api.APIClient;
import com.bonitasoft.engine.api.ApplicationAPI;
import com.bonitasoft.engine.api.IdentityAPI;
import com.bonitasoft.engine.api.LogAPI;
import com.bonitasoft.engine.api.MonitoringAPI;
import com.bonitasoft.engine.api.PageAPI;
import com.bonitasoft.engine.api.PlatformAPIAccessor;
import com.bonitasoft.engine.api.PlatformMonitoringAPI;
import com.bonitasoft.engine.api.ProcessAPI;
import com.bonitasoft.engine.api.ProfileAPI;
import com.bonitasoft.engine.api.ReportingAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import com.bonitasoft.engine.api.TenantManagementAPI;
import com.bonitasoft.engine.api.ThemeAPI;
import com.bonitasoft.engine.bpm.flownode.ManualTaskCreator;
import com.bonitasoft.engine.connector.APIAccessorConnector;
import com.bonitasoft.engine.log.Log;
import com.bonitasoft.engine.monitoring.MonitoringException;
import com.bonitasoft.engine.reporting.Report;
import com.bonitasoft.engine.reporting.ReportSearchDescriptor;

public class APITestSPUtil extends APITestUtil {

    protected static final Logger LOGGER = LoggerFactory.getLogger(APITestSPUtil.class);

    private ApplicationAPI applicationAPI;

    private PlatformMonitoringAPI platformMonitoringAPI;

    private TenantManagementAPI tenantManagementAPI;

    private PageAPI pageAPI;

    public APISession getSession() {
        return getApiClient().getSession();
    }

    protected APIClient getApiClient() {
        return BPMTestSPUtil.apiClient;
    }

    @Override
    public PlatformLoginAPI getPlatformLoginAPI() throws BonitaException {
        return PlatformAPIAccessor.getPlatformLoginAPI();
    }

    @Override
    public PlatformAPI getPlatformAPI(final PlatformSession session) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return PlatformAPIAccessor.getPlatformAPI(session);
    }

    @Override
    public LoginAPI getLoginAPI() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        return TenantAPIAccessor.getLoginAPI();
    }

    protected PlatformMonitoringAPI getPlatformMonitoringAPI() {
        return platformMonitoringAPI;
    }

    protected MonitoringAPI getMonitoringAPI() {
        return getApiClient().getMonitoringAPI();
    }

    @Override
    public ThemeAPI getThemeAPI() {
        return getApiClient().getThemeAPI();
    }

    @Override
    public ProcessAPI getProcessAPI() {
        return (ProcessAPI) super.getProcessAPI();
    }

    @Override
    public IdentityAPI getIdentityAPI() {
        return (IdentityAPI) super.getIdentityAPI();
    }

    @Override
    public ProfileAPI getProfileAPI() {
        return (ProfileAPI) super.getProfileAPI();
    }

    public ReportingAPI getReportingAPI() {
        return getApiClient().getReportingAPI();
    }

    public LogAPI getLogAPI() {
        return getApiClient().getLogAPI();
    }

    /**
     * @return
     * @deprecated use {@link org.bonitasoft.engine.test.APITestUtil#getTenantAdministrationAPI()}
     */
    @Deprecated
    public TenantManagementAPI getTenantManagementAPI() {
        return tenantManagementAPI;
    }

    public ApplicationAPI getSubscriptionApplicationAPI() {
        return applicationAPI;
    }

    public PageAPI getSubscriptionPageAPI() {
        return pageAPI;
    }

    public void loginOnTenantWith(final String userName, final String password, final long tenantId) throws BonitaException {
        BPMTestSPUtil.loginOnTenant(userName, password, tenantId);
        setOldAPIs();
    }

    @Override
    public void loginOnDefaultTenantWith(final String userName, final String password) throws BonitaException {
        BPMTestSPUtil.loginOnDefaultTenant(userName, password);
        setOldAPIs();
    }

    @Override
    public void loginOnDefaultTenantWithDefaultTechnicalUser() throws BonitaException {
        BPMTestSPUtil.loginOnDefaultTenantWithDefaultTechnicalUser();
        setOldAPIs();
    }

    protected void loginOnTenantWithTechnicalUser(final long tenantId) throws BonitaException {
        BPMTestSPUtil.loginOnTenantWithDefaultTechnicalUser(tenantId);
        setOldAPIs();
    }

    protected void setOldAPIs() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException {
        pageAPI = TenantAPIAccessor.getPageAPI(getSession());
        platformMonitoringAPI = TenantAPIAccessor.getPlatformMonitoringAPI(getSession());
        tenantManagementAPI = TenantAPIAccessor.getTenantManagementAPI(getSession());
        applicationAPI = TenantAPIAccessor.getApplicationAPI(getSession());
    }

    @Override
    public void logoutOnTenant() throws BonitaException {
        getApiClient().logout();
    }

    public void stopPlatform() throws BonitaException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, StopNodeException,
            StartNodeException {
        final PlatformSession loginPlatform = loginOnPlatform();
        final PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(loginPlatform);
        platformAPI.stopNode();
        logoutOnPlatform(loginPlatform);
    }

    public void startPlatform() throws BonitaException, BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, StopNodeException,
            StartNodeException {
        final PlatformSession loginPlatform = loginOnPlatform();
        final PlatformAPI platformAPI = PlatformAPIAccessor.getPlatformAPI(loginPlatform);
        platformAPI.startNode();
        logoutOnPlatform(loginPlatform);
    }

    public long createAndActivateTenant(final String uniqueName) throws BonitaException {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(uniqueName);
        stringBuilder.append("_");
        stringBuilder.append(System.currentTimeMillis());
        final String tenantUniqueName = stringBuilder.toString();

        return BPMTestSPUtil.createAndActivateTenantWithDefaultTechnicalLogger(tenantUniqueName);
    }

    protected boolean containsLogWithActionType(final List<Log> logs, final String actionType, final int minimalFrequency) {
        int count = 0;
        final Iterator<Log> iterator = logs.iterator();
        while (iterator.hasNext() && count < minimalFrequency) {
            final Log log = iterator.next();
            if (actionType.equals(log.getActionType())) {
                count++;
            }
        }

        return count == minimalFrequency;
    }

    protected ManualTaskCreator buildManualTaskCreator(final long parentTaskId, final String taskName, final long assignTo, final String description,
            final Date dueDate, final TaskPriority priority) {
        final ManualTaskCreator taskCreator = new ManualTaskCreator(parentTaskId, taskName);
        taskCreator.setDisplayName(taskName);
        taskCreator.setAssignTo(assignTo);
        taskCreator.setDescription(description);
        taskCreator.setDueDate(dueDate);
        taskCreator.setPriority(priority);
        return taskCreator;
    }

    public List<String> checkNoActiveTransactions() throws MonitoringException {
        final List<String> messages = new ArrayList<String>();
        long numberOfActiveTransactions = getMonitoringAPI().getNumberOfActiveTransactions();
        if (numberOfActiveTransactions != 0) {
            // retry 50 ms after because there might still be some jobs/works that run:
            try {
                LOGGER.warn("There was " + numberOfActiveTransactions + " active transaction, waiting 50ms and checking it again");
                Thread.sleep(50);
            } catch (final InterruptedException e) {
                throw new MonitoringException("interrupted while sleeping");
            }
            numberOfActiveTransactions = getMonitoringAPI().getNumberOfActiveTransactions();
            if (numberOfActiveTransactions != 0) {
                // go back to sleep 15s now:
                LOGGER.warn("There was still " + numberOfActiveTransactions + " active transaction, waiting 15s and checking it again");
                try {
                    Thread.sleep(15000);
                } catch (final InterruptedException e) {
                    throw new MonitoringException("interrupted while sleeping");
                }
                numberOfActiveTransactions = getMonitoringAPI().getNumberOfActiveTransactions();
                if (numberOfActiveTransactions != 0) {
                    messages.add("There are " + numberOfActiveTransactions + " active transactions.");
                }
            }
        }
        return messages;
    }

    public List<String> checkNoReports() throws SearchException {
        final List<String> messages = new ArrayList<String>();
        // only for non-default tenants:
        final SearchOptionsBuilder build = new SearchOptionsBuilder(0, 1000).filter(ReportSearchDescriptor.PROVIDED, false);
        final SearchResult<Report> reportSR = getReportingAPI().searchReports(build.done());
        final List<Report> reports = reportSR.getResult();
        if (reportSR.getCount() > 0) {
            final StringBuilder messageBuilder = new StringBuilder("Some Reports are still present: ");
            for (final Report report : reports) {
                messageBuilder.append(report.getName()).append(", ");
            }
            messages.add(messageBuilder.toString());
        }
        return messages;
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnector3(final ProcessDefinitionBuilder processDefinitionBuilder, final String actorName,
            final User user) throws BonitaException, IOException {
        final List<BarResource> connectorImplementations = Collections.singletonList(BuildTestUtil.getContentAndBuildBarResource("TestConnector3.impl",
                TestConnector3.class));
        final List<BarResource> generateConnectorDependencies = Arrays.asList(
                BuildTestUtil.generateJarAndBuildBarResource(TestConnector3.class, "TestConnector3.jar"),
                BuildTestUtil.generateJarAndBuildBarResource(VariableStorage.class, "VariableStorage.jar"));
        return deployAndEnableProcessWithActorAndConnectorAndParameter(processDefinitionBuilder, actorName, user, connectorImplementations,
                generateConnectorDependencies, null);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorWithCustomType(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user) throws BonitaException, IOException {
        final byte[] byteArray = IOUtil.getAllContentFrom(TestConnector.class
                .getResourceAsStream("/org/bonitasoft/engine/connectors/connector-with-custom-type.bak"));
        final BarResource barResource = new BarResource("connector-with-custom-type.jar", byteArray);

        return deployAndEnableProcessWithActorAndConnectorAndParameter(processDefinitionBuilder, actorName, user,
                Collections.singletonList(BuildTestUtil.getContentAndBuildBarResource("TestConnectorWithCustomType.impl", TestConnector.class)),
                Collections.singletonList(barResource), null);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorLongToExecute(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndConnectorAndParameter(processDefinitionBuilder, actorName, user, null, "TestConnectorLongToExecute.impl",
                TestConnectorLongToExecute.class, "TestConnectorLongToExecute.jar");
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorThatThrowException(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actor, final User user) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndTestConnectorThatThrowExceptionAndParameter(processDefinitionBuilder, actor, user, null);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorThatThrowExceptionAndParameter(
            final ProcessDefinitionBuilder processDefinitionBuilder, final String actorName, final User user, final Map<String, String> parameters)
            throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndConnectorAndParameter(processDefinitionBuilder, actorName, user, parameters,
                "TestConnectorThatThrowException.impl", TestConnectorThatThrowException.class, "TestConnectorThatThrowException.jar");
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnector(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actor, final User user) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndTestConnectorAndParameter(processDefinitionBuilder, actor, user, null);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorAndParameter(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user, final Map<String, String> parameters) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndTestConnectorAndParameter(processDefinitionBuilder, actorName, user, parameters,
                "TestConnector.impl", "TestConnector.jar");
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnector2(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actor, final User user) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndTestConnectorAndParameter(processDefinitionBuilder, actor, user, null);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorAndParameter2(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user, final Map<String, String> parameters) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndTestConnectorAndParameter(processDefinitionBuilder, actorName, user, parameters, "TestConnector2.impl",
                "TestConnector2.jar");
    }

    private ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorAndParameter(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user, final Map<String, String> parameters, final String name, final String jarName)
            throws IOException, BonitaException {
        final List<BarResource> connectorImplementations = Collections.singletonList(BuildTestUtil.getContentAndBuildBarResource(name, TestConnector.class));
        final List<BarResource> generateConnectorDependencies = Arrays.asList(BuildTestUtil.generateJarAndBuildBarResource(TestConnector.class, jarName),
                BuildTestUtil.generateJarAndBuildBarResource(VariableStorage.class, "VariableStorage.jar"));
        return deployAndEnableProcessWithActorAndConnectorAndParameter(processDefinitionBuilder, actorName, user, connectorImplementations,
                generateConnectorDependencies, parameters);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorWithOutput(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actor, final User user) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndTestConnectorWithOutputAndParameter(processDefinitionBuilder, actor, user, null);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndTestConnectorWithOutputAndParameter(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user, final Map<String, String> parameters) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndConnectorAndParameter(processDefinitionBuilder, actorName, user, parameters, "TestConnectorWithOutput.impl",
                TestConnectorWithOutput.class, "TestConnectorWithOutput.jar");
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndAPIAccessorConnector(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actor, final User user) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndAPIAccessorConnectorAndParameter(processDefinitionBuilder, actor, user, null);
    }

    public ProcessDefinition deployAndEnableProcessWithActorAndAPIAccessorConnectorAndParameter(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user, final Map<String, String> parameters) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndConnectorAndParameter(processDefinitionBuilder, actorName, user, parameters, "APIAccessorConnector.impl",
                APIAccessorConnector.class, "APIAccessorConnector.jar");
    }

    public ProcessDefinition deployAndEnableProcessWithExternalTestConnectorAndActor(final ProcessDefinitionBuilder processDefinitionBuilder,
            final String actorName, final User user) throws BonitaException, IOException {
        return deployAndEnableProcessWithActorAndConnector(processDefinitionBuilder, actorName, user, "TestExternalConnector.impl",
                TestExternalConnector.class,
                "TestExternalConnector.jar");
    }
}