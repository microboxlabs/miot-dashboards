package com.microboxlabs.dashboards.dashboard.service;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardConfigServiceImpl implements DashboardConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardConfigServiceImpl.class);

    private static final String DOCUMENT_LIBRARY = "documentLibrary";
    private static final String DASHBOARD_FOLDER = "dashboard";
    private static final String MIMETYPE_JSON = "application/json";

    private NodeService nodeService;
    private SiteService siteService;
    private FileFolderService fileFolderService;
    private ContentService contentService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    @Override
    public String getConfig(String siteShortName, String slug) {
        var docLib = getDocumentLibrary(siteShortName);
        if (docLib == null) {
            return null;
        }

        var dashboardFolder = fileFolderService.searchSimple(docLib, DASHBOARD_FOLDER);
        if (dashboardFolder == null) {
            return null;
        }

        var configFile = fileFolderService.searchSimple(dashboardFolder, toFileName(slug));
        if (configFile == null) {
            return null;
        }

        var reader = contentService.getReader(configFile, ContentModel.PROP_CONTENT);
        if (reader == null || !reader.exists()) {
            return null;
        }

        return reader.getContentString();
    }

    @Override
    public void saveConfig(String siteShortName, String slug, String configJson) {
        var docLib = getDocumentLibrary(siteShortName);
        if (docLib == null) {
            throw new IllegalStateException("Document library not found for site: " + siteShortName);
        }

        var dashboardFolder = ensureDashboardFolder(docLib);
        var fileName = toFileName(slug);
        var configFile = fileFolderService.searchSimple(dashboardFolder, fileName);

        if (configFile == null) {
            configFile = fileFolderService.create(dashboardFolder, fileName, ContentModel.TYPE_CONTENT).getNodeRef();
            logger.debug("Created dashboard config file {} for site {}", fileName, siteShortName);
        }

        var writer = contentService.getWriter(configFile, ContentModel.PROP_CONTENT, true);
        writer.setMimetype("application/json");
        writer.setEncoding("UTF-8");
        writer.putContent(configJson);
    }

    private NodeRef getDocumentLibrary(String siteShortName) {
        var site = siteService.getSite(siteShortName);
        if (site == null) {
            throw new IllegalStateException("Site not found: " + siteShortName);
        }
        return siteService.getContainer(siteShortName, DOCUMENT_LIBRARY);
    }

    private NodeRef ensureDashboardFolder(NodeRef docLib) {
        var existing = fileFolderService.searchSimple(docLib, DASHBOARD_FOLDER);
        if (existing != null) {
            return existing;
        }
        var folder = fileFolderService.create(docLib, DASHBOARD_FOLDER, ContentModel.TYPE_FOLDER).getNodeRef();
        nodeService.setProperty(folder, ContentModel.PROP_TITLE, "Dashboard");
        logger.debug("Created dashboard folder in document library");
        return folder;
    }

    private String toFileName(String slug) {
        return slug + "-config.json";
    }
}
