package io.jenkins.tools.pluginmodernizer.core.github;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ServerKeyVerifier} implementation that validates the remote SSH host key
 * against GitHub's published keys from the /meta API endpoint.
 *
 * @see <a href="https://api.github.com/meta">GitHub Meta API</a>
 */
public class GitHubServerKeyVerifier implements ServerKeyVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubServerKeyVerifier.class);

    static final String GITHUB_META_URL = "https://api.github.com/meta";

    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;

    private volatile List<PublicKey> cachedKeys;

    public GitHubServerKeyVerifier() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    GitHubServerKeyVerifier(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public boolean verifyServerKey(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        List<PublicKey> knownKeys = getKnownKeys();
        if (knownKeys.isEmpty()) {
            LOG.warn("No GitHub SSH host keys available; rejecting connection to {}", remoteAddress);
            return false;
        }
        for (PublicKey known : knownKeys) {
            if (known.equals(serverKey)) {
                return true;
            }
        }
        LOG.warn("SSH host key for {} does not match any known GitHub host key; rejecting connection", remoteAddress);
        return false;
    }

    private List<PublicKey> getKnownKeys() {
        if (cachedKeys != null) {
            return cachedKeys;
        }
        synchronized (this) {
            if (cachedKeys != null) {
                return cachedKeys;
            }
            cachedKeys = fetchGitHubKeys();
        }
        return cachedKeys;
    }

    private List<PublicKey> fetchGitHubKeys() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_META_URL))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warn("GitHub meta API returned HTTP {}; cannot validate SSH host keys", response.statusCode());
                return Collections.emptyList();
            }
            return parseKeys(response.body());
        } catch (Exception e) {
            LOG.warn("Failed to fetch GitHub SSH host keys from {}: {}", GITHUB_META_URL, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PublicKey> parseKeys(String json) {
        List<PublicKey> keys = new ArrayList<>();
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            JsonArray sshKeys = root.getAsJsonArray("ssh_keys");
            if (sshKeys == null) {
                LOG.warn("GitHub meta API response does not contain 'ssh_keys' field");
                return Collections.emptyList();
            }
            for (int i = 0; i < sshKeys.size(); i++) {
                String keyLine = sshKeys.get(i).getAsString().trim();
                if (keyLine.isEmpty()) {
                    continue;
                }
                try {
                    PublicKeyEntry entry = PublicKeyEntry.parsePublicKeyEntry(keyLine);
                    PublicKey key = entry.resolvePublicKey(null, null, null);
                    if (key != null) {
                        keys.add(key);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to parse GitHub SSH host key entry '{}': {}", keyLine, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse GitHub meta API JSON response: {}", e.getMessage());
        }
        return keys;
    }
}
