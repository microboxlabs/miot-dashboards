package com.microboxlabs.dashboards.datasource.service;

import com.microboxlabs.dashboards.datasource.model.DataSourceModel;

import com.alibaba.fastjson2.JSON;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.model.DataListModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceConfigServiceImpl implements DataSourceConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfigServiceImpl.class);

    private static final String DATA_LISTS_SEGMENT = "dataLists";
    private static final String DATA_SOURCE_CONFIGS_CONTAINER = "dataSourceConfigs";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_LAST_TESTED_AT = "lastTestedAt";

    private NodeService nodeService;
    private SiteService siteService;
    private FileFolderService fileFolderService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    @Override
    public List<Map<String, Object>> list(String siteShortName) {
        var container = findDataSourceContainer(siteShortName);
        if (container == null) {
            return List.of();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (var fi : fileFolderService.list(container)) {
            var nodeRef = fi.getNodeRef();
            if (nodeService.getType(nodeRef).equals(DataSourceModel.TYPE_DATA_SOURCE_CONFIG)) {
                results.add(toMap(nodeRef, siteShortName));
            }
        }
        return results;
    }

    @Override
    public Map<String, Object> get(NodeRef nodeRef) {
        if (!nodeService.exists(nodeRef)) {
            return Collections.emptyMap();
        }
        var parent = nodeService.getPrimaryParent(nodeRef).getParentRef();
        var siteShortName = resolveSiteFromContainer(parent);
        return toMap(nodeRef, siteShortName);
    }

    @Override
    public Map<String, Object> create(String siteShortName, Map<String, Object> props) {
        var container = ensureDataSourceContainer(siteShortName);

        Map<QName, Serializable> nodeProps = new HashMap<>();
        mapInputToProperties(nodeProps, props);

        String name = (String) props.get("name");
        QName assocQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);

        var nodeRef = nodeService.createNode(
            container,
            ContentModel.ASSOC_CONTAINS,
            assocQName,
            DataSourceModel.TYPE_DATA_SOURCE_CONFIG,
            nodeProps
        ).getChildRef();

        return toMap(nodeRef, siteShortName);
    }

    @Override
    public Map<String, Object> update(NodeRef nodeRef, String siteShortName, Map<String, Object> props) {
        if (!nodeService.exists(nodeRef)) {
            return Collections.emptyMap();
        }

        Map<QName, Serializable> nodeProps = new HashMap<>();
        mapInputToProperties(nodeProps, props);
        if (!nodeProps.isEmpty()) {
            nodeService.addProperties(nodeRef, nodeProps);
        }

        return toMap(nodeRef, siteShortName);
    }

    @Override
    public void delete(NodeRef nodeRef) {
        nodeService.deleteNode(nodeRef);
    }

    private NodeRef findDataSourceContainer(String siteShortName) {
        var dataLists = getDataListsContainer(siteShortName);
        if (dataLists == null) {
            return null;
        }
        return fileFolderService.searchSimple(dataLists, DATA_SOURCE_CONFIGS_CONTAINER);
    }

    private NodeRef ensureDataSourceContainer(String siteShortName) {
        var dataLists = getDataListsContainer(siteShortName);
        if (dataLists == null) {
            throw new IllegalStateException("Data Lists not found for site: " + siteShortName);
        }

        var existing = fileFolderService.searchSimple(dataLists, DATA_SOURCE_CONFIGS_CONTAINER);
        if (existing != null) {
            return existing;
        }

        var list = fileFolderService.create(dataLists, DATA_SOURCE_CONFIGS_CONTAINER, DataListModel.TYPE_DATALIST)
                .getNodeRef();
        nodeService.setProperty(list, ContentModel.PROP_TITLE, "Data Source Configs");
        nodeService.setProperty(list, ContentModel.PROP_DESCRIPTION,
                "Data source provider configurations for external API connections.");
        nodeService.setProperty(list, DataListModel.PROP_DATALIST_ITEM_TYPE,
                DataSourceModel.PREFIX_TYPE_DATA_SOURCE_CONFIG);
        logger.debug("Created dataSourceConfigs data list container for site {}", siteShortName);
        return list;
    }

    private NodeRef getDataListsContainer(String siteShortName) {
        var site = siteService.getSite(siteShortName);
        if (site == null) {
            throw new IllegalStateException("Site not found: " + siteShortName);
        }
        return siteService.getContainer(siteShortName, DATA_LISTS_SEGMENT);
    }

    private String resolveSiteFromContainer(NodeRef containerRef) {
        var siteInfo = siteService.getSite(containerRef);
        return siteInfo != null ? siteInfo.getShortName() : null;
    }

    private void mapInputToProperties(Map<QName, Serializable> nodeProps, Map<String, Object> input) {
        putIfPresent(nodeProps, DataSourceModel.PROP_DS_NAME, input, "name");
        putIfPresent(nodeProps, DataSourceModel.PROP_DS_TYPE, input, "type");
        putIfPresent(nodeProps, DataSourceModel.PROP_DS_DESCRIPTION, input, "description");
        putIfPresent(nodeProps, DataSourceModel.PROP_DS_URL, input, "url");
        if (input.containsKey(KEY_CONFIG)) {
            Object config = input.get(KEY_CONFIG);
            if (config != null) {
                nodeProps.put(DataSourceModel.PROP_DS_CONFIG_JSON, JSON.toJSONString(config));
            }
        }
        putIfPresent(nodeProps, DataSourceModel.PROP_DS_IS_ACTIVE, input, "isActive");
        putIfPresent(nodeProps, DataSourceModel.PROP_DS_LAST_TESTED_AT, input, KEY_LAST_TESTED_AT);
        putIfPresent(nodeProps, DataSourceModel.PROP_DS_LAST_TEST_RESULT, input, "lastTestResult");
    }

    private void putIfPresent(Map<QName, Serializable> nodeProps, QName qname, Map<String, Object> input, String key) {
        if (input.containsKey(key)) {
            nodeProps.put(qname, (Serializable) input.get(key));
        }
    }

    private Map<String, Object> toMap(NodeRef nodeRef, String siteShortName) {
        Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodeRef", nodeRef.toString());
        result.put("name", props.get(DataSourceModel.PROP_DS_NAME));
        result.put("type", props.get(DataSourceModel.PROP_DS_TYPE));
        result.put("description", props.get(DataSourceModel.PROP_DS_DESCRIPTION));
        result.put("url", props.get(DataSourceModel.PROP_DS_URL));
        String configJson = (String) props.get(DataSourceModel.PROP_DS_CONFIG_JSON);
        result.put(KEY_CONFIG, configJson != null && !configJson.isBlank()
            ? JSON.parseObject(configJson) : null);
        result.put("isActive", props.get(DataSourceModel.PROP_DS_IS_ACTIVE));

        var lastTestedAt = (Date) props.get(DataSourceModel.PROP_DS_LAST_TESTED_AT);
        if (lastTestedAt != null) {
            result.put(KEY_LAST_TESTED_AT, ZonedDateTime.ofInstant(lastTestedAt.toInstant(), ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        } else {
            result.put(KEY_LAST_TESTED_AT, null);
        }

        result.put("lastTestResult", props.get(DataSourceModel.PROP_DS_LAST_TEST_RESULT));
        result.put("site", siteShortName);
        return result;
    }
}
