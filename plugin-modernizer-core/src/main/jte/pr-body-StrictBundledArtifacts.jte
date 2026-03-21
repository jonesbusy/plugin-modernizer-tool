@import io.jenkins.tools.pluginmodernizer.core.model.Plugin
@import io.jenkins.tools.pluginmodernizer.core.model.Recipe
@param Plugin plugin
@param Recipe recipe
Hello `${plugin.getName()}` developers! :wave:

This is an automated pull request created by the [Jenkins Plugin Modernizer](https://github.com/jenkins-infra/plugin-modernizer-tool) tool. The tool has applied the following recipes to modernize the plugin:
<details aria-label="Recipe details for ${recipe.getDisplayName()}">
    <summary>${recipe.getDisplayName()}</summary>
    <p><em>${recipe.getName()}</em></p>
    <blockquote>${recipe.getDescription()}</blockquote>
</details>

## Why is this important?

Using strict bundled artifacts by setting `hpi.strictBundledArtifacts` to `true` ensure that not unexpected dependencies are bundled with the plugin. This can help to reduce the size of the plugin and avoid classpath conflicts with other plugins.

If the plugin is bundling dependencies, the property `<hpi.bundledArtifacts></hpi.bundledArtifacts>` need to contains comma-separated list of the dependencies that should be bundled with the plugin. If the plugin is not bundling any dependencies, the property `hpi.bundledArtifacts` can be removed.
