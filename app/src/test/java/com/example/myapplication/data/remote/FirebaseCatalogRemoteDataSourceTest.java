package com.example.myapplication.data.remote;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;

import javax.net.ssl.HttpsURLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FirebaseCatalogRemoteDataSourceTest {
    private static final URI ENDPOINT = URI.create("https://example.test/catalog.json");

    @Test
    public void readsUtf8SnapshotOverGetAndClosesTheConnection() throws IOException {
        String json = "{\"meta\":{\"schemaVersion\":1,\"revision\":1},\"title\":\"Canh chua cá\"}";
        TestConnection connection = connection(200, json);
        FirebaseCatalogRemoteDataSource remote = new FirebaseCatalogRemoteDataSource(ENDPOINT, url -> {
            assertEquals(ENDPOINT.toString(), url.toString());
            return connection;
        });

        assertEquals(json, remote.fetchSnapshot());
        assertEquals("GET", connection.getRequestMethod());
        assertEquals("application/json", connection.getRequestProperty("Accept"));
        assertTrue(connection.getConnectTimeout() > 0);
        assertTrue(connection.getReadTimeout() > 0);
        assertFalse(connection.getInstanceFollowRedirects());
        assertFalse(connection.getUseCaches());
        assertTrue(connection.streamClosed);
        assertTrue(connection.disconnected);
    }

    @Test
    public void rejectsRedirectPermissionDeniedAndServerErrorsBeforeReadingBody() throws IOException {
        for (int status : new int[]{302, 401, 403, 500}) {
            TestConnection connection = connection(status, "{}");

            FirebaseCatalogRemoteDataSource.HttpStatusException error = assertThrows(
                    FirebaseCatalogRemoteDataSource.HttpStatusException.class, remote(connection)::fetchSnapshot);

            assertEquals(status, error.statusCode);
            assertFalse(connection.streamOpened);
            assertTrue(connection.disconnected);
        }
    }

    @Test
    public void propagatesTimeoutAndStillDisconnects() throws IOException {
        TestConnection connection = connection(200, "{}");
        connection.timeout = new SocketTimeoutException("test timeout");

        assertThrows(SocketTimeoutException.class, remote(connection)::fetchSnapshot);

        assertTrue(connection.disconnected);
    }

    @Test
    public void doesNotTreatNullOrBlankResponseAsAnEmptyCatalog() throws IOException {
        for (String body : new String[]{" \n null \n", "", "  \r\n"}) {
            TestConnection connection = connection(200, body);

            assertThrows(IOException.class, remote(connection)::fetchSnapshot);

            assertTrue(connection.streamClosed);
            assertTrue(connection.disconnected);
        }
    }

    @Test
    public void enforcesResponseLimitWithAndWithoutContentLength() throws IOException {
        int oversized = 2 * 1024 * 1024 + 1;
        TestConnection declared = connection(200, "{}");
        declared.contentLength = oversized;
        assertThrows(IOException.class, remote(declared)::fetchSnapshot);
        assertFalse(declared.streamOpened);
        assertTrue(declared.disconnected);

        TestConnection streamed = new TestConnection(ENDPOINT.toURL(), 200, new byte[oversized]);
        assertThrows(IOException.class, remote(streamed)::fetchSnapshot);
        assertTrue(streamed.streamClosed);
        assertTrue(streamed.disconnected);
    }

    @Test
    public void rejectsCleartextAndCredentialBearingEndpoints() {
        for (String url : new String[]{"http://example.test/catalog.json",
                "https://user@example.test/catalog.json", "https://example.test/catalog.json?auth=example"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new FirebaseCatalogRemoteDataSource(URI.create(url)));
        }
    }

    private static FirebaseCatalogRemoteDataSource remote(TestConnection connection) {
        return new FirebaseCatalogRemoteDataSource(ENDPOINT, url -> connection);
    }

    private static TestConnection connection(int status, String body) throws IOException {
        return new TestConnection(ENDPOINT.toURL(), status, body.getBytes(StandardCharsets.UTF_8));
    }

    private static final class TestConnection extends HttpsURLConnection {
        private final int status;
        private final byte[] body;
        private int contentLength = -1;
        private SocketTimeoutException timeout;
        private boolean streamOpened;
        private boolean streamClosed;
        private boolean disconnected;

        private TestConnection(URL url, int status, byte[] body) {
            super(url);
            this.status = status;
            this.body = body;
        }

        @Override public int getResponseCode() throws IOException {
            if (timeout != null) throw timeout;
            return status;
        }
        @Override public int getContentLength() { return contentLength; }
        @Override public InputStream getInputStream() {
            streamOpened = true;
            return new ByteArrayInputStream(body) {
                @Override public void close() { streamClosed = true; }
            };
        }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
        @Override public String getCipherSuite() { throw new UnsupportedOperationException(); }
        @Override public Certificate[] getLocalCertificates() { throw new UnsupportedOperationException(); }
        @Override public Certificate[] getServerCertificates() { throw new UnsupportedOperationException(); }
    }
}
