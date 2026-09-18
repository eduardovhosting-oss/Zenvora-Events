# 🎉 Zenvora Events

Professional modular event engine for Paper servers.

## Features v1.0.0

- ⛏ Mining Rush
- 👹 Mob Frenzy
- 🎣 Fishing Festival
- ✨ Double XP
- 🎯 Player scoring and leaderboard
- 🏆 Configurable top rewards
- 🎁 Participation rewards
- ⏱ Event duration and cooldown
- 📢 Broadcasts, title, bossbar and sounds
- 🔧 Admin commands and tab completion
- 📅 Automatic scheduler
- 💾 Player/event statistics in YAML
- 🧩 Fully configurable event definitions
- 🌐 Spanish default messages
- 🧱 No external dependencies required

## Requirements

- Paper 26.2 / Minecraft 26.1-era API
- Java 25

Paper's current development documentation recommends Gradle and Java 25 for current Paper API development.

## Build

```bash
./gradlew build
```

The JAR will be in:

```text
build/libs/ZenvoraEvents-1.0.0.jar
```

## Commands

```text
/zevents
/zevents list
/zevents info <id>
/zevents start <id>
/zevents stop
/zevents top
/zevents reload
/zevents stats [player]
```

Alias:

```text
/ze
```

## Permissions

- `zenvoraevents.admin`
- `zenvoraevents.start`
- `zenvoraevents.stop`
- `zenvoraevents.top`
- `zenvoraevents.stats`

## Included event IDs

- `mining_rush`
- `mob_frenzy`
- `fishing_festival`
- `double_xp`

## Reward system

Rewards are executed as console commands.

Available placeholders:

- `%player%`
- `%event%`
- `%position%`
- `%points%`

Example:

```yaml
rewards:
  participation:
    - "give %player% diamond 1"
  top:
    "1":
      - "give %player% diamond 10"
    "2":
      - "give %player% diamond 5"
```

This keeps Zenvora Events compatible with economy plugins, custom currencies, ranks, crates and your own plugins.

## GitHub

Recommended repository name:

`ZenvoraEvents`

Suggested description:

> 🎉 Modular professional event engine for Paper Minecraft servers.

## License

MIT
