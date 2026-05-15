package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.proxy.shared.config.message.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.state.WhitelistConfig
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

object OldConfigMigrator {

    private const val CURRENT_CONFIG_VERSION = "2"
    private const val CURRENT_LAYOUT_VERSION = "2"

    fun migrate(dataDirectory: Path) {
        Files.createDirectories(dataDirectory)

        migrateMainConfig(dataDirectory)
        migrateMessages(dataDirectory.resolve("messages.yml"))
        migratePlaceholder(dataDirectory.resolve("placeholder.yml"))
        migrateLayouts(dataDirectory.resolve("layout"))
    }

    private fun migrateMainConfig(dataDirectory: Path) {
        val target = dataDirectory.resolve("config.yml")
        val joinStateFile = dataDirectory.resolve("joinstate.yml")
        val tabListFile = dataDirectory.resolve("tablist.yml")
        if (!Files.exists(joinStateFile) && !Files.exists(tabListFile)) return

        val joinStateNode = joinStateFile.takeIf(Files::exists)?.loadYaml()
        val tabListNode = tabListFile.takeIf(Files::exists)?.loadYaml()
        if (Files.exists(target)) {
            if (target.loadYaml().containsMigratedMainConfig(joinStateNode, tabListNode)) {
                deleteIfExists(joinStateFile)
                deleteIfExists(tabListFile)
                return
            }
        }

        val loader = createLoader(target)
        val node = loader.createNode()

        node.node("version").set(CURRENT_CONFIG_VERSION)
        migrateJoinStates(joinStateNode, node)
        migrateTabList(tabListNode, node)
        node.applyMainConfigComments()

        loader.save(node)
        target.writeCommentedYaml(::decorateMainConfigYaml)
        deleteIfExists(joinStateFile)
        deleteIfExists(tabListFile)
    }

    private fun CommentedConfigurationNode.containsMigratedMainConfig(
        joinStateNode: CommentedConfigurationNode?,
        tabListNode: CommentedConfigurationNode?
    ): Boolean {
        if (node("version").string != CURRENT_CONFIG_VERSION) return false

        if (joinStateNode != null) {
            val defaultState = joinStateNode.node("default-state").string ?: "public"
            val expectedInitialLayout = joinStateNode.node("join-states")
                .childrenList()
                .firstOrNull { it.node("name").string == defaultState }
                ?.node("motd-layout-by-proxy")
                ?.string
                ?: "public"

            if (node("initial-state").string != defaultState || node("initial-layout").string != expectedInitialLayout) {
                return false
            }

            val legacyStates = joinStateNode.node("join-states").childrenList()
            val currentStates = node("joinstates").childrenList()
            for (legacyState in legacyStates) {
                val name = legacyState.node("name").string ?: return false
                val currentState = currentStates.firstOrNull { it.node("name").string == name } ?: return false
                if (currentState.node("permission", "join").string != legacyState.node("join-permission").string.orEmpty()) {
                    return false
                }
                if (currentState.node("permission", "full").string != legacyState.node("full-join-permission").string.orEmpty()) {
                    return false
                }
                if (currentState.node("forced-motd-layout").string != legacyState.node("motd-layout-by-proxy").string) {
                    return false
                }
            }
        }

        if (tabListNode != null) {
            val expectedUpdateTime = tabListNode.node("tab-list-update-time")
                .long
                .takeIf { it > 0 }
                ?.let { (it / 50L).coerceAtLeast(1L) }
                ?: 20L

            val legacyGroups = tabListNode.node("groups").childrenList()
            val currentGroups = node("tablist").childrenList()
            for (legacyGroup in legacyGroups) {
                val name = legacyGroup.node("group-or-service").string ?: return false
                val currentGroup = currentGroups.firstOrNull { it.node("name").string == name } ?: return false
                if (currentGroup.node("update-time").long != expectedUpdateTime) return false

                val legacyLayouts = legacyGroup.node("tab-lists").childrenList()
                val currentLayouts = currentGroup.node("layout").childrenList()
                if (currentLayouts.size < legacyLayouts.size) return false

                for ((index, legacyLayout) in legacyLayouts.withIndex()) {
                    val currentLayout = currentLayouts[index]
                    if (currentLayout.node("header").string != legacyLayout.node("header").legacyTextList()) return false
                    if (currentLayout.node("footer").string != legacyLayout.node("footer").legacyTextList()) return false
                }
            }
        }

        return true
    }

