package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.api.shared.repository.YamlDirectoryRepository
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.motd.MotdLayoutConfiguration
import java.nio.file.Path
import java.util.logging.Logger

class MotdLayoutHandler(
    private val directory: Path,
    private val proxyPlugin: ProxyPlugin
): YamlDirectoryRepository<MotdLayoutConfiguration>(
    directory,
    MotdLayoutConfiguration::class.java,
) {

    private val logger = Logger.getLogger(MotdLayoutHandler::class.java.name)

    fun loadMotdLayouts() {
        load()
        createDefaultLayoutsIfEmpty()
    }

    private fun createDefaultLayoutsIfEmpty() {
        if (entities.isEmpty()) {
            this.proxyPlugin.joinStateConfiguration.get().joinStates.forEach {
                createAndSaveDefaultLayout(it.motdLayoutByProxy)
            }
        }
    }

    private fun createAndSaveDefaultLayout(layoutName: String) {
        val defaultLayout = MotdLayoutConfiguration()
        save("${layoutName}.yml", defaultLayout)
        logger.info("Created and saved default layout: $layoutName")
    }

    private fun getMotdLayout(name: String): MotdLayoutConfiguration =
        entities[directory.resolve("${name}.yml").toFile()] ?: MotdLayoutConfiguration()


    fun getCurrentMotdLayout(): MotdLayoutConfiguration {
        return getMotdLayout(this.proxyPlugin.joinStateHandler.localState)
    }

}
