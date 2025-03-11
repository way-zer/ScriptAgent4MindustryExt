![For Mindustry](https://img.shields.io/badge/For-Mindustry-orange)
![Lang CN](https://img.shields.io/badge/Lang-ZH--CN-blue)
![Support 7.5](https://img.shields.io/badge/Support_Version-7.5(136+)-success)
![GitHub Releases](https://img.shields.io/github/downloads/way-zer/ScriptAgent4MindustryExt/latest/total)
[![BuildPlugin](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/buildPlugin.yml/badge.svg)](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/buildPlugin.yml)
[![CheckScript](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/checkScripts.yml/badge.svg)](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/checkScripts.yml)

> This README is machine-translated. For any issues or suggestions, please feel free to open an issue or submit a PR to improve it.

## ScriptAgent
A modular framework based on Kotlin script (kts)
- Powerful: Based on Kotlin, it can access all Java interfaces (what plugins can do, scripts can do)
- Efficient: After script loading, converted to JVM bytecode, with no performance gap compared to Java plugins
- Flexible: Modules and scripts have complete life cycles, supporting hot loading and hot reloading
- Quick Development: Provides a wealth of practical auxiliary functions, no compilation needed, quick deployment to the server
- Intelligent: Supports intelligent completion in IDEA or Android Studio during development
- Customizable: Apart from the core part, plugin functions are implemented via scripts, modifiable as per requirements, and module definition scripts can also expand DSL for scripts

The loader (jar) itself has no specific functionality, it is only responsible for the loading and management of scripts, with all functions implemented by scripts.

### ScriptAgent for Mindustry (SA4MDT)
This is the implementation of the framework for Mindustry, comprising a loader and a series of functional scripts, specifically divided into the following 6 modules:
- coreLib (coreLibrary): The standard library of the framework
- core (coreMindustry): The specific implementation for Mindustry
- main module: Used for storing simple scripts
- wayzer module: A complete set of Mindustry server basic plugins (By: WayZer)
  - Discussion: QQ group 1033116078 or discuss directly in Discussions
  - Plugin test server: cn.mindustry.top
- mapScript: Special scripts designed for MDT, with life cycles bound to a single game session, loaded only when needed
- ~~mirai module: Script wrapper for the QQ bot library mirai~~ (Due to uncontrollable factors from upstream, planned to be removed)

### Client Preview
![image](https://user-images.githubusercontent.com/15688938/132090295-59a57f81-cc72-4ab5-8c10-deadf7ae452a.png)
![image](https://user-images.githubusercontent.com/15688938/132090317-cc62339d-8ce5-4906-90d0-e8fda1bacf36.png)

### Server Backend Preview
![image](https://user-images.githubusercontent.com/15688938/132090197-e041d11c-e09a-49ee-94e8-d2cdae30038f.png)
![image](https://user-images.githubusercontent.com/15688938/132090212-1f924326-4ba7-43be-bbb8-e055599fa75c.png)
![image](https://user-images.githubusercontent.com/15688938/132090238-bbfcaf2e-154a-446c-9d1f-92f391835f0a.png)

## Quick Start
### Plugin Installation (Recommended for General Users)
The allInOne version integrates pre-compiled scripts into the loader:
1. Download the `xxx.allinone.jar` file from the Release page and place it in the `config/mods` directory
2. Start the server (the first start will download dependencies from the network, which will take a long time)

### Loader + Script Installation (For Advanced Users)
1. Download the pre-compiled jar and script package zip from the Release page
2. Place the jar file in the `config/mods` folder, and extract the script package to the `config/scripts` folder (needs to be created by yourself)
3. Start the server (the first start will download dependencies from the network, which will take a long time)
4. Wait for the plugin to load (the first run of the script will compile, which is time-consuming, and the cache will be saved after compilation)

### Standalone Run/Script Development
Please refer to the [Wiki](https://github.com/way-zer/ScriptAgent4MindustryExt/wiki) for specific functions and development-related content.

## License
- Loader: Free for use; no reproduction or other-purpose use without permission.
- Scripts in this repository:
  - Free for private modification and use. Public use requires indicating the source (fork or reference this repository). Do not modify the original author's copyright information.
  - The `mirai` module and all dependent code follow the `AGPLv3` license.
- Other scripts: Owned by their respective authors, who can declare their own open-source licenses. They are unaffected by the loader's copyright.
