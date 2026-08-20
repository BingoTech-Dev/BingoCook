
# BingoCook

A Minecraft mod built with NeoForge for Minecraft 26.2.

## Requirements

- JDK 25 (64-bit)
- IntelliJ IDEA 2025.2+ or Eclipse 2025-12+

## Setup

```bash
# Import the project in your IDE (IntelliJ IDEA: open build.gradle as a Gradle project)
# or build from the command line:
./gradlew build
```

## Run configurations

- `./gradlew runClient` — launch the game client
- `./gradlew runServer` — launch a dedicated server
- `./gradlew runData` — run data generators (output to `src/generated/resources`)

If you are missing libraries in your IDE, run `./gradlew --refresh-dependencies`.
To reset everything (does not affect your code), run `./gradlew clean`.

## Mapping names

By default this project uses the official Mojang mapping names, which are covered by a
specific license: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

## Additional resources

- NeoForge documentation: https://docs.neoforged.net/
- NeoForge Discord: https://discord.neoforged.net/
