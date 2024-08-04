package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.config.GeneralConfig
import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.motd.MotdLayoutConfiguration
import java.io.File

class MotdLayoutHandler(
    val yamlConfig: YamlConfig,
    val generalConfig: GeneralConfig
) {

    private val loadedMotdLayouts: MutableMap<String, MotdLayoutConfiguration> = mutableMapOf()

    fun loadMotdLayouts() {
        loadedMotdLayouts.clear()
        val directory = File(yamlConfig.dirPath + "/layout")

        if (!directory.exists()) {
            directory.mkdirs()
        }

        directory.listFiles()?.forEach {
            val name = it.nameWithoutExtension
            val motdLayout = yamlConfig.load<MotdLayoutConfiguration>("layout/$name")
            if (motdLayout != null) {
                loadedMotdLayouts[name] = motdLayout
                println("Loaded motd layout: $name")
            }
        }

        if (loadedMotdLayouts.isEmpty()) {
            println("No motd layouts found, creating default motd layout")
            val defaultMotdLayout = MotdLayoutConfiguration()
            yamlConfig.save("layout/default-motd", defaultMotdLayout)
            loadedMotdLayouts["default-motd"] = defaultMotdLayout
        }
    }

    fun getMotdLayout(name: String): MotdLayoutConfiguration {
        return loadedMotdLayouts[name] ?: MotdLayoutConfiguration()
    }

    fun getCurrentMotdLayout(): MotdLayoutConfiguration {
        return getMotdLayout(generalConfig.currentLayout)
    }

    fun getLoadedMotdLayouts(): List<String> {
        return loadedMotdLayouts.keys.toList()
    }

    fun setMotdLayout(name: String) {
        generalConfig.currentLayout = name
        yamlConfig.save("general", generalConfig)
    }
}