    private fun migrateJoinStates(source: CommentedConfigurationNode?, target: CommentedConfigurationNode) {
        val defaultState = source?.node("default-state")?.string ?: "public"
        val oldStates = source?.node("join-states")?.childrenList().orEmpty()

        target.node("initial-state").set(defaultState)
        target.node("initial-layout").set(
            oldStates
                .firstOrNull { it.node("name").string == defaultState }
                ?.node("motd-layout-by-proxy")
                ?.string
                ?: "public"
        )

        val states = if (oldStates.isEmpty()) {
            defaultJoinStates()
        } else {
            oldStates.map { state ->
                mapOf(
                    "name" to (state.node("name").string ?: ""),
                    "permission" to mapOf(
                        "join" to (state.node("join-permission").string ?: ""),
                        "full" to (state.node("full-join-permission").string ?: "")
                    ),
                    "forced-motd-layout" to state.node("motd-layout-by-proxy").string
                )
            }
        }

        target.node("joinstates").set(states)
        val whitelist = WhitelistConfig()
        target.node("whitelist").set(
            mapOf<String, Any>(
                "enabled" to whitelist.enabled,
                "players" to whitelist.players
            )
        )
    }

    private fun migrateTabList(source: CommentedConfigurationNode?, target: CommentedConfigurationNode) {
        val updateTimeTicks = source
            ?.node("tab-list-update-time")
            ?.long
            ?.takeIf { it > 0 }
            ?.let { (it / 50L).coerceAtLeast(1L) }
            ?: 20L

        val oldGroups = source?.node("groups")?.childrenList().orEmpty()
        val tabList = if (oldGroups.isEmpty()) {
            defaultTabList()
        } else {
            oldGroups.map { group ->
                mapOf(
                    "name" to (group.node("group-or-service").string ?: "*"),
                    "layout" to group.node("tab-lists").childrenList().map { layout ->
                        mapOf(
                            "header" to layout.node("header").legacyTextList(),
                            "footer" to layout.node("footer").legacyTextList()
                        )
                    },
                    "update-time" to updateTimeTicks
                )
            }
        }

        target.node("tablist").set(tabList)
    }

    private fun migrateMessages(path: Path) {
        if (!Files.exists(path)) return

        val source = path.loadYaml()
        if (source.node("kick-message").virtual() && source.node("command-message").virtual()) return

        val defaults = MessageConfig()
        val command = source.node("command-message")
        val loader = createLoader(path)
        val target = loader.createNode()

        target.node("version").set(defaults.version)
        target.node("variables").set(mapOf("prefix" to defaults.variables.prefix))
        target.node("kick").set(
            mapOf(
                "no-permission" to source.node("kick-message", "no-permission").stringOr(defaults.kick.noPermission),
                "network-full" to source.node("kick-message", "network-full").stringOr(defaults.kick.networkFull),
                "no-join-state" to source.node("kick-message", "no-join-state").stringOr(defaults.kick.noJoinState)
            )
        )
        target.node("command").set(
            mapOf(
                "join-state" to mapOf(
                    "server" to mapOf(
                        "update-success" to command.node("join-state-server-update-success").stringOr(defaults.command.joinState.server.updateSuccess),
                        "update-failure" to command.node("join-state-server-update-failure").stringOr(defaults.command.joinState.server.updateFailure),
                        "update-no-change" to command.node("join-state-server-update-no-change").stringOr(defaults.command.joinState.server.updateNoChange)
                    ),
                    "group" to mapOf(
                        "update-success" to command.node("join-state-group-update-success").stringOr(defaults.command.joinState.group.updateSuccess),
                        "update-failure" to command.node("join-state-group-update-failure").stringOr(defaults.command.joinState.group.updateFailure),
                        "update-no-change" to command.node("join-state-group-update-no-change").stringOr(defaults.command.joinState.group.updateNoChange)
                    ),
                    "help" to mapOf(
                        "header" to command.node("join-state-help-header").stringOr(defaults.command.joinState.help.header),
                        "entry" to command.node("join-state-help-command").stringOr(defaults.command.joinState.help.entry)
                    ),
                    "list" to mapOf(
                        "groups" to mapOf(
                            "header" to command.node("join-state-group-list-header").stringOr(defaults.command.joinState.list.groups.header),
                            "entry" to command.node("join-state-group-list-entry").stringOr(defaults.command.joinState.list.groups.entry)
                        ),
                        "states" to mapOf(
                            "header" to command.node("join-state-state-list-header").stringOr(defaults.command.joinState.list.states.header),
                            "entry" to command.node("join-state-state-list-entry").stringOr(defaults.command.joinState.list.states.entry)
                        )
                    )
                ),
                "layout" to mapOf(
                    "help" to mapOf(
                        "header" to defaults.command.layout.help.header,
                        "entry" to defaults.command.layout.help.entry
                    ),
                    "info" to mapOf(
                        "header" to defaults.command.layout.info.header,
                        "entry" to defaults.command.layout.info.entry
                    ),
                    "set" to mapOf(
                        "update-success" to defaults.command.layout.set.updateSuccess,
                        "update-failure" to defaults.command.layout.set.updateFailure,
                        "update-no-change" to defaults.command.layout.set.updateNoChange
                    )
                )
            )
        )
        target.applyMessageComments()

        loader.save(target)
        path.writeCommentedYaml(::decorateMessagesYaml)
    }

