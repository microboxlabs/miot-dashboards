package com.microboxlabs.dashboards.datasource.webscript;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.microboxlabs.dashboards.datasource.service.DataSourceConfigService;

public class DataSourceConfigWebscriptTest {

    private DataSourceConfigWebscript webscript;
    private DataSourceConfigService service;
    private WebScriptRequest req;
    private WebScriptResponse res;
    private StringWriter responseWriter;

    @Before
    public void setUp() throws IOException {
        service = mock(DataSourceConfigService.class);
        req = mock(WebScriptRequest.class);
        res = mock(WebScriptResponse.class);
        responseWriter = new StringWriter();
        when(res.getWriter()).thenReturn(responseWriter);

        webscript = new DataSourceConfigWebscript();
        webscript.setDataSourceConfigService(service);
    }

    @Test
    public void testHandleListReturnsDatasources() throws IOException {
        // Match is a final class — use a real instance with template vars
        Match match = new Match("", Map.of("action", "list"), "");
        when(req.getServiceMatch()).thenReturn(match);
        when(req.getParameter("site")).thenReturn("test-site");
        when(service.list("test-site")).thenReturn(List.of(
            Map.of("name", "ds1", "type", "POSTGREST")
        ));

        webscript.execute(req, res);

        String response = responseWriter.toString();
        assertNotNull(response);
        assertTrue(response.contains("ds1"));
        assertTrue(response.contains("POSTGREST"));
        verify(service).list("test-site");
    }
}
