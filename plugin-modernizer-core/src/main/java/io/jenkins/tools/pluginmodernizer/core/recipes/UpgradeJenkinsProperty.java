package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.config.RecipesConsts;
import io.jenkins.tools.pluginmodernizer.core.visitors.UpdateBomVersionVisitor;
import java.util.Collections;
import java.util.Optional;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.ChangePropertyValue;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the version property unless it is already greater than minimumVersion
 */
public class UpgradeJenkinsProperty extends Recipe {

    /**
     * Logger for the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeJenkinsProperty.class);

    @Option(
            displayName = "Minimum version",
            description = "Value to apply to the matching property if < this.",
            example = "2.452.4")
    String minimumVersion;

    public UpgradeJenkinsProperty(String minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade property's value to version";
    }

    @Override
    public String getDescription() {
        return "If the current value is < given version, upgrade it.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator =
                Semver.validate(minimumVersion, null).getValue();
        assert versionComparator != null;
        return Preconditions.check(
                new MavenVisitor<ExecutionContext>() {
                    @Override
                    public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                        String value =
                                getResolutionResult().getPom().getProperties().get("jenkins.version");
                        if (value == null) {
                            return document;
                        }
                        if (value.contains("${jenkins.baseline}")) {
                            value = getResolutionResult()
                                    .getPom()
                                    .getProperties()
                                    .get("jenkins.baseline");
                        }
                        Optional<String> upgrade =
                                versionComparator.upgrade(value, Collections.singleton(minimumVersion));
                        if (!upgrade.isPresent()) {
                            return document;
                        }
                        return SearchResult.found(document);
                    }
                },
                new MavenVisitor<ExecutionContext>() {

                    public String bomNameForJenkinsVersion(String version) {
                        if (RecipesConsts.LTS_PATTERN.test(version)
                                || RecipesConsts.LTS_BASELINE_PATTERN.test(version)) {
                            if (version.startsWith("${jenkins.baseline}")) {
                                return "bom-${jenkins.baseline}.x";
                            } else {
                                int lastIndex = version.lastIndexOf(".");
                                String prefix = version.substring(0, lastIndex);
                                return "bom-" + prefix + ".x";
                            }
                        }
                        return "bom-weekly";
                    }

                    private void changeBomArtifactId(Xml.Document document) {

                        // Get the bom tag
                        Xml.Tag bom = UpdateBomVersionVisitor.getBomTag(document);
                        if (bom == null) {
                            return;
                        }

                        Xml.Tag artifactIdTag = bom.getChild("artifactId").orElseThrow();
                        Xml.Tag version = bom.getChild("version").orElseThrow();

                        LOG.debug("Artifact ID is {}", artifactIdTag.getValue().get());
                        LOG.debug("Version is {}", version.getValue().get());

                        // Change the artifact and perform upgrade
                        if (!artifactIdTag.getValue().get().equals("bom-${jenkins.baseline}.x")) {
                            doAfterVisit(new ChangeTagValueVisitor<>(
                                    artifactIdTag, bomNameForJenkinsVersion(minimumVersion)));
                        }
                    }

                    @Override
                    public Xml visitTag(Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = (Tag) super.visitTag(tag, ctx);
                        if (!isPropertyTag()) {
                            return t;
                        }
                        // Change the baseline
                        if ("jenkins.baseline".equals(t.getName())) {
                            if (minimumVersion.matches("\\d+\\.\\d+")) {
                                doAfterVisit(new ChangePropertyValue("jenkins.version", minimumVersion, false, false)
                                        .getVisitor());
                                changeBomArtifactId(getCursor().firstEnclosing(Xml.Document.class));
                                return t;
                            } else {
                                String minimumBaseline = minimumVersion.substring(0, minimumVersion.lastIndexOf('.'));
                                doAfterVisit(new ChangePropertyValue("jenkins.version", minimumVersion, false, false)
                                        .getVisitor());
                                changeBomArtifactId(getCursor().firstEnclosing(Xml.Document.class));
                                return t;
                            }
                        }
                        if (!t.getName().equals("jenkins.version")) {
                            return t;
                        }
                        if (!t.getValue().isPresent()) {
                            return t;
                        }
                        String newValue = minimumVersion;
                        if (t.getValue().get().contains("${jenkins.baseline}")) {
                            if (minimumVersion.matches("\\d+\\.\\d+")) {
                                newValue = "${jenkins.baseline}";
                            } else {
                                newValue = "${jenkins.baseline}."
                                        + minimumVersion.substring(minimumVersion.lastIndexOf('.') + 1);
                            }
                        }
                        doAfterVisit(
                                new ChangePropertyValue("jenkins.version", minimumVersion, false, false).getVisitor());
                        changeBomArtifactId(getCursor().firstEnclosing(Xml.Document.class));
                        return t;
                    }
                });
    }
}
