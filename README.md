# TFA Dry tracker

Personal and team purple dry streaks for **The Fractured Archive**, with a sidebar and optional overlay.

| Completed raid result | Personal dry | Team dry |
| --- | --- | --- |
| Nobody gets a purple | +1 | +1 |
| A teammate gets a purple | +1 | Reset to 0 |
| You get a purple | Reset to 0 | Reset to 0 |

- Solo and group completions count toward both streaks.
- Each character starts at zero and has separate saved counters.
- The last revealed purple item and recipient remain visible through subsequent dry raids.
- **Undo last raid** restores both counters and the last item to their previous values.
- Duplicate completion and reward events do not add another raid.

## Plugin Hub installation

**Publication status: preparing the Plugin Hub submission. The plugin is not searchable yet.**

After RuneLite approves and publishes the plugin, open **Configuration > Plugin Hub**, search for **TFA Dry tracker**, and install it. Enable the plugin in each RuneLite profile where you want it. The purple-chest sidebar shows the active character's saved data.

Publishing a source repository alone does not add a plugin to the Hub; the submission must be reviewed and merged by RuneLite.

## Tracking

A confirmed TFA completion adds one to each streak. The reward reveal then updates that same completion:

- A visible unique chest establishes a team purple and resets the team streak.
- Your personal streak resets only when a game loot announcement names you or your TFA reward inventory contains a purple item. Default or delayed chest-ownership values cannot reset your personal streak.
- The supported TFA announcement is `Playername found something special: Item name`. The parser accepts color tags, non-breaking spaces and an optional final period. It requires a full current-party name and an exact TFA purple item. Older `Special loot:` announcements are also supported.
- Ordinary player chat, room kills, wipes and external spectating do not count as a completed raid.

The plugin reads game state and draws UI. It sends no game inputs or network requests. Saved data uses RuneLite's per-character configuration.

## Limits

Keep the plugin enabled for the completion and reward reveal. Enter the reward vault to observe the purple chest, and open your chest if its item has not already been announced. The item remains unknown until the game reveals it.

Existing historical KC is not imported: the first observed completion adds one tracked raid. Raids completed while the plugin is disabled cannot be reconstructed. Once a new raid starts, an older pending reward cannot change its counters.

Loading the plugin in RuneLite has been tested by the user. Event parsing and counter behavior have automated tests; a full live raid has not been verified by the developer.

## Development

Use JDK 11 or newer and the included Gradle wrapper:

```sh
./gradlew test
./gradlew run
```

On Windows use `gradlew.bat`. The development client uses RuneLite's standard external-plugin entry point. For Jagex Accounts, follow the [official development sign-in instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

The Hub submission uses `build=standard` and no additional production dependencies. Local build tasks are for development. `./gradlew jar` builds the ordinary plugin JAR; `./gradlew shadowJar` builds a development executable with dependencies.

## References

- [RuneLite Plugin Hub submission process](https://github.com/runelite/plugin-hub#submitting-a-plugin)
- [RuneLite TFA completion recognition](https://github.com/runelite/runelite/blob/9fc69b64f418ab8166890dbd40fb7e63aa333918/runelite-client/src/main/java/net/runelite/client/plugins/screenshot/ScreenshotPlugin.java)
- [RuneLite TFA reward inventory handling](https://github.com/runelite/runelite/blob/9fc69b64f418ab8166890dbd40fb7e63aa333918/runelite-client/src/main/java/net/runelite/client/plugins/loottracker/LootTrackerPlugin.java)
- [Upstream TFA chest detection](https://github.com/LlemonDuck/the-fractured-archive/blob/28a5c062059e8e06378288ea5a6ad77b54fe3e89/src/main/java/com/duckblade/osrs/tfa/features/loot/LootChestHighlight.java)
