# Proxy Plugin with MOTD and TabList Functions
## Description

This proxy plugin provides a variety of features for MOTD and TabList in your server network. It supports BungeeCord/Waterfall and Velocity.

## Features

### MOTD

- Configurable first and second line of the MOTD.
- Ability to specify multiple options for each line to be randomly selected.
- Customizable PlayerInfo and server version.
- Dynamic player counts with configurable range.
- Support for Adventure Minimessage format.
- Standard placeholders: `%ONLINE_PLAYERS%` and `%MAX_PLAYERS%`.

### TabList

- Customizable update interval.
- Animated header and footer.
- Ability to set tab lists based on server names or groups.
- Usage of Adventure Minimessage format.
- Standard placeholders: `%ONLINE_PLAYERS%`, `%MAX_PLAYERS%`, `%SERVICE_NAME%`, `%CURRENT_TIME%`, `%CURRENT_DATE%` and `%PING%`.

## Configuration

The plugin is configured via the files `motd.yml` and `tablist.yml`. In these files you will find all the options for customizing MOTD and TabList.

## Plugin API

The plugin provides an API for other plugins to interact with the MOTD and TabList. The API is available for both BungeeCord/Waterfall and Velocity.

You can use the event `MotdConfigurationEvent` or `TabListConfigurationEvent` to update the MOTD or TabList. The event contains the configuration object that you can modify.
In the case of the TabList event, you can edit the header and footer of the TabList. Velocity is used for this example.

```kotlin
    @Subscribe
    fun onTabListConfiguration(event: TabListConfigurationEvent) {
        var header = event.tabListConfiguration.header
        header = header.replaceText(TextReplacementConfig.builder().match("%MY_FUNNY_PLACEHOLDER%").replacement("<red>Test" + 123).build())
        event.tabListConfiguration.header = header

        var footer = event.tabListConfiguration.footer
        footer = footer.replaceText(TextReplacementConfig.builder().match("%MY_FUNNY_PLACEHOLDER%").replacement("<red>Test" + 123).build())
        event.tabListConfiguration.footer = footer
    }
```

## Additional Information

For more information about the plugin, please visit the project page: <https://wiki.simplecoud.app/plugin/proxy>

## Installation

1. Download the plugin from the project page: <https://wiki.simplecoud.app/plugin/proxy>.
2. Copy the file `proxy-velocity.jar` or `proxy-bungeecord.jar` into your proxy's plugin folder.
3. Restart the proxy.
4. Edit the files `motd.yml` and `tablist.yml` according to your preferences.
5. Restart the proxy again.

## Supported Platforms:

- BungeeCord/Waterfall
- Velocity

## License:

This plugin is licensed under the [MIT License](https://opensource.org/licenses/MIT). For more information, refer to the accompanying license file.