CREATE TABLE data_instance (
    tenantId INT8 NOT NULL,
	id INT8 NOT NULL,
	name VARCHAR(50),
	description VARCHAR(50),
	transientData BOOLEAN,
	className VARCHAR(100),
	containerId INT8,
	containerType VARCHAR(60),
	namespace VARCHAR(100),
	element VARCHAR(60),
	intValue INT,
	longValue INT8,
	shortTextValue VARCHAR(255),
	booleanValue BOOLEAN,
	doubleValue NUMERIC(19,5),
	floatValue REAL,
	blobValue BYTEA,
	clobValue TEXT,
	discriminant VARCHAR(50) NOT NULL,
	PRIMARY KEY (tenantid, id)
);

CREATE INDEX idx_datai_container ON data_instance (tenantId, containerId, containerType, name);

