package com.example.myapplication.data.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

/** Fetches the public RTDB catalog without Firebase credentials or an SDK. */
public final class FirebaseCatalogRemoteDataSource implements CatalogRemoteDataSource {
    public static final URI PRODUCTION_ENDPOINT = URI.create(
            "https://bep-nha-2ca75-default-rtdb.asia-southeast1.firebasedatabase.app/catalog.json");

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 15_000;
    private static final int MAX_RESPONSE_BYTES = 2 * 1024 * 1024;

    private final URI endpoint;
    private final ConnectionFactory connectionFactory;

    public FirebaseCatalogRemoteDataSource() {
        this(PRODUCTION_ENDPOINT);
    }

    public FirebaseCatalogRemoteDataSource(URI endpoint) {
        this(endpoint, url -> (HttpsURLConnection) url.openConnection());
    }

    FirebaseCatalogRemoteDataSource(URI endpoint, ConnectionFactory connectionFactory) {
        Objects.requireNonNull(endpoint, "endpoint");
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null
                || endpoint.getRawFragment() != null) {
            throw new IllegalArgumentException("catalog endpoint must be a public HTTPS URL without credentials");
        }
        this.endpoint = endpoint;
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public String fetchSnapshot() throws IOException {
        HttpsURLConnection connection = connectionFactory.open(endpoint.toURL());
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != HttpsURLConnection.HTTP_OK) throw new HttpStatusException(status);
            if (connection.getContentLength() > MAX_RESPONSE_BYTES) throw responseTooLarge();

            try (InputStream stream = connection.getInputStream()) {
                String json = readJson(stream);
                if (json.isEmpty() || "null".equals(json)) {
                    throw new IOException("RTDB has no published catalog snapshot");
                }
                return json;
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String readJson(InputStream stream) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            if (count > MAX_RESPONSE_BYTES - body.size()) throw responseTooLarge();
            body.write(buffer, 0, count);
        }
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(body.toByteArray())).toString().trim();
    }

    private static IOException responseTooLarge() {
        return new IOException("catalog response exceeds " + MAX_RESPONSE_BYTES + " bytes");
    }

    interface ConnectionFactory {
        HttpsURLConnection open(URL endpoint) throws IOException;
    }

    public static final class HttpStatusException extends IOException {
        public final int statusCode;

        private HttpStatusException(int statusCode) {
            super("RTDB catalog request failed with HTTP " + statusCode);
            this.statusCode = statusCode;
        }
    }
}
