package app.simplecloud.plugin.proxy.shared.joinstate

sealed interface JoinResult {

    data object Allowed : JoinResult

    data class Denied(val message: String) : JoinResult
}
