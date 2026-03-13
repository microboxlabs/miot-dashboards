package com.microboxlabs.dashboards.dashboard.webscript;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.microboxlabs.dashboards.dashboard.service.DashboardConfigService;

public class DashboardConfigWebscriptTest {

    private DashboardConfigWebscript webscript;
    private DashboardConfigService service;
    private WebScriptRequest req;
    private WebScriptResponse res;
    private StringWriter responseWriter;

    @Before
    public void setUp() throws IOException {
        service = mock(DashboardConfigService.class);
        req = mock(WebScriptRequest.class, RETURNS_DEEP_STUBS);
        res = mock(WebScriptResponse.class);
        responseWriter = new StringWriter();
        when(res.getWriter()).thenReturn(responseWriter);

        webscript = new DashboardConfigWebscript();
        webscript.setDashboardConfigService(service);
    }

    @Test
    public void testHandleGetReturnsConfig() throws IOException {
        Match match = new Match("", Map.of("action", "get"), "");
        when(req.getServiceMatch()).thenReturn(match);
        when(req.getContent().getContent()).thenReturn("{\"site\":\"test-site\",\"slug\":\"my-dashboard\"}");

        when(service.getConfig("test-site", "my-dashboard")).thenReturn("{\"key\":\"value\"}");

        webscript.execute(req, res);

        String response = responseWriter.toString();
        assertNotNull(response);
        assertTrue(response.contains("data"));
        assertTrue(response.contains("key"));
        verify(service).getConfig("test-site", "my-dashboard");
    }

    @Test
    public void testHandleGetReturnsNullConfig() throws IOException {
        Match match = new Match("", Map.of("action", "get"), "");
        when(req.getServiceMatch()).thenReturn(match);
        when(req.getContent().getContent()).thenReturn("{\"site\":\"test-site\",\"slug\":\"nonexistent\"}");

        when(service.getConfig("test-site", "nonexistent")).thenReturn(null);

        webscript.execute(req, res);

        String response = responseWriter.toString();
        assertNotNull(response);
        assertTrue(response.contains("data"));
        verify(service).getConfig("test-site", "nonexistent");
    }

    @Test
    public void testHandleSaveCallsService() throws IOException {
        Match match = new Match("", Map.of("action", "save"), "");
        when(req.getServiceMatch()).thenReturn(match);
        when(req.getContent().getContent()).thenReturn("{\"site\":\"test-site\",\"slug\":\"my-dashboard\",\"config\":{\"key\":\"value\"}}");

        webscript.execute(req, res);

        String response = responseWriter.toString();
        assertNotNull(response);
        assertTrue(response.contains("success"));
        assertTrue(response.contains("true"));
        verify(service).saveConfig("test-site", "my-dashboard", "{\"key\":\"value\"}");
    }

    @Test
    public void testUnknownActionReturnsBadRequest() throws IOException {
        Match match = new Match("", Map.of("action", "unknown"), "");
        when(req.getServiceMatch()).thenReturn(match);

        webscript.execute(req, res);

        String response = responseWriter.toString();
        assertTrue(response.contains("error"));
        assertTrue(response.contains("Unknown action"));
        verify(res).setStatus(400);
    }
}