    private fun migratePlaceholder(path: Path) {
        if (!Files.exists(path)) return

        val source = path.loadYaml()
        if (source.node("current-date-format").virtual() && source.node("current-time-format").virtual()) return

        val loader = createLoader(path)
        val target = loader.createNode()
        target.node("currentDateFormat").set(source.node("current-date-format").string ?: "dd.MM.yyyy")
        target.node("currentTimeFormat").set(source.node("current-time-format").string ?: "HH:mm:ss")
        target.node("pingColors").set(
            source.node("ping-colors").childrenList().map { color ->
                mapOf(
                    "ping" to color.node("ping").int,
                    "color" to color.node("color").string.orEmpty()
                )
            }
        )

        loader.save(target)
    }

    private fun migrateLayouts(layoutDirectory: Path) {
        if (!Files.isDirectory(layoutDirectory)) return

        Files.list(layoutDirectory).use { paths ->
            paths
                .filter { it.isRegularFile() && it.extension == "yml" }
                .forEach(::migrateLayout)
        }
        deleteGeneratedDefaultLayoutIfPublicExists(layoutDirectory)
    }

    private fun migrateLayout(path: Path) {
        val source = path.loadYaml()
        if (!source.isLegacyLayout()) {
            updateCurrentLayoutVersion(path, source)
            return
        }

        val legacyIconFile = source.node("server-icon").string
        val loader = createLoader(path)
        val target = loader.createNode()
        val layoutName = path.name.removeSuffix(".yml")
        val firstLines = source.node("first-lines").legacyLines(default = listOf("<color:#0ea5e9>A simplecloud.app network"))
        val secondLines = source.node("second-lines").legacyLines(default = listOf(" "))
        val entryCount = maxOf(firstLines.size, secondLines.size, 1)
        val versionName = source.node("version-name").string.orEmpty()
        val playerInfo = source.node("player-info").legacyLines(default = emptyList())
        val displayType = source.node("max-player-display-type").string ?: "REAL"
        val dynamicPlayerRange = source.node("dynamic-player-range").int.takeIf { it > 0 } ?: 5

        target.node("config-version").set(CURRENT_LAYOUT_VERSION)
        target.node("motd").set(
            mapOf(
                "enabled" to source.node("enabled").booleanOr(true),
                "update-type" to "RANDOM",
                "layouts" to (0 until entryCount).map { index ->
                    mapOf(
                        "name" to layoutName,
                        "line1" to firstLines.getOrElse(index) { firstLines.lastOrNull().orEmpty() },
                        "line2" to secondLines.getOrElse(index) { secondLines.lastOrNull().orEmpty() }
                    )
                }
            )
        )
        target.node("server-icon").set(
            mapOf(
                "enabled" to false,
                "file" to (legacyIconFile ?: "simplecloud.png")
            )
        )
        target.node("player-list").set(
            mapOf(
                "enabled" to playerInfo.isNotEmpty(),
                "player-list" to playerInfo
            )
        )
        target.node("version").set(
            mapOf(
                "name" to mapOf(
                    "enabled" to versionName.isNotBlank(),
                    "text" to versionName.ifBlank { "Example" }
                ),
                "slots" to mapOf(
                    "enabled" to true,
                    "type" to displayType,
                    "fake-slots" to 100,
                    "dynamic-player-range" to dynamicPlayerRange
                )
            )
        )
        target.applyLayoutComments()

        loader.save(target)
        path.writeCommentedYaml(::decorateLayoutYaml)
    }

