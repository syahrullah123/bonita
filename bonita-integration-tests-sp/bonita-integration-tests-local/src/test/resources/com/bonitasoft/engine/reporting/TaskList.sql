SELECT
    TSK.ID AS TSK_FLOW_NODE_DEFINITION_ID,
    TSK.DISPLAYNAME AS TSK_DISPLAY_NAME,
    TSK.STATENAME AS TSK_STATE_NAME,
    TSK.REACHEDSTATEDATE AS TSK_REACHED_STATE_DATE,
    CS.ID AS CS_ID,
    0 AS CS_ARCHIVED_ID,
    'OPEN' as CS_STATE_NAME,
    APS.PROCESSID AS APS_PROCESS_ID,
    APS.NAME AS APS_NAME,
    USR.FIRSTNAME AS USR_FIRSTNAME,
    USR.LASTNAME AS USR_LASTNAME
FROM flownode_instance TSK
INNER JOIN process_instance CS ON TSK.ROOTCONTAINERID = CS.ID
INNER JOIN process_definition APS ON CS.PROCESSDEFINITIONID = APS.PROCESSID
LEFT OUTER JOIN user_ USR ON (TSK.ASSIGNEEID = USR.ID AND USR.TENANTID = $P{BONITA_TENANT_ID})
WHERE TSK.TENANTID = $P{BONITA_TENANT_ID}
AND TSK.KIND in ('manual','user')
AND CS.TENANTID = $P{BONITA_TENANT_ID}
AND APS.TENANTID = $P{BONITA_TENANT_ID}
$P!{_p_state_name}
AND TSK.REACHEDSTATEDATE BETWEEN $P{_p_date_from} AND $P{_p_date_to}
$P!{_p_apps_id}
UNION
SELECT
    TSK.ID AS TSK_FLOW_NODE_DEFINITION_ID,
    TSK.DISPLAYNAME AS TSK_DISPLAY_NAME,
    (CASE
         WHEN TSK.STATENAME = 'aborted'
             THEN  'completed'
             ELSE TSK.STATENAME
    END) AS TSK_STATE_NAME,
    TSK.REACHEDSTATEDATE AS TSK_REACHED_STATE_DATE,
    CS.ID AS CS_ID,
    0 AS CS_ARCHIVED_ID,
    'OPEN' as CS_STATE_NAME,
    APS.PROCESSID AS APS_PROCESS_ID,
    APS.NAME AS APS_NAME,
    USR.FIRSTNAME AS USR_FIRSTNAME,
    USR.LASTNAME AS USR_LASTNAM
FROM arch_flownode_instance TSK
INNER JOIN process_instance  CS ON TSK.ROOTCONTAINERID = CS.ID
INNER JOIN process_definition APS ON CS.PROCESSDEFINITIONID = APS.PROCESSID
LEFT OUTER JOIN user_ USR ON (TSK.ASSIGNEEID = USR.ID AND USR.TENANTID = $P{BONITA_TENANT_ID})
WHERE TSK.KIND in ('manual','user')
AND TSK.STATEID in (2,16)
AND TSK.TENANTID = $P{BONITA_TENANT_ID}
AND CS.TENANTID = $P{BONITA_TENANT_ID}
AND APS.TENANTID = $P{BONITA_TENANT_ID}
$P!{_p_state_name}
AND TSK.REACHEDSTATEDATE BETWEEN $P{_p_date_from} AND $P{_p_date_to}
$P!{_p_apps_id}
UNION
SELECT
    TSK.ID AS TSK_FLOW_NODE_DEFINITION_ID,
    TSK.DISPLAYNAME AS TSK_DISPLAY_NAME,
    (CASE
         WHEN TSK.STATENAME = 'aborted'
             THEN  'completed'
             ELSE TSK.STATENAME
    END) AS TSK_STATE_NAME,
    TSK.REACHEDSTATEDATE AS TSK_REACHED_STATE_DATE,
    CS.SOURCEOBJECTID AS CS_ID,
    CS.ID AS CS_ARCHIVED_ID,
    'ARCHIVED' as CS_STATE_NAME,
    APS.PROCESSID AS APS_PROCESS_ID,
    APS.NAME AS APS_NAME,
    USR.FIRSTNAME AS USR_FIRSTNAME,
    USR.LASTNAME AS USR_LASTNAM
FROM arch_flownode_instance TSK
INNER JOIN arch_process_instance CS ON TSK.ROOTCONTAINERID = CS.SOURCEOBJECTID
INNER JOIN process_definition APS ON CS.PROCESSDEFINITIONID = APS.PROCESSID
LEFT OUTER JOIN user_ USR ON (TSK.ASSIGNEEID = USR.ID AND USR.TENANTID = $P{BONITA_TENANT_ID})
WHERE TSK.KIND in ('manual','user')
AND TSK.STATEID in (2,16)
AND TSK.TENANTID = $P{BONITA_TENANT_ID}
AND CS.STATEID in (3,4,6)
AND CS.TENANTID = $P{BONITA_TENANT_ID}
AND APS.TENANTID = $P{BONITA_TENANT_ID}
$P!{_p_state_name}
AND TSK.REACHEDSTATEDATE BETWEEN $P{_p_date_from} AND $P{_p_date_to}
$P!{_p_apps_id}