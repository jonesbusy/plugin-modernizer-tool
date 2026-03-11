package io.jenkins.tools.pluginmodernizer.core.github;

import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            List<PublicKey> keys = fetchGitHubKeys();
            if (!keys.isEmpty()) {
                cachedKeys = keys;
            }
            return keys;
        }
    }

    private List<PublicKey> fetchGitHubKeys() {
        try {
            List<String> sshKeys = github.getMeta().getSshKeys();
            if (sshKeys == null || sshKeys.isEmpty()) {
                LOG.warn("GitHub meta returned no SSH host keys");
                return Collections.emptyList();
            }
            List<PublicKey> keys = parseKeys(sshKeys);
            if (keys.isEmpty()) {
                LOG.warn("No valid SSH host keys could be parsed from GitHub meta");
                return Collections.emptyList();
            }
            return keys;
        } catch (Exception e) {
            LOG.warn("Failed to fetch GitHub SSH host keys: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PublicKey> parseKeys(List<String> rawKeys) {
        List<PublicKey> keys = new ArrayList<>();
        for (String keyLine : rawKeys) {
            if (keyLine == null || keyLine.isBlank()) {
                continue;
            }
            try {
                PublicKeyEntry entry = PublicKeyEntry.parsePublicKeyEntry(keyLine.trim());
                PublicKey key = entry.resolvePublicKey(null, null, null);
                if (key != null) {
                    keys.add(key);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse SSH host key entry '{}': {}", keyLine, e.getMessage());
            }
        }
        return keys;
    }
}