    private fun updateCurrentLayoutVersion(path: Path, source: CommentedConfigurationNode) {
        if (source.node("motd").virtual()) {
            return
        }

        var changed = false
        if (source.node("config-version").string != CURRENT_LAYOUT_VERSION) {
            source.node("config-version").set(CURRENT_LAYOUT_VERSION)
            changed = true
        }
        changed = disableServerIconForPreviouslyMigratedLayout(source) || changed
        if (!changed) return

        source.applyLayoutComments()
        createLoader(path).save(source)
        path.writeCommentedYaml(::decorateLayoutYaml)
    }

    private fun disableServerIconForPreviouslyMigratedLayout(source: CommentedConfigurationNode): Boolean {
        val serverIcon = source.node("server-icon")
        val playerList = source.node("player-list")
        val looksLikePreviouslyMigratedLayout = serverIcon.node("enabled").boolean &&
            serverIcon.node("file").string == "simplecloud.png" &&
            playerList.node("enabled").boolean == false &&
            playerList.node("player-list").childrenList().isEmpty()

        if (looksLikePreviouslyMigratedLayout) {
            serverIcon.node("enabled").set(false)
            return true
        }
        return false
    }

    private fun deleteGeneratedDefaultLayoutIfPublicExists(layoutDirectory: Path) {
        val publicLayout = layoutDirectory.resolve("public.yml")
        val defaultLayout = layoutDirectory.resolve("default.yml")
        if (!Files.exists(publicLayout) || !Files.exists(defaultLayout)) return

        val defaultNode = defaultLayout.loadYaml()
        val firstMotd = defaultNode.node("motd", "layouts").childrenList().firstOrNull() ?: return
        val isGeneratedDefault = defaultNode.node("motd").virtual().not() &&
            firstMotd.node("name").string == "default" &&
            firstMotd.node("line1").string == "<color:#0ea5e9>A simplecloud.app network" &&
            firstMotd.node("line2").string == " "

        if (isGeneratedDefault) {
            deleteIfExists(defaultLayout)
        }
    }

    private fun CommentedConfigurationNode.isLegacyLayout(): Boolean {
        val version = node("version").string ?: return false
        return version == "1" &&
            !node("first-lines").virtual() &&
            node("motd").virtual()
    }

    private fun CommentedConfigurationNode.legacyTextList(): String {
        return legacyLines(default = emptyList()).joinToString("<br>")
    }

    private fun CommentedConfigurationNode.legacyLines(default: List<String>): List<String> {
        if (virtual()) return default
        if (isList) return childrenList().map { it.string.orEmpty() }
        return listOf(string.orEmpty())
    }

    private fun CommentedConfigurationNode.stringOr(default: String): String {
        return string ?: default
    }

    private fun CommentedConfigurationNode.booleanOr(default: Boolean): Boolean {
        return if (virtual()) default else boolean
    }

    private fun CommentedConfigurationNode.applyMainConfigComments() {
        node("initial-state").comment(JOIN_STATES_COMMENT)
        node("whitelist").comment(WHITELIST_COMMENT)
        node("whitelist", "players").comment("Supports player names and UUIDs.")
        node("tablist").comment(TABLIST_COMMENT)
        node("tablist").childrenList().forEach { group ->
            group.node("update-time").comment("Update interval in ticks.")
        }
    }

    private fun CommentedConfigurationNode.applyLayoutComments() {
        node("motd").comment(SERVER_LIST_LAYOUT_COMMENT)
        node("motd", "update-type").comment("QUEUE, RANDOM")
        node("server-icon").comment("Server icon displayed in the multiplayer server list.")
        node("player-list").comment("Text displayed when hovering over the player count in the server list")
        node("version").comment("Server version text and player slots configuration")
        node("version", "slots", "type").comment(
            "REAL - Displays the actual number of online players\n" +
                "FAKE - Displays a fixed custom number of online players\n" +
                "DYNAMIC - Displays a dynamic number based on actual players plus a range"
        )
    }

