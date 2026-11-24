package com.example.myapplication.data.image;

import java.net.URI;
import java.net.URISyntaxException;

/** Resolves versioned catalog assets within one configured HTTPS origin. */
public final class HostingImageUrlResolver {
    public static final URI PRODUCTION_ORIGIN = URI.create("https://bep-nha-2ca75.web.app/");

    private final URI origin;

    public HostingImageUrlResolver(URI origin) {
        if (!"https".equalsIgnoreCase(origin.getScheme()) || origin.getHost() == null
                || origin.getRawUserInfo() != null || origin.getRawQuery() != null
                || origin.getRawFragment() != null
                || !("".equals(origin.getRawPath()) || "/".equals(origin.getRawPath()))) {
            throw new IllegalArgumentException("image origin must be an HTTPS origin");
        }
        this.origin = origin.resolve("/");
    }

    public URI resolve(String imagePath) {
        return origin.resolve(relativePath(imagePath));
    }

    boolean contains(URI uri) {
        String path = uri.getRawPath();
        if (path == null || !path.startsWith("/") || uri.getRawQuery() != null
                || uri.getRawFragment() != null || !origin.equals(uri.resolve("/"))) return false;
        try {
            return resolve(path.substring(1)).equals(uri);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    public static URI relativePath(String imagePath) {
        if (imagePath == null) throw new IllegalArgumentException("imagePath is required");
        try {
            URI relative = new URI(imagePath);
            String decoded = relative.getPath();
            if (relative.isAbsolute() || relative.getRawAuthority() != null
                    || relative.getRawQuery() != null || relative.getRawFragment() != null || decoded == null
                    || decoded.indexOf('\\') >= 0 || decoded.indexOf('%') >= 0
                    || decoded.codePoints().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("imagePath must be a relative asset path");
            }
            String[] segments = decoded.split("/", -1);
            if (segments.length != relative.getRawPath().split("/", -1).length) {
                throw new IllegalArgumentException("imagePath contains an encoded slash");
            }
            for (String segment : segments) {
                if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                    throw new IllegalArgumentException("imagePath has an invalid segment");
                }
            }
            return relative;
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("invalid imagePath", error);
        }
    }
}
