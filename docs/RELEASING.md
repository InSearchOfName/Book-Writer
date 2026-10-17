# Releasing

This page explains how Book Writer is versioned, how branches are laid out, and how a release is made.

## Versions

Two versions are involved, both set in `gradle.properties`:

| Property | Meaning | Example |
|---|---|---|
| `minecraft_version` | The Minecraft version the mod is built for | `26.2` |
| `mod_version` | The version of the mod itself | `1.1.1` |

## Release naming

Every release is named and tagged:

```
{minecraft_version}-v{mod_version}
```

Examples: `26.2-v1.1.1`, `26.1-v1.1.1`, `1.21.11-v1.1.0`.

The same release exists in two places:

- a **git tag** and **GitHub release** with that name, with the mod jar and the sources jar attached
- a **Modrinth version** for that Minecraft version

## Branches

Each Minecraft version gets its own branch, named:

```
{minecraft_version without patch}-v{major version of the mod}
```

Examples:

| Branch | Minecraft | Mod |
|---|---|---|
| `26.1-v1` | 26.1.x | 1.x.x |

The patch number of the Minecraft version is dropped, so patch updates of the same Minecraft version (for example 26.1 to 26.1.1) stay on the same branch. A new mod major version for the same Minecraft version gets a new branch (`26.1-v2`).

`main` is the branch for the latest Minecraft version. Older Minecraft versions live on their own branches and only receive fixes.

## Making a release

1. Make sure the change is merged into the right branch (`main` for the latest Minecraft version, otherwise that version's branch).
2. Set `minecraft_version` and `mod_version` in `gradle.properties` and bump the matching constraints in `src/main/resources/fabric.mod.json`.
3. Check the build passes locally with `./gradlew build`.
4. The Jenkins pipeline (see `Jenkinsfile`) builds the project and publishes the release: it creates the `{minecraft_version}-v{mod_version}` tag and GitHub release with generated release notes, attaches the jars, and uploads the same files to Modrinth using the release notes as the changelog.

## Starting a new Minecraft version

1. Open an issue for the update (for example "Update to Minecraft 26.3").
2. Branch from the latest branch and bump `minecraft_version`, `loader_version`, `fabric_api_version`, `loom_version` and `owo_version` in `gradle.properties`.
3. Update `fabric.mod.json`, fix any compile errors and test with `./gradlew runClient`.
4. Merge, then release as above. If the previous Minecraft version should keep receiving fixes, keep its branch.
5. Dependabot keeps the Gradle wrapper up to date on `main`.
