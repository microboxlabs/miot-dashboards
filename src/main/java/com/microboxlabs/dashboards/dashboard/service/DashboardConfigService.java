package com.microboxlabs.dashboards.dashboard.service;

public interface DashboardConfigService {
    String getConfig(String siteShortName, String slug);
    void saveConfig(String siteShortName, String slug, String configJson);
}
