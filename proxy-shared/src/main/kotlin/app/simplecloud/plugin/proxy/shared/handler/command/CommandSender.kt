package app.simplecloud.plugin.proxy.shared.handler.command

interface CommandSender {

    fun sendMessage(message: String)
}