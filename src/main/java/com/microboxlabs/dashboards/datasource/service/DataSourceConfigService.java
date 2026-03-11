package com.microboxlabs.dashboards.datasource.service;

import java.util.List;
import java.util.Map;
import org.alfresco.service.cmr.repository.NodeRef;

public interface DataSourceConfigService {
    List<Map<String, Object>> list(String siteShortName);
    Map<String, Object> get(NodeRef nodeRef);
    Map<String, Object> create(String siteShortName, Map<String, Object> props);
    Map<String, Object> update(NodeRef nodeRef, String siteShortName, Map<String, Object> props);
    void delete(NodeRef nodeRef);
}
