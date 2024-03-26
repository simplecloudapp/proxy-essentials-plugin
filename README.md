# Proxy Plugin mit MOTD und TabList Funktionen
## Beschreibung

Dieses Proxy Plugin bietet eine Vielzahl von Funktionen für die MOTD und TabList in deinem Servernetzwerk. Es unterstützt BungeeCord/Waterfall und Velocity.

## Features

### MOTD

- Konfigurierbare erste und zweite Zeile der MOTD.
- Möglichkeit, mehrere Optionen für jede Zeile anzugeben, die zufällig ausgewählt werden.
- Anpassbare PlayerInfo und Serverversion.
- Dynamische Spielerzahlen mit konfigurierbarem Bereich.
- Unterstützung für das Adventure Minimessage Format.
- Standard-Platzhalter: `%ONLINE_PLAYERS%` und `%MAX_PLAYERS%`.

### TabList

- Anpassbares Update-Intervall.
- Möglichkeit, TabListen basierend auf Servernamen oder Gruppen festzulegen.
- Nutzung des Adventure Minimessage Formats.
- Standard-Platzhalter: `%ONLINE_PLAYERS%`, `%MAX_PLAYERS%` und `%SERVICE_NAME%`.

## Konfiguration

Die Konfiguration des Plugins erfolgt über die Datei `config.yml`. In dieser Datei finden Sie alle Optionen, um die MOTD und die TabList anzupassen.

## Weitere Informationen

Weitere Informationen zum Plugin finden Sie auf der Projektseite: <https://de.squarespace.com/>

## Installation

1. Laden Sie das Plugin von der Projektseite: <https://de.squarespace.com/> herunter.
2. Kopieren Sie die Datei `proxy-velocity.jar` oder `proxy-bungeecord.jar` in den Plugin-Ordner Ihres Proxys.
3. Starten Sie den Proxy neu.
4. Bearbeiten Sie die Datein `motd-configuration.yml` und `tablist-configuration.yml` nach Ihren Wünschen.
5. Starten Sie den Proxy erneut.

## Unterstützte Plattformen:

- BungeeCord/Waterfall
- Velocity

## Lizenz:

Dieses Plugin ist unter der [MIT-Lizenz](https://opensource.org/licenses/MIT) lizenziert. Weitere Informationen findest du in der beigefügten Lizenzdatei.