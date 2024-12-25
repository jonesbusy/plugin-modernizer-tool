package io.jenkins.tools.pluginmodernizer.core.recipes;

import io.jenkins.tools.pluginmodernizer.core.extractor.PluginMetadata;
import io.jenkins.tools.pluginmodernizer.core.extractor.PomVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.AddPropertyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.openrewrite.Tree.randomId;

public class MigrateToJenkinsBaseLineProperty extends Recipe {

    public static final Logger LOG = LoggerFactory.getLogger(MigrateToJenkinsBaseLineProperty.class);

    @Override
    public String getDisplayName() {
        return "Migrate to Jenkins baseline property";
    }

    @Override
    public String getDescription() {
        return "Migrate to Jenkins baseline property.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateToJenkinsBaseLinePropertyVisitor();
    }

    /**
     * Visitor to migrate to Jenkins baseline property.
     */
    private static class MigrateToJenkinsBaseLinePropertyVisitor extends MavenIsoVisitor<ExecutionContext> {

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
            tag = super.visitTag(tag, executionContext);
            if (tag.getName().equals("jenkins.version") && tag.getContent() != null) {
                doAfterVisit(new AddCommentVisitor<>("Jenkins baseline version"));
            }
            return tag;
        }

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            Xml.Document d = super.visitDocument(document, ctx);

            PluginMetadata pluginMetadata = new PomVisitor().reduce(document, new PluginMetadata());

            // Keep only major and minor and ignore patch version
            String jenkinsBaseline = pluginMetadata.getJenkinsVersion();
            String jenkinsPatch = null;
            if (pluginMetadata.getJenkinsVersion().matches("\\d+\\.\\d+\\.\\d+")) {
                jenkinsBaseline = jenkinsBaseline.substring(0, jenkinsBaseline.lastIndexOf('.'));
                jenkinsPatch = pluginMetadata
                        .getJenkinsVersion()
                        .substring(pluginMetadata.getJenkinsVersion().lastIndexOf('.') + 1);
            }
            LOG.debug("Jenkins baseline version is {}", jenkinsBaseline);
            LOG.debug("Jenkins patch version is {}", jenkinsPatch);

            if (pluginMetadata.getBomArtifactId() == null || "bom-weekly".equals(pluginMetadata.getBomArtifactId())) {
                LOG.debug("Project is using Jenkins weekly bom or not bom, skipping");
                return d;
            }

            performUpdate(document, jenkinsBaseline, jenkinsPatch);
            return document;
        }

        private void performUpdate(Xml.Document document, String jenkinsBaseline, String jenkinsPatch) {

            // Add or changes properties
            doAfterVisit(new AddPropertyVisitor("jenkins.baseline", jenkinsBaseline, false));
            if (jenkinsPatch != null) {
                doAfterVisit(new AddPropertyVisitor("jenkins.version", "${jenkins.baseline}." + jenkinsPatch, false));
            } else {
                doAfterVisit(new AddPropertyVisitor("jenkins.version", "${jenkins.baseline}", false));
            }

            // Change the bom artifact ID
            Xml.Tag artifactIdTag = document.getRoot()
                    .getChild("dependencyManagement")
                    .flatMap(dm -> dm.getChild("dependencies"))
                    .flatMap(deps -> deps.getChild("dependency"))
                    .filter(dep -> dep.getChildValue("groupId")
                            .map("io.jenkins.tools.bom"::equals)
                            .orElse(false))
                    .flatMap(dep -> dep.getChild("artifactId"))
                    .orElseThrow();

            doAfterVisit(new ChangeTagValueVisitor<>(artifactIdTag, "bom-${jenkins.baseline}.x"));
        }
    }

    public static class AddCommentVisitor<P> extends XmlVisitor<P> {
        private final String comment;

        public AddCommentVisitor(String comment) {
            this.comment = comment;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, P p) {
            if (tag.getName().equals("jenkins.version") && tag.getContent() != null) {
                List<Content> contents = new ArrayList<>(tag.getContent());
                boolean containsComment = contents.stream()
                        .anyMatch(c -> c instanceof Xml.Comment &&
                                comment.equals(((Xml.Comment) c).getText()));
                if (!containsComment) {
                    int insertPos = 0;
                    Xml.Comment customComment = new Xml.Comment(randomId(),
                            contents.get(insertPos).getPrefix(),
                            Markers.EMPTY,
                            comment);
                    contents.add(insertPos, customComment);
                    tag = tag.withContent(contents);
                }
            }
            return tag;
        }
    }

}
