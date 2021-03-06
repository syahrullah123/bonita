/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.expression;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.bpm.document.Document;
import org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException;
import org.bonitasoft.engine.core.document.api.DocumentService;
import org.bonitasoft.engine.core.document.model.SMappedDocument;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.SAActivityInstance;
import org.bonitasoft.engine.expression.exception.SExpressionDependencyMissingException;
import org.bonitasoft.engine.expression.model.SExpression;
import org.bonitasoft.engine.service.ModelConvertor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Vincent Elcrin
 */
@RunWith(MockitoJUnitRunner.class)
public class DocumentReferenceExpressionExecutorStrategyTest {

    public static final long PROCESS_INSTANCE_ID = 1L;
    private static final long PARENT_PROCESS_INSTANCE_ID = 2L;
    private static final long A_LONG_TIME_AGO = 1234L;

    @Mock
    private DocumentService documentService;
    @Mock
    private ActivityInstanceService activityInstanceService;
    @InjectMocks
    private DocumentReferenceExpressionExecutorStrategy strategy;
    @Mock
    private SMappedDocument document;
    @Mock
    private SMappedDocument parentDocument;
    @Mock
    private SMappedDocument archivedDocument;
    @Mock
    SExpression expression;
    @Mock
    private SFlowNodeInstance flowNodeInstance;

    @Before
    public void setUp() throws Exception {
        doReturn(flowNodeInstance).when(activityInstanceService).getFlowNodeInstance(PROCESS_INSTANCE_ID);
        doReturn(PARENT_PROCESS_INSTANCE_ID).when(flowNodeInstance).getParentProcessInstanceId();
        doReturn(document).when(documentService).getMappedDocument(eq(PROCESS_INSTANCE_ID), nullable(String.class));
        doReturn(parentDocument).when(documentService).getMappedDocument(eq(PARENT_PROCESS_INSTANCE_ID), nullable(String.class));
        doReturn(archivedDocument).when(documentService).getMappedDocument(eq(PROCESS_INSTANCE_ID), nullable(String.class), eq(A_LONG_TIME_AGO));
    }

    @Test(expected = SExpressionDependencyMissingException.class)
    public void evaluate_should_throw_an_exception_when_container_id_is_null() throws Exception {
        strategy.evaluate(Collections.<SExpression> emptyList(), Collections.<String, Object> emptyMap(), null, ContainerState.ACTIVE);
    }

    @Test(expected = SExpressionDependencyMissingException.class)
    public void evaluate_should_throw_an_exception_when_container_type_is_null() throws Exception {
        strategy.evaluate(Collections.<SExpression> emptyList(), Collections.<String, Object> singletonMap("containerId", PROCESS_INSTANCE_ID), null,
                ContainerState.ACTIVE);
    }

    @Test
    public void evaluate_result_should_contains_process_document_when_container_is_a_process_instance() throws Exception {
        final Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("containerId", PROCESS_INSTANCE_ID);
        dependencies.put("containerType", "PROCESS_INSTANCE");

        final List<Object> result = strategy.evaluate(asList(expression), dependencies, null, ContainerState.ACTIVE);

        assertThat(result).hasSize(1).contains(ModelConvertor.toDocument(document, documentService));
    }

    @Test
    public void evaluate_result_should_contains_parent_process_document_when_container_is_not_a_process_instance() throws Exception {
        final Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("containerId", PROCESS_INSTANCE_ID);
        dependencies.put("containerType", "OTHER");

        final List<Object> result = strategy.evaluate(asList(expression), dependencies, null, ContainerState.ACTIVE);

        assertThat(result).hasSize(1).contains(ModelConvertor.toDocument(parentDocument, documentService));
    }

    @Test
    public void evaluate_result_should_contains_null_when_document_can_not_be_found_for_a_process_instance() throws Exception {
        doThrow(SObjectNotFoundException.class).when(documentService).getMappedDocument(eq(PROCESS_INSTANCE_ID),
                nullable(String.class));
        final Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("containerId", PROCESS_INSTANCE_ID);
        dependencies.put("containerType", "PROCESS_INSTANCE");

        final List<Object> result = strategy.evaluate(asList(expression), dependencies, null, ContainerState.ACTIVE);

        assertThat(result).hasSize(1).contains((Document) null);
    }

    @Test
    public void evaluate_result_should_contains_null_when_document_can_not_be_found_for_a_parent_process_instance() throws Exception {
        doThrow(SObjectNotFoundException.class).when(documentService).getMappedDocument(eq(PARENT_PROCESS_INSTANCE_ID),
                nullable(String.class));
        final Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("containerId", PROCESS_INSTANCE_ID);
        dependencies.put("containerType", "OTHER");

        final List<Object> result = strategy.evaluate(asList(expression), dependencies, null, ContainerState.ACTIVE);

        assertThat(result).hasSize(1).contains((Document) null);
    }

    @Test
    public void evaluate_result_should_contains_archived_document_when_a_time_is_defined() throws Exception {
        final Map<String, Object> dependencies = new HashMap<>();
        dependencies.put("containerId", PROCESS_INSTANCE_ID);
        dependencies.put("containerType", "PROCESS_INSTANCE");
        dependencies.put("time", A_LONG_TIME_AGO);

        final List<Object> result = strategy.evaluate(Collections.singletonList(expression), dependencies, null,
                ContainerState.ACTIVE);

        assertThat(result).hasSize(1).contains(ModelConvertor.toDocument(archivedDocument, documentService));
    }

    @Test
    public void getProcessInstance_should_query_archives_if_time_is_set() throws Exception {
        final long containerId = 123456L;
        final String containerType = "ACTIVITY_INSTANCE";
        final SAActivityInstance activityInstance = mock(SAActivityInstance.class);
        doReturn(activityInstance).when(activityInstanceService).getMostRecentArchivedActivityInstance(containerId);
        final long processInstanceId = 99998888777L;
        doReturn(processInstanceId).when(activityInstance).getParentProcessInstanceId();

        final long retrievedPIId = strategy.getProcessInstance(containerId, containerType, A_LONG_TIME_AGO);

        verify(activityInstanceService).getMostRecentArchivedActivityInstance(containerId);
        assertThat(retrievedPIId).isEqualTo(processInstanceId);

    }
}
