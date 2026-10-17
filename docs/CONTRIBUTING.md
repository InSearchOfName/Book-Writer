# Contributing

## How to contribute

Pull requests can only be opened by collaborators. To contribute, start with an issue:

1. Open an issue from the [issue chooser](https://github.com/InSearchOfName/Book-Writer/issues/new/choose). There are templates for:
   - **Bug report**: include the Minecraft version, mod version and logs
   - **Minecraft version update**: the dependency table and checklist for moving to a new Minecraft version
   - **Feature request**: what you'd like and why
2. Describe what you'd like to change. If you want to work on it yourself, say so in the issue.
3. The maintainer will pick it up, or add you as a collaborator so you can open a pull request for it.

Blank issues are disabled, so pick the template that fits best.

## Pull requests

For collaborators: start from the issue's branch (for example `31-issue-templates`) and describe the change in the pull request, with `Closes #N` for the issue it resolves. See [RELEASING.md](RELEASING.md) for branch and release naming.

## Tests

Unit tests live in `src/test/java` and use JUnit. Run them with:

```
./gradlew test
```

`./gradlew build` (and CI) runs them too, so a failing test fails the build. Logic that doesn't need Minecraft, such as splitting text into pages and books (`BookLayout`), lives in plain classes so it can be unit tested. When you change that logic, add or update a test for it.

The client game test (`./gradlew runClientGameTest`) is not available on this branch, because Fabric API for this Minecraft version has no client game test API.
