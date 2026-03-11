package io.jenkins.tools.pluginmodernizer.core.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.List;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.kohsuke.github.GHMeta;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class GitHubServerKeyVerifierTest {

    @Mock
    private GitHub github;

    @Mock
    private GHMeta meta;

    private PublicKey knownPublicKey;
    private PublicKey unknownPublicKey;
    private String knownKeyLine;

    private static final InetSocketAddress GITHUB_ADDRESS = new InetSocketAddress("github.com", 22);

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);

        KeyPair knownPair = gen.generateKeyPair();
        knownPublicKey = knownPair.getPublic();

        KeyPair unknownPair = gen.generateKeyPair();
        unknownPublicKey = unknownPair.getPublic();

        knownKeyLine = PublicKeyEntry.appendPublicKeyEntry(new StringBuilder(), knownPublicKey)
                .toString();
    }

    @Test
    void shouldAcceptKnownGitHubKey() throws Exception {
        when(github.getMeta()).thenReturn(meta);
        when(meta.getSshKeys()).thenReturn(List.of(knownKeyLine));

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(github);
        assertTrue(verifier.verifyServerKey(null, GITHUB_ADDRESS, knownPublicKey));
    }

    @Test
    void shouldRejectUnknownKey() throws Exception {
        when(github.getMeta()).thenReturn(meta);
        when(meta.getSshKeys()).thenReturn(List.of(knownKeyLine));

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(github);
        assertFalse(verifier.verifyServerKey(null, GITHUB_ADDRESS, unknownPublicKey));
    }

    @Test
    void shouldRejectWhenApiUnavailable() throws Exception {
        doThrow(new IOException("Connection refused")).when(github).getMeta();

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(github);
        assertFalse(verifier.verifyServerKey(null, GITHUB_ADDRESS, knownPublicKey));
    }

    @Test
    void shouldRejectOnEmptyKeyList() throws Exception {
        when(github.getMeta()).thenReturn(meta);
        when(meta.getSshKeys()).thenReturn(List.of());

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(github);
        assertFalse(verifier.verifyServerKey(null, GITHUB_ADDRESS, knownPublicKey));
    }
}
