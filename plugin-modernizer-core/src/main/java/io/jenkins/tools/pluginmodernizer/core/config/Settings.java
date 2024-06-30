package io.jenkins.tools.pluginmodernizer.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {

    private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

    public static final Path DEFAULT_CACHE_PATH;

    public static final Path DEFAULT_MAVEN_HOME;

    public static final String MAVEN_REWRITE_PLUGIN_VERSION;

    public static final String RECIPE_DATA_YAML_PATH = "recipe_data.yaml";

    static {
        String cacheBaseDir = System.getProperty("user.home");
        if (cacheBaseDir == null) {
            cacheBaseDir = System.getProperty("user.dir");
        }

        String cacheDirFromEnv = System.getenv("CACHE_DIR");
        if (cacheDirFromEnv == null) {
            DEFAULT_CACHE_PATH = Paths.get(cacheBaseDir, ".cache", "jenkins-plugin-modernizer-cli");
        } else {
            DEFAULT_CACHE_PATH = Paths.get(cacheDirFromEnv);
        }
        DEFAULT_MAVEN_HOME = getDefaultMavenHome();
        MAVEN_REWRITE_PLUGIN_VERSION = getRewritePluginVersion();
    }

    private static Path getDefaultMavenHome() {
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome == null) {
            mavenHome = System.getenv("M2_HOME");
        }
        if (mavenHome == null) {
            return null;
        }
        return Path.of(mavenHome);
    }

    private static String getRewritePluginVersion() {
        return readProperty("openrewrite.maven.plugin.version", "versions.properties");
    }

    private static String readProperty(String key, String resource) {
        Properties properties = new Properties();
        try (InputStream input = Settings.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                LOG.error("Error reading {} from settings", resource);
                throw new IOException(String.format("Unable to to load `%s`", resource));
            }
            properties.load(input);
        }
        catch (IOException e) {
            LOG.error("Error reading from settings", e);
            return null;
        }

        String version = properties.getProperty("openrewrite.maven.plugin.version");
        if (version == null || version.isEmpty()) {
            LOG.error(String.format("Unable to read `%s` from `%s`", key, resource));
            return null;
        }

        return version.trim();
    }
}
