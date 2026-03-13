package com.microboxlabs.dashboards.dashboard.webscript;

import java.io.IOException;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.microboxlabs.dashboards.dashboard.service.DashboardConfigService;

public class DashboardConfigWebscript extends AbstractWebScript {

    private static final Logger logger = LoggerFactory.getLogger(DashboardConfigWebscript.class);

    private static final String CONTENT_TYPE_JSON = "application/json";

    private DashboardConfigService dashboardConfigService;

    public void setDashboardConfigService(DashboardConfigService service) {
        this.dashboardConfigService = service;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var action = req.getServiceMatch().getTemplateVars().get("action");
        try {
            switch (action) {
                case "get":
                    handleGet(req, res);
                    break;
                case "save":
                    handleSave(req, res);
                    break;
                default:
                    res.setContentType(CONTENT_TYPE_JSON);
                    res.setStatus(Status.STATUS_BAD_REQUEST);
                    res.getWriter().write(jsonError("Unknown action"));
            }
        } catch (WebScriptException e) {
            throw e;
        } catch (IllegalStateException e) {
            res.setStatus(Status.STATUS_NOT_FOUND);
            res.setContentType(CONTENT_TYPE_JSON);
            res.getWriter().write(jsonError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Dashboard Config API error", e);
            res.setStatus(Status.STATUS_INTERNAL_SERVER_ERROR);
            res.setContentType(CONTENT_TYPE_JSON);
            res.getWriter().write(jsonError(e.getMessage()));
        }
    }

    private void handleGet(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var body = new JSONObject(req.getContent().getContent());
        var site = body.optString("site", null);
        var slug = body.optString("slug", null);

        if (site == null || site.isBlank() || slug == null || slug.isBlank()) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing required parameters: site, slug");
        }

        var configJson = dashboardConfigService.getConfig(site, slug);

        res.setContentType("application/json;charset=UTF-8");
        var response = new JSONObject();
        response.put("data", configJson != null ? new JSONObject(configJson) : JSONObject.NULL);
        res.getWriter().write(response.toString());
    }

    private void handleSave(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var body = new JSONObject(req.getContent().getContent());
        var site = body.optString("site", null);
        var slug = body.optString("slug", null);

        if (site == null || site.isBlank() || slug == null || slug.isBlank()) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing required parameters: site, slug");
        }

        if (!body.has("config")) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing required fields: site, slug, and config");
        }

        var config = body.getJSONObject("config");
        dashboardConfigService.saveConfig(site, slug, config.toString());

        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(new JSONObject(Map.of("success", true)).toString());
    }

    private String jsonError(String message) {
        return new JSONObject(Map.of("error", message)).toString();
    }
}
