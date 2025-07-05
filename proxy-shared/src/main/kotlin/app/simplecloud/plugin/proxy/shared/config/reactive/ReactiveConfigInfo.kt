package app.simplecloud.plugin.proxy.shared.config.reactive


data class ReactiveConfigInfo<T>(
    val clazz: Class<T>,
    val reactiveConfig: ReactiveConfig<T>
)