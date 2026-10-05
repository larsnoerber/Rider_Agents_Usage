# Documentation index

Source documentation updated for the current 1.1.0 code on October 5, 2026. This update does not create a release,
version tag or binary upload. Generated build/dist folders are not source documentation.

## User guides

- [Repository overview and Rider setup](../README.md)
- [Standalone Windows app](../windows/README.md): ten providers, connections, saved usage, bar layouts and tray
- [Provider/data comparison](PROVIDERS.md): edition availability and quota versus recorded usage
- [Visual Studio Code](../vscode/README.md)
- [Visual Studio](../visualstudio/README.md)
- [Windows privacy](../windows/Packaging/Privacy.md)
- [Rider plugin EULA](../EULA.md) and [source license](../LICENSE)

## Development and delivery

- [Project rules](../AGENTS.md)
- [Architecture](ARCHITECTURE.md): package boundaries and Windows partial-class responsibilities
- [Build/development](DEVELOPMENT.md): separate editor/Windows build commands and Rider C# indexing
- [Windows verification](../windows/TESTING.md): current evidence, historical checks and opt-in commands
- [Changelog](../CHANGELOG.md)
- [Implemented ideas](IDEAS.md): edition scope and future proposals

## Publishing material

- [Marketplace overview](MARKETPLACE.md)
- [Visual Studio upload workflow](../visualstudio/Marketplace/README.md)
- [Visual Studio listing copy](../visualstudio/Marketplace/overview.md)
- [Windows Store draft](../windows/Packaging/StoreListing.md)

Build, installation and publication are separate actions. A successful build does not validate live provider APIs,
Store certification or every desktop interaction. Run/add tests only when requested. The current GitHub update
contains source/docs; EXE/MSIX artifacts stay local.
