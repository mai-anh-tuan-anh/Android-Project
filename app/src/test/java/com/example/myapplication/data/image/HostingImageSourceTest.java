package com.example.myapplication.data.image;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.cert.Certificate;

import javax.net.ssl.HttpsURLConnection;

import static org.junit.Assert.*;

public class HostingImageSourceTest {
    private final HostingImageUrlResolver resolver = new HostingImageUrlResolver(HostingImageUrlResolver.PRODUCTION_ORIGIN);

    @Test public void resolvesWithinHostingOriginAndVersionChangesCacheKey() {
        URI first = resolver.resolve("recipes/recipe-0001/cover-v1.jpg");
        URI second = resolver.resolve("recipes/recipe-0001/cover-v2.jpg");
        assertEquals("https://bep-nha-2ca75.web.app/recipes/recipe-0001/cover-v1.jpg", first.toString());
        assertEquals(64, RecipeImageLoader.cacheKey(first).length());
        assertNotEquals(RecipeImageLoader.cacheKey(first), RecipeImageLoader.cacheKey(second));
    }

    @Test public void rejectsAbsolutePathsTraversalAndAmbiguousPercentEncoding() {
        for (String path : new String[]{"", "/cover.jpg", "https://example.test/x.jpg", "//example.test/x.jpg",
                "file:///cover.jpg", "http://bep-nha-2ca75.web.app/cover.jpg", "recipes/../cover.jpg",
                "recipes/%2e%2e/cover.jpg", "recipes/%252e%252e/cover.jpg", "recipes%2fcover.jpg",
                "recipes/%5cfoo.jpg", "recipes//cover.jpg", "recipes/cover.jpg?x=1", "recipes/cover.jpg#part"}) {
            assertThrows(path, IllegalArgumentException.class, () -> resolver.resolve(path));
        }
    }

    @Test public void readsSuccessfulImageAndClosesConnection() throws Exception {
        FakeConnection connection = fake();
        assertArrayEquals(connection.body, fetch(connection, 5));
        assertTrue(connection.disconnected);
        assertTrue(connection.closed);
        assertFalse(connection.getInstanceFollowRedirects());
        assertTrue(connection.getReadTimeout() > 0 && connection.getConnectTimeout() > 0);
    }

    @Test public void rejectsRedirectWrongMimeAndForeignOrigin() throws Exception {
        FakeConnection redirect = fake();
        redirect.status = 302;
        assertThrows(IOException.class, () -> fetch(redirect, 5));
        assertTrue(redirect.disconnected);
        assertFalse(redirect.opened);
        FakeConnection wrongMime = fake();
        wrongMime.mime = "text/html";
        assertThrows(IOException.class, () -> fetch(wrongMime, 5));
        HostingImageSource source = new HostingImageSource(resolver, 5, url -> {
            throw new AssertionError("foreign origin opened a connection");
        });
        assertThrows(IOException.class, () -> source.fetch(URI.create("https://example.test/cover.jpg")));
    }

    @Test public void enforcesDeclaredAndStreamedSizeLimits() throws Exception {
        FakeConnection largeHeader = fake();
        largeHeader.declared = 10;
        assertThrows(IOException.class, () -> fetch(largeHeader, 4));
        assertFalse(largeHeader.opened);
        FakeConnection largeStream = fake();
        largeStream.declared = -1;
        assertThrows(IOException.class, () -> fetch(largeStream, 2));
        assertTrue(largeStream.closed);
        assertTrue(largeStream.disconnected);
    }

    private byte[] fetch(FakeConnection connection, int maxBytes) throws IOException {
        return new HostingImageSource(resolver, maxBytes, url -> connection).fetch(resolver.resolve("recipes/cover-v1.jpg"));
    }

    private FakeConnection fake() throws Exception {
        return new FakeConnection(resolver.resolve("recipes/cover-v1.jpg").toURL());
    }

    private static final class FakeConnection extends HttpsURLConnection {
        final byte[] body = {1, 2, 3, 4};
        long declared = body.length;
        int status = 200;
        String mime = "image/jpeg; charset=binary";
        boolean disconnected;
        boolean opened;
        boolean closed;

        FakeConnection(URL url) { super(url); }
        @Override public int getResponseCode() { return status; }
        @Override public String getContentType() { return mime; }
        @Override public long getContentLengthLong() { return declared; }
        @Override public InputStream getInputStream() {
            opened = true;
            return new ByteArrayInputStream(body) {
                @Override public void close() { closed = true; }
            };
        }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() {}
        @Override public String getCipherSuite() { return ""; }
        @Override public Certificate[] getLocalCertificates() { return null; }
        @Override public Certificate[] getServerCertificates() { return null; }
    }
}
