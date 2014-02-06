/*******************************************************************************
 * Copyright (C) 2013-2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine.business.data.impl;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.xml.transform.TransformerException;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaRuntimeException;
import org.bonitasoft.engine.commons.io.IOUtil;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.SDependencyAlreadyExistsException;
import org.bonitasoft.engine.dependency.SDependencyCreationException;
import org.bonitasoft.engine.dependency.SDependencyException;
import org.bonitasoft.engine.dependency.model.SDependency;
import org.bonitasoft.engine.dependency.model.SDependencyMapping;
import org.bonitasoft.engine.dependency.model.builder.SDependencyBuilderFactory;
import org.bonitasoft.engine.dependency.model.builder.SDependencyMappingBuilderFactory;
import org.hibernate.dialect.Dialect;

import com.bonitasoft.engine.business.data.BusinessDataNotFoundException;
import com.bonitasoft.engine.business.data.BusinessDataRespository;
import com.bonitasoft.engine.business.data.NonUniqueResultException;
import com.bonitasoft.engine.business.data.SBusinessDataRepositoryDeploymentException;
import com.bonitasoft.engine.business.data.SBusinessDataRepositoryException;

/**
 * @author Matthieu Chaffotte
 * @author Romain Bioteau
 */
public class JPABusinessDataRepositoryImpl implements BusinessDataRespository {

    private static final String DEFAULT_DATA_SOURCE_NAME = "java:comp/env/BusinessDataDS";

    private final String dataSourceName;

    private final DependencyService dependencyService;

    private EntityManagerFactory entityManagerFactory;

    private List<String> classNameList;

    public JPABusinessDataRepositoryImpl(final DependencyService dependencyService) {
        this(dependencyService, DEFAULT_DATA_SOURCE_NAME);
    }

    public JPABusinessDataRepositoryImpl(final DependencyService dependencyService, final String dataSourceName) {
        this.dependencyService = dependencyService;
        this.dataSourceName = dataSourceName;
    }

    @Override
    public void deploy(final byte[] bdrArchive, final long tenantId) throws SBusinessDataRepositoryException {
        byte[] transformedBdrArchive = null;
        try {
            transformedBdrArchive = transformBDRArchive(bdrArchive);
        } catch (final IOException e) {
            throw new SBusinessDataRepositoryDeploymentException(e);
        } catch (final TransformerException e) {
            throw new SBusinessDataRepositoryDeploymentException(e);
        }

        final SDependency sDependency = createSDependency(transformedBdrArchive);
        try {
            dependencyService.createDependency(sDependency);
            final SDependencyMapping sDependencyMapping = createDependencyMapping(tenantId, sDependency);
            dependencyService.createDependencyMapping(sDependencyMapping);
        } catch (final SDependencyAlreadyExistsException e) {
            throw new SBusinessDataRepositoryDeploymentException(e);
        } catch (final SDependencyCreationException e) {
            throw new SBusinessDataRepositoryDeploymentException(e);
        } catch (final SDependencyException e) {
            throw new SBusinessDataRepositoryDeploymentException(e);
        }
    }

    protected SDependencyMapping createDependencyMapping(final long tenantId, final SDependency sDependency) {
        return BuilderFactory.get(SDependencyMappingBuilderFactory.class).createNewInstance(sDependency.getId(), tenantId, "tenant").done();
    }

    protected SDependency createSDependency(final byte[] transformedBdrArchive) {
        return BuilderFactory.get(SDependencyBuilderFactory.class).createNewInstance("BDR", "1.0", "BDR.jar", transformedBdrArchive).done();
    }

    protected byte[] transformBDRArchive(final byte[] bdrArchive) throws SBusinessDataRepositoryDeploymentException, IOException, TransformerException {
        try {
            classNameList = IOUtil.getClassNameList(bdrArchive);
        } catch (final IOException e) {
            throw new SBonitaRuntimeException(e);
        }

        if (classNameList == null || classNameList.isEmpty()) {
            throw new IllegalStateException("No entity found in bdr archive");
        }
        final byte[] persistenceFileContent = getPersistenceFileContentFor(classNameList);
        return IOUtil.addJarEntry(bdrArchive, "META-INF/persistence.xml", persistenceFileContent);
    }

    public List<String> getClassNameList() {
        return classNameList;
    }

    public void setClassNameList(final List<String> classNameList) {
        this.classNameList = classNameList;
    }

    protected byte[] getPersistenceFileContentFor(final List<String> classNames) throws SBusinessDataRepositoryDeploymentException, IOException,
            TransformerException {
        final PersistenceUnitBuilder builder = new PersistenceUnitBuilder();
        for (final String classname : classNames) {
            builder.addClass(classname);
        }
        return IOUtil.toByteArray(builder.done());
    }

    @Override
    public void start() throws SBusinessDataRepositoryDeploymentException {
        final Map<String, Object> configOverrides = new HashMap<String, Object>();
        configOverrides.put("hibernate.ejb.resource_scanner", InactiveScanner.class.getName());
        configOverrides.put("hibernate.connection.datasource", dataSourceName);
        entityManagerFactory = Persistence.createEntityManagerFactory("BDR", configOverrides);
        final Properties properties = toProperties(entityManagerFactory.getProperties());
        final Dialect dialect = Dialect.getDialect(properties);
        try {
            executeQueries(new SchemaGenerator(dialect, properties, getClassNameList()).generate());
        } catch (final SQLException e) {
            throw new SBusinessDataRepositoryDeploymentException(e);
        }
    }

    private void executeQueries(final String... sqlQuerys) {
        final EntityManager entityManager = getEntityManager();
        for (final String sqlQuery : sqlQuerys) {
            final Query query = entityManager.createNativeQuery(sqlQuery);
            query.executeUpdate();
        }
    }

    private Properties toProperties(final Map<String, Object> propertiesAsMap) {
        final Properties properties = new Properties();
        properties.putAll(propertiesAsMap);
        return properties;
    }

    @Override
    public void stop() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
    }

    @Override
    public <T> T find(final Class<T> entityClass, final Serializable primaryKey) throws BusinessDataNotFoundException {
        final EntityManager em = getEntityManager();
        final T entity = em.find(entityClass, primaryKey);
        if (entity == null) {
            throw new BusinessDataNotFoundException("Impossible to get data with id: " + primaryKey);
        }
        return entity;
    }

    @Override
    public <T> T find(final Class<T> resultClass, final String qlString, final Map<String, Object> parameters) throws BusinessDataNotFoundException,
            NonUniqueResultException {
        final EntityManager em = getEntityManager();
        final TypedQuery<T> query = em.createQuery(qlString, resultClass);
        if (parameters != null) {
            for (final Entry<String, Object> parameter : parameters.entrySet()) {
                query.setParameter(parameter.getKey(), parameter.getValue());
            }
        }
        try {
            return query.getSingleResult();
        } catch (final javax.persistence.NonUniqueResultException nure) {
            throw new NonUniqueResultException(nure);
        } catch (final NoResultException nre) {
            throw new BusinessDataNotFoundException("Impossible to get data using query: " + qlString + " and parameters: " + parameters, nre);
        }
    }

    private EntityManager getEntityManager() {
        if (entityManagerFactory == null) {
            throw new IllegalStateException("The BDR is not started");
        }
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.joinTransaction();
        return entityManager;
    }

    @Override
    public void persist(final Object entity) {
        if (entity == null) {
            return;
        }
        final EntityManager em = getEntityManager();
        em.persist(entity);
    }

}
