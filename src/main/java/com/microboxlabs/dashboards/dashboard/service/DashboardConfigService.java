package com.microboxlabs.dashboards.dashboard.service;

import java.util.Map;

public interface DashboardConfigService {
    String getConfig(String siteShortName, String slug);
    Map<String, String> listConfigs(String siteShortName);
    void saveConfig(String siteShortName, String slug, String configJson);
    boolean deleteConfig(String siteShortName, String slug);
}
