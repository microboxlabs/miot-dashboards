package com.microboxlabs.dashboards.datasource.webscript;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.alibaba.fastjson2.JSON;

import com.microboxlabs.dashboards.datasource.service.DataSourceConfigService;

public class DataSourceConfigWebscript extends AbstractWebScript {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfigWebscript.class);

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String KEY_NODE_REF = "nodeRef";
    private static final String KEY_CONFIG = "config";
    private static final String KEY_IS_ACTIVE = "isActive";
    private static final String KEY_LAST_TESTED_AT = "lastTestedAt";
    private static final String KEY_LAST_TEST_RESULT = "lastTestResult";

    private DataSourceConfigService service;

    public void setDataSourceConfigService(DataSourceConfigService service) {
        this.service = service;
    }

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var action = req.getServiceMatch().getTemplateVars().get("action");
        try {
            switch (action) {
                case "list":
                    handleList(req, res);
                    break;
                case "create":
                    handleCreate(req, res);
                    break;
                case "update":
                    handleUpdate(req, res);
                    break;
                case "delete":
                    handleDelete(req, res);
                    break;
                case "get":
                    handleGet(req, res);
                    break;
                default:
                    res.setContentType(CONTENT_TYPE_JSON);
                    res.setStatus(Status.STATUS_BAD_REQUEST);
                    res.getWriter().write(jsonError("Unknown action"));
            }
        } catch (WebScriptException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Data Source Config API error", e);
            res.setStatus(Status.STATUS_INTERNAL_SERVER_ERROR);
            res.setContentType(CONTENT_TYPE_JSON);
            res.getWriter().write(jsonError(e.getMessage()));
        }
    }

    private void handleList(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var site = req.getParameter("site");
        if (site == null || site.isBlank()) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing required parameter: site");
        }
        var dataSources = service.list(site);
        writeOk(res, Map.of("dataSources", dataSources));
    }

    private void handleCreate(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var body = new JSONObject(req.getContent().getContent());
        var site = body.getString("site");
        var props = bodyToMap(body);
        var created = service.create(site, props);
        writeOk(res, created);
    }

    private void handleUpdate(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var body = new JSONObject(req.getContent().getContent());
        var nodeRefStr = body.getString(KEY_NODE_REF);
        var site = body.getString("site");
        var nodeRef = new NodeRef(nodeRefStr);
        var props = bodyToMap(body);
        var updated = service.update(nodeRef, site, props);
        if (updated.isEmpty()) {
            writeNotFound(res);
            return;
        }
        writeOk(res, updated);
    }

    private void handleDelete(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var body = new JSONObject(req.getContent().getContent());
        var nodeRefStr = body.getString(KEY_NODE_REF);
        var nodeRef = new NodeRef(nodeRefStr);
        var exists = service.get(nodeRef);
        if (exists.isEmpty()) {
            writeNotFound(res);
            return;
        }
        service.delete(nodeRef);
        writeOk(res, Map.of("success", true));
    }

    private void handleGet(WebScriptRequest req, WebScriptResponse res) throws IOException {
        var nodeRefStr = req.getParameter(KEY_NODE_REF);
        if (nodeRefStr == null || nodeRefStr.isBlank()) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Missing required parameter: nodeRef");
        }
        var nodeRef = new NodeRef(nodeRefStr);
        var result = service.get(nodeRef);
        if (result.isEmpty()) {
            writeNotFound(res);
            return;
        }
        writeOk(res, result);
    }

    private Map<String, Object> bodyToMap(JSONObject body) {
        Map<String, Object> props = new HashMap<>();
        putIfHas(props, body, "name");
        putIfHas(props, body, "type");
        putIfHas(props, body, "description");
        putIfHas(props, body, "url");
        if (body.has(KEY_CONFIG)) {
            props.put(KEY_CONFIG, body.getJSONObject(KEY_CONFIG).toMap());
        }
        if (body.has(KEY_IS_ACTIVE)) {
            props.put(KEY_IS_ACTIVE, body.getBoolean(KEY_IS_ACTIVE));
        }
        if (body.has(KEY_LAST_TESTED_AT)) {
            var val = body.optString(KEY_LAST_TESTED_AT, null);
            if (val != null) {
                props.put(KEY_LAST_TESTED_AT, Date.from(java.time.Instant.parse(val)));
            }
        }
        if (body.has(KEY_LAST_TEST_RESULT)) {
            props.put(KEY_LAST_TEST_RESULT, body.optBoolean(KEY_LAST_TEST_RESULT));
        }
        return props;
    }

    private void putIfHas(Map<String, Object> props, JSONObject body, String key) {
        if (body.has(key)) {
            props.put(key, body.optString(key, null));
        }
    }

    private void writeOk(WebScriptResponse res, Map<String, Object> map) throws IOException {
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(JSON.toJSONString(map));
    }

    private void writeNotFound(WebScriptResponse res) throws IOException {
        res.setStatus(Status.STATUS_NOT_FOUND);
        res.setContentType(CONTENT_TYPE_JSON);
        res.getWriter().write(jsonError("Data source not found"));
    }

    private String jsonError(String message) {
        return new JSONObject(Map.of("error", message)).toString();
    }
}
