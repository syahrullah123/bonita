/*******************************************************************************
 * Copyright (C) 2009, 2013 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.core.reporting;

import java.sql.SQLException;
import java.util.List;

import org.bonitasoft.engine.commons.TenantLifecycleService;
import org.bonitasoft.engine.commons.exceptions.SObjectAlreadyExistsException;
import org.bonitasoft.engine.commons.exceptions.SObjectModificationException;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;

/**
 * @author Matthieu Chaffotte
 */
public interface ReportingService extends TenantLifecycleService {

    String REPORT = "REPORT";

    String selectList(String selectQuery) throws SQLException;

    SReport addReport(SReport report, byte[] content) throws SReportCreationException, SReportAlreadyExistsException;

    SReport getReport(long reportId) throws SBonitaReadException, SReportNotFoundException;

    /**
     * Get a report from its name.
     * 
     * @param reportName
     *            the name of the report to retrieve.
     * @return the report if found, NULL if not found.
     * @throws SBonitaReadException
     *             if an read error occurs.
     */
    SReport getReportByName(String reportName) throws SBonitaReadException;

    long getNumberOfReports(QueryOptions options) throws SBonitaReadException;

    List<SReport> searchReports(QueryOptions options) throws SBonitaReadException;

    void deleteReport(long reportId) throws SReportDeletionException, SReportNotFoundException;

    byte[] getReportContent(long reportId) throws SBonitaReadException, SReportNotFoundException;

    SReport update(SReport report, EntityUpdateDescriptor entityUpdateDescriptor) throws SObjectModificationException;
}
