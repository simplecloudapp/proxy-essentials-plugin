package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.motd.MotdLayoutConfiguration
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.logging.Logger

class MotdLayoutHandler(
    private val yamlConfig: YamlConfig,
    private val proxyPlugin: ProxyPlugin
) {

    private val cloudControllerHandler = proxyPlugin.cloudControllerHandler
    private val loadedMotdLayouts: MutableMap<String, MotdLayoutConfiguration> = mutableMapOf()
    private val logger = Logger.getLogger(MotdLayoutHandler::class.java.name)

    fun loadMotdLayouts() {
        loadedMotdLayouts.clear()
        initializeLayoutDirectory()
        loadLayoutsFromDirectory()
        createDefaultLayoutsIfEmpty()
    }

    private fun initializeLayoutDirectory() {
        val directory = File(yamlConfig.dirPath + "/layout")
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    private fun loadLayoutsFromDirectory() {
        File(yamlConfig.dirPath + "/layout").listFiles()?.forEach { file ->
            yamlConfig.load<MotdLayoutConfiguration>("layout/${file.nameWithoutExtension}")?.let { layout ->
                loadedMotdLayouts[file.nameWithoutExtension] = layout
                logger.info("Loaded MOTD layout: ${file.nameWithoutExtension}")
            }
        }
    }

    private fun createDefaultLayoutsIfEmpty() {
        if (loadedMotdLayouts.isEmpty()) {
            this.proxyPlugin.joinStateConfiguration.joinStates.forEach {
                createAndSaveDefaultLayout(it.motdLayoutByProxy)
            }
        }
    }

    private fun createAndSaveDefaultLayout(layoutName: String) {
        val defaultLayout = MotdLayoutConfiguration()
        yamlConfig.save("layout/$layoutName", defaultLayout)
        loadedMotdLayouts[layoutName] = defaultLayout
        logger.info("Created and saved default layout: $layoutName")
    }

    private fun getMotdLayout(name: String): MotdLayoutConfiguration =
        loadedMotdLayouts[name] ?: MotdLayoutConfiguration()

    fun getCurrentMotdLayout(): MotdLayoutConfiguration {
        return getMotdLayout(this.proxyPlugin.joinStateHandler.localState)
    }
}
