package io.jenkins.tools.pluginmodernizer.core.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
public class GitHubServerKeyVerifierTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse httpResponse;

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
        String json = "{\"ssh_keys\":[\"" + knownKeyLine + "\"]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(httpClient);
        assertTrue(verifier.verifyServerKey(null, GITHUB_ADDRESS, knownPublicKey));
    }

    @Test
    void shouldRejectUnknownKey() throws Exception {
        String json = "{\"ssh_keys\":[\"" + knownKeyLine + "\"]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(httpClient);
        assertFalse(verifier.verifyServerKey(null, GITHUB_ADDRESS, unknownPublicKey));
    }

    @Test
    void shouldRejectWhenApiUnavailable() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("Connection refused"));

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(httpClient);
        assertFalse(verifier.verifyServerKey(null, GITHUB_ADDRESS, knownPublicKey));
    }

    @Test
    void shouldRejectOnNonOkHttpResponse() throws Exception {
        when(httpResponse.statusCode()).thenReturn(503);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        GitHubServerKeyVerifier verifier = new GitHubServerKeyVerifier(httpClient);
        assertFalse(verifier.verifyServerKey(null, GITHUB_ADDRESS, knownPublicKey));
    }
}