    private fun CommentedConfigurationNode.applyMessageComments() {
        node("variables").comment(VARIABLES_COMMENT)
        node("kick").comment(KICK_MESSAGES_COMMENT)
        node("command").comment(COMMAND_MESSAGES_COMMENT)
    }

    private fun defaultJoinStates(): List<Map<String, Any?>> {
        return ProxyEssentialsConfig().joinstates.map { state ->
            mapOf(
                "name" to state.name,
                "permission" to mapOf(
                    "join" to state.permission.join,
                    "full" to state.permission.full
                ),
                "forced-motd-layout" to state.forcedMotdLayout
            )
        }
    }

    private fun defaultTabList(): List<Map<String, Any>> {
        return ProxyEssentialsConfig().tablist.map { group ->
            mapOf(
                "name" to group.name,
                "layout" to group.layout.map { layout ->
                    mapOf(
                        "header" to layout.header,
                        "footer" to layout.footer
                    )
                },
                "update-time" to group.updateTime
            )
        }
    }

    private fun Path.loadYaml(): CommentedConfigurationNode {
        return createLoader(this).load()
    }

    private fun deleteIfExists(path: Path) {
        Files.deleteIfExists(path)
    }

    private fun Path.writeCommentedYaml(decorate: (String) -> String) {
        Files.writeString(this, decorate(Files.readString(this)))
    }

    private fun decorateMainConfigYaml(content: String): String {
        return content
            .insertBefore("initial-state:", JOIN_STATES_YAML_COMMENT)
            .insertBefore("whitelist:", WHITELIST_YAML_COMMENT)
            .insertBefore("    players:", "    # Supports player names and UUIDs.\n")
            .insertBefore("tablist:", TABLIST_YAML_COMMENT)
            .insertBefore("    update-time:", "    # Update interval in ticks.\n")
    }

    private fun decorateLayoutYaml(content: String): String {
        return content
            .insertBefore("motd:", SERVER_LIST_LAYOUT_YAML_COMMENT)
            .insertBefore("    update-type:", "    # QUEUE, RANDOM\n")
            .insertBefore("player-list:", "# Text displayed when hovering over the player count in the server list\n")
            .insertBefore("version:", "# Server version text and player slots configuration\n")
            .insertBefore("        type:", "        # REAL - Displays the actual number of online players\n" +
                "        # FAKE - Displays a fixed custom number of online players\n" +
                "        # DYNAMIC - Displays a dynamic number based on actual players plus a range\n")
    }

    private fun decorateMessagesYaml(content: String): String {
        return content
            .insertBefore("variables:", VARIABLES_YAML_COMMENT)
            .insertBefore("kick:", KICK_MESSAGES_YAML_COMMENT)
            .insertBefore("command:", COMMAND_MESSAGES_YAML_COMMENT)
    }

    private fun String.insertBefore(linePrefix: String, comment: String): String {
        if (comment.isEmpty() || contains(comment.trimEnd())) return this
        val lines = lines().toMutableList()
        val index = lines.indexOfFirst { it.startsWith(linePrefix) }
        if (index == -1) return this
        val commentLines = comment.trimEnd().lines()
        lines.addAll(index, commentLines)
        return lines.joinToString("\n").trimEnd() + "\n"
    }

