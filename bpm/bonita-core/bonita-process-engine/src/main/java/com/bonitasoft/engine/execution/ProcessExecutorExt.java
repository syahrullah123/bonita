/*******************************************************************************
 * Copyright (C) 2011-2013 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.execution;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.archive.ArchiveService;
import org.bonitasoft.engine.bpm.model.ConnectorDefinitionWithInputValues;
import org.bonitasoft.engine.bpm.model.ConnectorEvent;
import org.bonitasoft.engine.bpm.model.impl.BPMInstancesCreator;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.connector.ConnectorService;
import org.bonitasoft.engine.core.expression.control.api.ExpressionResolverService;
import org.bonitasoft.engine.core.expression.control.model.SExpressionContext;
import org.bonitasoft.engine.core.operation.OperationService;
import org.bonitasoft.engine.core.operation.model.SOperation;
import org.bonitasoft.engine.core.operation.model.builder.SOperationBuilders;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SFlowElementContainerDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.builder.BPMDefinitionBuilders;
import org.bonitasoft.engine.core.process.document.api.ProcessDocumentService;
import org.bonitasoft.engine.core.process.document.model.builder.SProcessDocumentBuilder;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.GatewayInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.TokenService;
import org.bonitasoft.engine.core.process.instance.api.TransitionService;
import org.bonitasoft.engine.core.process.instance.api.event.EventInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceCreationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceModificationException;
import org.bonitasoft.engine.core.process.instance.model.SConnectorInstance;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.data.instance.api.DataInstanceContainer;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SEvent;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.connector.InvalidEvaluationConnectorConditionException;
import org.bonitasoft.engine.execution.ContainerRegistry;
import org.bonitasoft.engine.execution.FlowNodeExecutor;
import org.bonitasoft.engine.execution.ProcessExecutor;
import org.bonitasoft.engine.execution.ProcessExecutorImpl;
import org.bonitasoft.engine.execution.event.EventsHandler;
import org.bonitasoft.engine.execution.handler.SProcessInstanceHandler;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.expression.exception.SExpressionDependencyMissingException;
import org.bonitasoft.engine.expression.exception.SExpressionEvaluationException;
import org.bonitasoft.engine.expression.exception.SExpressionTypeUnknownException;
import org.bonitasoft.engine.expression.exception.SInvalidExpressionException;
import org.bonitasoft.engine.expression.model.SExpression;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilders;
import org.bonitasoft.engine.lock.LockService;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;
import org.bonitasoft.engine.work.WorkService;

import com.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import com.bonitasoft.engine.core.process.instance.model.builder.SProcessInstanceUpdateBuilder;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 * @author Yanyan Liu
 * @author Elias Ricken de Medeiros
 * @author Hongwen Zang
 * @author Celine Souchet
 */
public class ProcessExecutorExt extends ProcessExecutorImpl implements ProcessExecutor {

    protected final BPMInstanceBuilders instanceBuilders;

    public ProcessExecutorExt(final BPMInstanceBuilders instanceBuilders, final ActivityInstanceService activityInstanceService,
            final ProcessInstanceService processInstanceService, final FlowNodeStateManager flowNodeStateManager, final TechnicalLoggerService logger,
            final FlowNodeExecutor flowNodeExecutor, final TransactionExecutor transactionExecutor, final WorkService workService,
            final ProcessDefinitionService processDefinitionService, final GatewayInstanceService gatewayInstanceService,
            final TransitionService transitionService, final EventInstanceService eventInstanceService, final ConnectorService connectorService,
            final ConnectorInstanceService connectorInstanceService, final ClassLoaderService classLoaderService, final OperationService operationService,
            final SExpressionBuilders expressionBuilders, final ExpressionResolverService expressionResolverService, final EventService eventService,
            final Map<String, SProcessInstanceHandler<SEvent>> handlers, final ProcessDocumentService processDocumentService,
            final SProcessDocumentBuilder documentBuilder, final ReadSessionAccessor sessionAccessor, final ContainerRegistry containerRegistry,
            final BPMInstancesCreator bpmInstancesCreator, final ArchiveService archiveService, final LockService lockService, final TokenService tokenService,
            final EventsHandler eventsHandler, final BPMDefinitionBuilders bpmDefinitionBuilders, final SOperationBuilders operationBuilders) {
        super(instanceBuilders, activityInstanceService, processInstanceService, flowNodeStateManager, logger, flowNodeExecutor, transactionExecutor,
                workService, processDefinitionService, gatewayInstanceService, transitionService, eventInstanceService, connectorService,
                connectorInstanceService, classLoaderService, operationService, expressionBuilders, expressionResolverService, eventService, handlers,
                processDocumentService, documentBuilder, sessionAccessor, containerRegistry, bpmInstancesCreator, archiveService, lockService, tokenService,
                eventsHandler, bpmDefinitionBuilders, operationBuilders);

        this.instanceBuilders = instanceBuilders;
    }

