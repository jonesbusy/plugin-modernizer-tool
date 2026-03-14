package io.jenkins.tools.pluginmodernizer.core.github;

import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ServerKeyVerifier} that validates the remote SSH host key against GitHub's
 * published keys fetched via {@link GitHub#getMeta()}.
 *
 * @see <a href="https://api.github.com/meta">GitHub Meta API</a>
 */
public class GitHubServerKeyVerifier implements ServerKeyVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubServerKeyVerifier.class);

    private final GitHub github;

    private volatile List<PublicKey> cachedKeys;

    public GitHubServerKeyVerifier(GitHub github) {
        this.github = github;
    }

    @Override
    public boolean verifyServerKey(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        return isValidServerKey(remoteAddress, serverKey);
    }

    private boolean isValidServerKey(SocketAddress remoteAddress, PublicKey serverKey) {
        if (getKnownKeys().stream().noneMatch(serverKey::equals)) {
            LOG.warn(
                    "SSH host key for {} does not match any known GitHub host key; rejecting connection",
                    remoteAddress);
            return false;
        }
        return true;
    }

    private List<PublicKey> getKnownKeys() {
        if (cachedKeys != null) {
            return cachedKeys;
        }
        synchronized (this) {
            if (cachedKeys != null) {
                return cachedKeys;
            }
            List<PublicKey> keys = fetchGitHubKeys();
            if (!keys.isEmpty()) {
                cachedKeys = keys;
            }
            return keys;
        }
    }

    private List<PublicKey> fetchGitHubKeys() {
        try {
            var sshKeys = github.getMeta().getSshKeys();
            if (sshKeys == null || sshKeys.isEmpty()) {
                LOG.warn("GitHub meta returned no SSH host keys");
                return List.of();
            }
            var keys = parseKeys(sshKeys);
            if (keys.isEmpty()) {
                LOG.warn("No valid SSH host keys could be parsed from GitHub meta");
            }
            return keys;
        } catch (Exception e) {
            LOG.warn("Failed to fetch GitHub SSH host keys: {}", e.getMessage());
            return List.of();
        }
    }

    private PublicKey parseKey(String keyLine) {
        try {
            var entry = PublicKeyEntry.parsePublicKeyEntry(keyLine);
            return entry.resolvePublicKey(null, null, null);
        } catch (Exception e) {
            LOG.warn("Failed to parse SSH host key entry '{}': {}", keyLine, e.getMessage());
            return null;
        }
    }

    private List<PublicKey> parseKeys(List<String> rawKeys) {
        return rawKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(this::parseKey)
                .filter(Objects::nonNull)
                .toList();
    }
}