    private fun createLoader(path: Path): YamlConfigurationLoader {
        return YamlConfigurationLoader.builder()
            .path(path)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                }
            }
            .build()
    }

    private const val JOIN_STATES_COMMENT =
        "───────────────────────────────────────────────────────────────────────────────\n" +
            "Join States\n" +
            "Configures who can join the proxy and how full or restricted states behave.\n\n" +
            "Read more @ https://docs.simplecloud.app/manual/plugins/proxy-essentials\n" +
            "───────────────────────────────────────────────────────────────────────────────\n\n" +
            "Initial settings used when a group is created for the first time."

    private const val WHITELIST_COMMENT =
        "Global whitelist for direct player access.\n" +
            "Prefer permission-based access for regular users.\n" +
            "Use this list only for administrators or emergency access."

    private const val TABLIST_COMMENT =
        "───────────────────────────────────────────────────────────────────────────────\n" +
            "Tablist\n" +
            "Configures tablist layouts and update intervals for connected players.\n\n" +
            "Read more @ https://docs.simplecloud.app/manual/plugins/proxy-essentials\n" +
            "───────────────────────────────────────────────────────────────────────────────"

    private const val SERVER_LIST_LAYOUT_COMMENT =
        "───────────────────────────────────────────────────────────────────────────────\n" +
            "Server List Layout\n" +
            "Configures how your server appears in the multiplayer server list,\n" +
            "including MOTD, server icon, and player count display.\n\n" +
            "Read more @ https://docs.simplecloud.app/manual/plugins/proxy-essentials\n" +
            "───────────────────────────────────────────────────────────────────────────────"

    private const val VARIABLES_COMMENT =
        "───────────────────────────────────────────────────────────────────────────────\n" +
            "Variables\n" +
            "Reusable variables that can be used throughout the messages.\n" +
            "Usage: <variable_name> will be replaced with the defined value.\n" +
            "───────────────────────────────────────────────────────────────────────────────"

    private const val KICK_MESSAGES_COMMENT =
        "───────────────────────────────────────────────────────────────────────────────\n" +
            "Kick Messages\n" +
            "───────────────────────────────────────────────────────────────────────────────"

    private const val COMMAND_MESSAGES_COMMENT =
        "───────────────────────────────────────────────────────────────────────────────\n" +
            "Command Messages\n" +
            "───────────────────────────────────────────────────────────────────────────────"

    private const val JOIN_STATES_YAML_COMMENT =
        "\n# ───────────────────────────────────────────────────────────────────────────────\n" +
            "# Join States\n" +
            "# Configures who can join the proxy and how full or restricted states behave.\n" +
            "#\n" +
            "# Read more @ https://docs.simplecloud.app/manual/plugins/proxy-essentials\n" +
            "# ───────────────────────────────────────────────────────────────────────────────\n" +
            "\n" +
            "# Initial settings used when a group is created for the first time.\n"

    private const val WHITELIST_YAML_COMMENT =
        "\n# Global whitelist for direct player access.\n" +
            "# Prefer permission-based access for regular users.\n" +
            "# Use this list only for administrators or emergency access.\n"

    private const val TABLIST_YAML_COMMENT =
        "\n# ───────────────────────────────────────────────────────────────────────────────\n" +
            "# Tablist\n" +
            "# Configures tablist layouts and update intervals for connected players.\n" +
            "#\n" +
            "# Read more @ https://docs.simplecloud.app/manual/plugins/proxy-essentials\n" +
            "# ───────────────────────────────────────────────────────────────────────────────\n"

    private const val SERVER_LIST_LAYOUT_YAML_COMMENT =
        "\n# ───────────────────────────────────────────────────────────────────────────────\n" +
            "# Server List Layout\n" +
            "# Configures how your server appears in the multiplayer server list,\n" +
            "# including MOTD, server icon, and player count display.\n" +
            "#\n" +
            "# Read more @ https://docs.simplecloud.app/manual/plugins/proxy-essentials\n" +
            "# ───────────────────────────────────────────────────────────────────────────────\n"

    private const val VARIABLES_YAML_COMMENT =
        "\n# ───────────────────────────────────────────────────────────────────────────────\n" +
            "# Variables\n" +
            "# Reusable variables that can be used throughout the messages.\n" +
            "# Usage: <variable_name> will be replaced with the defined value.\n" +
            "# ───────────────────────────────────────────────────────────────────────────────\n"

    private const val KICK_MESSAGES_YAML_COMMENT =
        "\n# ───────────────────────────────────────────────────────────────────────────────\n" +
            "# Kick Messages\n" +
            "# ───────────────────────────────────────────────────────────────────────────────\n"

    private const val COMMAND_MESSAGES_YAML_COMMENT =
        "\n# ───────────────────────────────────────────────────────────────────────────────\n" +
            "# Command Messages\n" +
            "# ───────────────────────────────────────────────────────────────────────────────\n"
}
