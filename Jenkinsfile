// While this isn't a plugin, it is much simpler to reuse the pipeline code for CI
// allowing easy windows / linux testing and producing incrementals
// the only feature that buildPlugin has that relates to plugins is allowing you to test against multiple jenkins versions
buildPlugin(
    useContainerAgent: false,
    configurations: [
        [platform: 'linux', jdk: 21],
        [platform: 'windows', jdk: 17],
 ])
