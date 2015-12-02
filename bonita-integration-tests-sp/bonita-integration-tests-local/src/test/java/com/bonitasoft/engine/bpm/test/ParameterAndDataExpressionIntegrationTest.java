/*******************************************************************************
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.bpm.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.impl.ProcessAPIImpl;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.CollectionUtil;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.expression.ContainerState;
import org.bonitasoft.engine.expression.exception.SExpressionEvaluationException;
import org.bonitasoft.engine.expression.exception.SInvalidExpressionException;
import org.bonitasoft.engine.expression.model.SExpression;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilder;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilderFactory;
import org.bonitasoft.engine.transaction.STransactionCommitException;
import org.bonitasoft.engine.transaction.STransactionCreationException;
import org.bonitasoft.engine.transaction.STransactionRollbackException;
import org.junit.Ignore;
import org.junit.Test;

import com.bonitasoft.engine.CommonBPMServicesSPTest;
import com.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilderExt;

public class ParameterAndDataExpressionIntegrationTest extends CommonBPMServicesSPTest {

    private static final Map<Integer, Object> EMPTY_RESOLVED_EXPRESSIONS = Collections.emptyMap();

    private final ProcessAPIImpl processAPIImpl = new ProcessAPIImpl();

    private SExpression newExpression(final String content, final String expressionType, final String returnType, final String interpreter,
            final List<SExpression> dependencies) throws SInvalidExpressionException {
        final SExpressionBuilder eb = BuilderFactory.get(SExpressionBuilderFactory.class).createNewInstance();
        eb.setContent(content);
        eb.setExpressionType(expressionType);
        eb.setInterpreter(interpreter);
        eb.setReturnType(returnType);
        eb.setDependencies(dependencies);
        return eb.done();
    }

    private Object createAndEvaluateParameterExpression(final String nameParameter, final Long deployId, final String key) throws Exception {
        final SExpression strExpr = newExpression(nameParameter, SExpression.TYPE_PARAMETER, String.class.getName(), null, null);
        final Map<String, Object> dependencies = CollectionUtil.buildSimpleMap(key, deployId);
        return getTenantAccessor().getExpressionService().evaluate(strExpr, dependencies, EMPTY_RESOLVED_EXPRESSIONS, ContainerState.ACTIVE);
    }

    private ProcessDefinition createProcessAndInsertParameterAndDeployIt(final String processName, final String version, final String parameterName,
            final String parameterValue) throws Exception {
        // create process Definition
        final ProcessDefinitionBuilderExt processBuilder = new ProcessDefinitionBuilderExt().createNewInstance(processName, version);
        // processBuilder.addParameter(parameterName, String.class.getCanonicalName()).addUserTask(taskName, null);
        final Map<String, String> params = new HashMap<String, String>();
        params.put(parameterName, parameterValue);
        final DesignProcessDefinition processDefinition = processBuilder.done();
        // create business archive
        final BusinessArchiveBuilder businessArchive = new BusinessArchiveBuilder().createNewBusinessArchive();
        businessArchive.setParameters(params);
        businessArchive.setProcessDefinition(processDefinition);
        // deploy the archive

        getTransactionService().begin();
        final ProcessDefinition process = processAPIImpl.deploy(businessArchive.done());
        getTransactionService().complete();
        return process;
    }

    @Ignore("Wait until new service configuration")
    @Test
    public void evaluateParameterExpression() throws Exception {
        final String nameParameter = "name";
        final ProcessDefinition deploy = createProcessAndInsertParameterAndDeployIt("firstProcess", "1.0", nameParameter, "baptiste");
        final Long deployId = deploy.getId();
        // create expression
        // check
        assertEquals("baptiste", createAndEvaluateParameterExpression(nameParameter, deployId, "processDefinitionId"));
        deleteProcess(deploy);
    }

    @Test(expected = SExpressionEvaluationException.class)
    public void evaluateExpressionWithAnUnknownParameter() throws Exception {
        final String nameParameter = "name";
        final ProcessDefinition deploy = createProcessAndInsertParameterAndDeployIt("firstProcess", "1.0", nameParameter, "baptiste");
        final Long deployId = deploy.getId();

        try {
            createAndEvaluateParameterExpression("nonExistingParameter", deployId, "processDefinitionId");
        } finally {
            deleteProcess(deploy);
        }
    }

    protected void deleteProcess(ProcessDefinition processDefinition)
            throws STransactionCreationException, DeletionException, STransactionCommitException, STransactionRollbackException {
        getTransactionService().begin();
        processAPIImpl.deleteProcessDefinition(processDefinition.getId());
        getTransactionService().complete();
    }

    @Test
    public void evaluateExpWithParameterAndDataFromDB() throws Exception {
        final String parameterName = "name";
        final String parameterValue = "baptiste";
        final String strDataName = "address";
        final String intDataName = "phone";
        final String strDataValue = "zhangan street 151";
        final String expContent = "'welcome '+name+' to '+address+',Please call '+phone";
        final List<String> variableNames = new ArrayList<String>();
        variableNames.add(parameterName);
        variableNames.add(strDataName);
        variableNames.add(intDataName);
        // create expression
        final SExpression strExpr = newExpression(expContent, SExpression.TYPE_READ_ONLY_SCRIPT, String.class.getName(), SExpression.GROOVY, null);
        final Map<String, Object> dependencies = new HashMap<String, Object>();
        dependencies.put(parameterName, parameterValue);
        dependencies.put(strDataName, strDataValue);
        final int intDataValue = 13812345;
        dependencies.put(intDataName, intDataValue);
        dependencies.put("processDefinitionId", 158l);
        // check
        assertEquals("welcome " + parameterValue + " to " + strDataValue + ",Please call " + intDataValue,
                getTenantAccessor().getExpressionService().evaluate(strExpr, dependencies, EMPTY_RESOLVED_EXPRESSIONS, ContainerState.ACTIVE));
    }

}