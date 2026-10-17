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

Each Minecraft version gets its own branch. Minecraft changed how it numbers versions, so there are two styles:

| Minecraft versioning | Branch is named | Examples |
|---|---|---|
| Old, `major.minor.patch` (1.x) | the full version | `1.20.4`, `1.21.1`, `1.21.4` |
| New, `year.release` (26.1 and later) | `{year}.{release}` | `26.1`, `26.2` |

With the old style every version, including patch versions, has its own branch, because they can differ. With the new style a hotfix of the same release (for example 26.1.1) stays on the `26.1` branch.

The mod version is not part of the branch name, because the release tag already identifies a point in time.

`main` is the branch for the latest Minecraft version. Older Minecraft versions live on their own branches and only receive fixes.

## Making a release

Releases are made by the **Release** GitHub Actions workflow (`.github/workflows/release.yml`).

1. Make sure the change is merged into the right branch (`main` for the latest Minecraft version, otherwise that version's branch) and that CI passes.
2. Open **Actions > Release > Run workflow**, pick the branch, and choose:
   - **bump**: `patch`, `minor` or `major`, which decides how `mod_version` goes up (for example `1.1.1` becomes `1.1.2`, `1.2.0` or `2.0.0`)
   - **environment**: `test` or `prod`

Or from the terminal: `gh workflow run release.yml --ref main -f bump=patch -f environment=test`.

### What each environment does

| Step | `test` | `prod` |
|---|---|---|
| Work out the new version and tag `{minecraft_version}-v{mod_version}` | yes | yes |
| Commit the new `mod_version` to the branch | no | yes |
| Build with `./gradlew build` | yes | yes |
| Generate release notes with GitHub | yes | yes |
| Create the tag and GitHub release with the jars | no | yes |
| Publish to Modrinth (same name, notes and jars) | test project | real project |

Use `test` to try the workflow: it publishes only to the Modrinth test project and does not create a tag, a GitHub release or a commit.

### Environment setup

Both environments (`test` and `prod`, under **Settings > Environments**) need:

| Name | Kind | Value |
|---|---|---|
| `MODRINTH_TOKEN` | secret | Modrinth personal access token |
| `MODRINTH_PROJECT_ID` | variable | the Modrinth project to publish to (the test project for `test`) |
| `MODRINTH_API_URL` | variable | `https://api.modrinth.com/v2` |
| `MODRINTH_DEP_FABRIC_API` | variable | Modrinth project ID of Fabric API |
| `MODRINTH_DEP_OWO` | variable | Modrinth project ID of owo-lib |

`prod` only allows deployments from `main` and the Minecraft version branches. The prod run pushes the version commit with the built-in `GITHUB_TOKEN`, so if branch protection that blocks direct pushes is ever turned on, the workflow needs a bypass or a personal access token.

## Starting a new Minecraft version

1. Open an issue for the update (for example "Update to Minecraft 26.3").
2. Branch from the latest branch and bump `minecraft_version`, `loader_version`, `fabric_api_version`, `loom_version` and `owo_version` in `gradle.properties`.
3. Update `fabric.mod.json`, fix any compile errors and test with `./gradlew runClient`.
4. Merge, then release as above. If the previous Minecraft version should keep receiving fixes, keep its branch.
5. Dependabot keeps the Gradle wrapper up to date on `main`.