    @Override
    protected boolean initialize(final long userId, final SProcessDefinition sDefinition, final SProcessInstance sInstance,
            final SExpressionContext expressionContext, final List<SOperation> operations, final Map<String, Object> context,
            final SFlowElementContainerDefinition processContainer, final List<ConnectorDefinitionWithInputValues> connectors)
            throws SProcessInstanceCreationException {
        try {
            // Create SDataInstances
            bpmInstancesCreator.createDataInstances(sInstance, processContainer.getDataDefinitions(), expressionContext, operations, context);
            try {
                initializeStringIndexes(sInstance, sDefinition);
            } catch (final SBonitaException e) {
                throw new SProcessInstanceCreationException("unable to initialize string index on process instance", e);
            }
            createDocuments(sDefinition, sInstance, userId);
            if (connectors != null) {
                executeConnectors(sDefinition, sInstance, connectors);
            }
            executeOperations(operations, context, sInstance);

            // Create connectors
            bpmInstancesCreator.createConnectorInstances(sInstance, processContainer.getConnectors(), SConnectorInstance.PROCESS_TYPE);

            return executeConnectors(sDefinition, sInstance, ConnectorEvent.ON_ENTER, connectorService);
        } catch (final SProcessInstanceCreationException e) {
            throw e;
        } catch (final SBonitaException e) {
            throw new SProcessInstanceCreationException(e);
        } catch (final BonitaHomeNotSetException e) {
            throw new SProcessInstanceCreationException(e);
        } catch (final IOException e) {
            throw new SProcessInstanceCreationException(e);
        } catch (final InvalidEvaluationConnectorConditionException e) {
            throw new SProcessInstanceCreationException(e);
        }
    }

    private void initializeStringIndexes(final SProcessInstance sInstance, final SProcessDefinition sDefinition) throws SExpressionTypeUnknownException,
            SExpressionEvaluationException, SExpressionDependencyMissingException, SInvalidExpressionException, SProcessInstanceModificationException {
        final SExpressionContext contextDependency = new SExpressionContext(sInstance.getId(), DataInstanceContainer.PROCESS_INSTANCE.name(),
                sDefinition.getId());
        boolean update = false;
        final SProcessInstanceUpdateBuilder updateBuilder = instanceBuilders.getProcessInstanceUpdateBuilder();
        for (int i = 1; i <= 5; i++) {
            final SExpression value = sDefinition.getStringIndexValue(i);
            if (value != null) {
                update = true;
                final Object evaluate = expressionResolverService.evaluate(value, contextDependency);
                switch (i) {
                    case 1:
                        updateBuilder.updateStringIndex1(String.valueOf(evaluate));
                        break;
                    case 2:
                        updateBuilder.updateStringIndex2(String.valueOf(evaluate));
                        break;
                    case 3:
                        updateBuilder.updateStringIndex3(String.valueOf(evaluate));
                        break;
                    case 4:
                        updateBuilder.updateStringIndex4(String.valueOf(evaluate));
                        break;
                    case 5:
                        updateBuilder.updateStringIndex5(String.valueOf(evaluate));
                        break;
                }
            }
        }
        if (update) {
            processInstanceService.updateProcess(sInstance, updateBuilder.done());
        }
    }

}
