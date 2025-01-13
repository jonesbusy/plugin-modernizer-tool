package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import io.jenkins.tools.pluginmodernizer.core.extractor.JenkinsfileVisitor;
import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.jenkins.UpgradeVersionProperty;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A recipe to upgrade the Jenkins version in the pom.xml.
 * Take care of updating the bom or adding the bom if it's not present.
 * Not changing anything if the version is already higher than the minimum version.
 */
public class UpgradeJenkinsVersion extends ScanningRecipe<UpgradeJenkinsVersion.PlatformConfigAccumulator> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeJenkinsVersion.class);

    /**
     * The minimum version.
     */
    @Option(displayName = "Version", description = "The version.", example = "2.452.4")
    String minimumVersion;

    /**
     * Constructor.
     * @param minimumVersion The minimum version.
     */
    public UpgradeJenkinsVersion(String minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade Jenkins version";
    }

    @Override
    public String getDescription() {
        return "Upgrade Jenkins version.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(PlatformConfigAccumulator acc) {
        return new MavenIsoVisitor<>() {

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

                Set<JDK> jenkinsfileJdks = acc.getJdks();
                List<JDK> jenkinsJdks = JDK.get(minimumVersion);

                // All jenkinsfile JDK must be supported by the new Jenkins version
                if (jenkinsfileJdks.stream().anyMatch(jdk -> !jenkinsJdks.contains(jdk))) {
                    LOG.warn(
                            "Jenkinsfile JDKs are not supported by the new Jenkins version. Will not change the version.");
                    return document;
                }

                // Return another tree with jenkins version updated
                document = (Xml.Document) new UpgradeVersionProperty("jenkins.version", minimumVersion)
                        .getVisitor()
                        .visitNonNull(document, ctx);
                return (Xml.Document) new UpdateBom().getVisitor().visitNonNull(document, ctx);
            }
        };
    }

    @Override
    public PlatformConfigAccumulator getInitialValue(ExecutionContext ctx) {
        return new PlatformConfigAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(PlatformConfigAccumulator acc) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                tree = super.visit(tree, ctx);
                if (tree instanceof SourceFile
                        && ArchetypeCommonFile.JENKINSFILE.same(((SourceFile) tree).getSourcePath())) {
                    acc.setPlatform(new JenkinsfileVisitor()
                            .reduce(tree, new PluginMetadata())
                            .getJdks());
                }
                return tree;
            }
        };
    }

    public static class PlatformConfigAccumulator {
        private Set<JDK> jdks = new HashSet<>();

        public void setPlatform(Set<JDK> jdks) {
            this.jdks = jdks;
        }

        public Set<JDK> getJdks() {
            return jdks;
        }
    }
}
