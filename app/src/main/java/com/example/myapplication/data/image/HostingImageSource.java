package com.example.myapplication.data.image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/** HTTPS transport for images resolved by the configured Hosting resolver. */
public final class HostingImageSource implements RecipeImageLoader.Source {
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private final HostingImageUrlResolver resolver;
    private final int maxBytes;
    private final Connections connections;

    public HostingImageSource(HostingImageUrlResolver resolver, int maxBytes) {
        this(resolver, maxBytes, url -> (HttpsURLConnection) url.openConnection());
    }

    HostingImageSource(HostingImageUrlResolver resolver, int maxBytes, Connections connections) {
        this.resolver = resolver;
        this.maxBytes = maxBytes;
        this.connections = connections;
    }

    @Override
    public byte[] fetch(URI uri) throws IOException {
        if (!resolver.contains(uri)) throw new IOException("image requires the configured HTTPS origin");
        HttpsURLConnection connection = connections.open(uri.toURL());
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            int status = connection.getResponseCode();
            if (status != HttpsURLConnection.HTTP_OK) throw new IOException("image response HTTP " + status);
            String contentType = connection.getContentType();
            String mime = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (!"image/jpeg".equals(mime) && !"image/png".equals(mime) && !"image/webp".equals(mime)) {
                throw new IOException("unsupported image content type");
            }
            long declaredSize = connection.getContentLengthLong();
            if (declaredSize > maxBytes) throw new IOException("image response too large");
            int expected = declaredSize > 0 ? (int) declaredSize : Math.min(8192, maxBytes);
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream(expected)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (count > maxBytes - output.size()) throw new IOException("image response too large");
                    output.write(buffer, 0, count);
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    interface Connections { HttpsURLConnection open(URL url) throws IOException; }
}
