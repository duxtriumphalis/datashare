package org.icij.datashare;

import net.codestory.http.WebServer;
import net.codestory.http.misc.Env;
import net.codestory.rest.FluentRestTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DocumentResourceTest implements FluentRestTest {
    private static WebServer server = new WebServer() {
        @Override
        protected Env createEnv() {
            return Env.prod();
        }
    }.startOnRandomPort();
    @Mock Repository repository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        server.configure(routes -> routes.add(new DocumentResource(repository)));
    }

    @Test
    public void testStarDocument() throws Exception {
        when(repository.star(any(), any())).thenReturn(true).thenReturn(false);
        put("/api/document/star/doc_id").should().respond(201);
        put("/api/document/star/doc_id").should().respond(200);
    }

    @Test
    public void testUnstarDocument() throws Exception {
        when(repository.unstar(any(), any())).thenReturn(true).thenReturn(false);
        put("/api/document/unstar/doc_id").should().respond(201);
        put("/api/document/unstar/doc_id").should().respond(200);
    }

    @Test
    public void testGetStarredDocuments() throws Exception {
        when(repository.getStarredDocuments(any())).thenReturn(Arrays.asList("doc1", "doc2"));
        get("/api/document/starred").should().respond(200).haveType("application/json").contain("[\"doc1\",\"doc2\"]");
    }

    @Override
    public int port() { return server.port();}
}
