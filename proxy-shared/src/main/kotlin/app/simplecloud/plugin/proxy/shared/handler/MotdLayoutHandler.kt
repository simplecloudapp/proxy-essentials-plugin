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

    companion object {
        private const val RANDOM_MOTD_KEY = "random-motd-layouts"
        private const val CURRENT_LAYOUT_KEY = "current-motd-layout"
        private const val CURRENT_MAINTENANCE_LAYOUT_KEY = "current-maintenance-layout"
        private const val DEFAULT_LAYOUT_NAME = "default-motd"
        private const val DEFAULT_MAINTENANCE_LAYOUT_NAME = "default-maintenance-motd"
    }

    fun loadMotdLayouts() {
        loadedMotdLayouts.clear()
        initializeLayoutDirectory()

        loadLayoutsFromDirectory()

        createDefaultLayoutsIfEmpty()

        initializeCloudProperties()
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
        if (getAllNoneMaintenanceLayouts().isEmpty()) {
            createAndSaveDefaultLayout(DEFAULT_LAYOUT_NAME)
        }

        if (getAllMaintenanceLayouts().isEmpty()) {
            createAndSaveDefaultLayout(DEFAULT_MAINTENANCE_LAYOUT_NAME)
        }
    }

    private fun createAndSaveDefaultLayout(layoutName: String) {
        val defaultLayout = MotdLayoutConfiguration()
        yamlConfig.save("layout/$layoutName", defaultLayout)
        loadedMotdLayouts[layoutName] = defaultLayout
        logger.info("Created and saved default layout: $layoutName")
    }

    private fun initializeCloudProperties() = runBlocking {
        with(cloudControllerHandler) {
            if (getServiceProperties(RANDOM_MOTD_KEY).isEmpty()) {
                setServiceProperties(RANDOM_MOTD_KEY, "false")
                setGroupProperties(RANDOM_MOTD_KEY, "false")

                logger.info("No random MOTD layout key found in service properties, setting to false.")
            }

            if (getServiceProperties(CURRENT_LAYOUT_KEY).isEmpty()) {
                setServiceProperties(CURRENT_LAYOUT_KEY, getAllNoneMaintenanceLayouts().first())
                setGroupProperties(CURRENT_LAYOUT_KEY, getAllNoneMaintenanceLayouts().first())

                logger.info("No current MOTD layout key found in service properties, setting to first layout.")
            }

            if (getServiceProperties(CURRENT_MAINTENANCE_LAYOUT_KEY).isEmpty()) {
                setServiceProperties(CURRENT_MAINTENANCE_LAYOUT_KEY, getAllMaintenanceLayouts().first())
                setGroupProperties(CURRENT_MAINTENANCE_LAYOUT_KEY, getAllMaintenanceLayouts().first())

                logger.info("No current maintenance MOTD layout key found in service properties, setting to first layout.")
            }
        }
    }

    fun getMotdLayout(name: String): MotdLayoutConfiguration =
        loadedMotdLayouts[name] ?: MotdLayoutConfiguration()

    suspend fun getCurrentMotdLayout(): MotdLayoutConfiguration {
        val layouts = if (proxyPlugin.maintenance) getAllMaintenanceLayouts() else getAllNoneMaintenanceLayouts()
        val useRandomLayouts = cloudControllerHandler.getServiceProperties(RANDOM_MOTD_KEY).toBoolean()
        val currentLayoutKey = if (proxyPlugin.maintenance) CURRENT_MAINTENANCE_LAYOUT_KEY else CURRENT_LAYOUT_KEY
        val selectedLayout = cloudControllerHandler.getServiceProperties(currentLayoutKey).ifEmpty {
            logger.warning("No current layout found, using random layout as fallback.")
            layouts.firstOrNull() ?: DEFAULT_LAYOUT_NAME
        }

        return getMotdLayout(if (useRandomLayouts) layouts.random() else selectedLayout)
    }

    fun getAllMaintenanceLayouts(): List<String> = loadedMotdLayouts.keys.filter { it.contains("maintenance") }

    fun getAllNoneMaintenanceLayouts(): List<String> = loadedMotdLayouts.keys.filter { !it.contains("maintenance") }
}
