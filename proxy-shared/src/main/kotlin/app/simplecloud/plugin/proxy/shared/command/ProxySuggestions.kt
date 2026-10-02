package app.simplecloud.plugin.proxy.shared.command

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.layout.LayoutRepository
import kotlinx.coroutines.future.await
import org.incendo.cloud.kotlin.coroutines.SuspendingSuggestionProvider
import org.incendo.cloud.suggestion.Suggestion
import org.incendo.cloud.suggestion.SuggestionProvider

object ProxySuggestions {

    fun <C : ProxySender> groups(api: CloudApi): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { _, _ ->
            try {
                api.group().allGroups.await().map { Suggestion.suggestion(it.name) }
            } catch (_: Exception) {
                emptyList()
            }
        }.asSuggestionProvider()

    fun <C : ProxySender> targetTypes(): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { _, _ ->
            listOf(Suggestion.suggestion("group"), Suggestion.suggestion("ps"))
        }.asSuggestionProvider()

    fun <C : ProxySender> targets(api: CloudApi, arg: String = "targetType"): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { context, _ ->
            try {
                if (context.get<String>(arg).equals("ps", true)) {
                    api.persistentServer().allPersistentServers.await().map { Suggestion.suggestion(it.name) }
                } else {
                    api.group().allGroups.await().map { Suggestion.suggestion(it.name) }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }.asSuggestionProvider()

    fun <C : ProxySender> serverIds(api: CloudApi, arg: String = "target"): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { context, _ ->
            try {
                val group = context.get<String>(arg)
                api.server().getServersByGroup(group).await().map { Suggestion.suggestion(it.numericalId.toString()) }
            } catch (_: Exception) {
                emptyList()
            }
        }.asSuggestionProvider()

    fun <C : ProxySender> joinStates(config: ConfigurationFactory<ProxyEssentialsConfig>): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { _, _ ->
            config.get().joinstates.map { Suggestion.suggestion(it.name) }
        }.asSuggestionProvider()

    fun <C : ProxySender> layouts(repository: LayoutRepository): SuggestionProvider<C> =
        SuspendingSuggestionProvider<C> { _, _ ->
            repository.getNames().map { Suggestion.suggestion(it) }
        }.asSuggestionProvider()
}
