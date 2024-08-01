package io.jenkins.tools.pluginmodernizer.core.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.JenkinsPluginInfo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS", justification = "safe because versions from pom.xml")
public class PluginModernizer {

    private static final Logger LOG = LoggerFactory.getLogger(PluginModernizer.class);

    private final Config config;

    private final MavenInvoker mavenInvoker;

    private final GHService ghService;

    private final CacheManager cacheManager;

    private final ExecutorService executorService;

    /**
     * Create a new PluginModernizer
     * @param config The configuration to use
     */
    public PluginModernizer(Config config) {
        this.config = config;
        this.mavenInvoker = new MavenInvoker(config);
        this.ghService = new GHService(config);
        this.cacheManager = new CacheManager(config.getCachePath());
        this.executorService = Executors.newFixedThreadPool(config.getThreads());
    }

    /**
     * Entry point to start the plugin modernization process
     */
    public void start() {
        LOG.info("Plugins: {}", config.getPluginNames());
        LOG.info("Recipes: {}", config.getRecipes());
        LOG.info("GitHub owner: {}", config.getGithubOwner());
        LOG.info("Update Center Url: {}", config.getJenkinsUpdateCenter());
        LOG.debug("Cache Path: {}", config.getCachePath());
        LOG.debug("Dry Run: {}", config.isDryRun());
        LOG.debug("Skip Push: {}", config.isSkipPush());
        LOG.debug("Skip Pull Request: {}", config.isSkipPullRequest());
        LOG.debug("Maven rewrite plugin version: {}", Settings.MAVEN_REWRITE_PLUGIN_VERSION);
        cacheManager.createCache();
        config.getPluginNames().stream().map(Plugin::build).forEach(plugin -> {
            CompletableFuture.supplyAsync(() -> process(plugin), executorService);
        });
        executorService.shutdown();
    }

    /**
     * Process a plugin
     * @param plugin The plugin to process
     */
    private CompletableFuture<Boolean> process(Plugin plugin) {
        try {

            // Determine repo name
            plugin.withRepositoryName(JenkinsPluginInfo.extractRepoName(
                    plugin.getName(), config.getCachePath(), config.getJenkinsUpdateCenter()));

            plugin.fork(ghService);
            plugin.fetch(ghService);
            // plugin.clean(mavenInvoker);
            plugin.checkoutBranch(ghService);
            // plugin.runOpenRewrite(mavenInvoker); // TODO: Maybe lock ? Maven cache corruption if parallel builds?
            plugin.commit(ghService);
            plugin.push(ghService);
            plugin.openPullRequest(ghService);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            LOG.error("Failed to process plugin: {}", plugin, e);
            plugin.addError(e);
            return CompletableFuture.completedFuture(false);
        }
    }
}
