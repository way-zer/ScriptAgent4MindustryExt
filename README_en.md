[ ![Download](https://api.bintray.com/packages/way-zer/maven/cf.wayzer%3AScriptAgent4Mindustry/images/download.svg) ](https://bintray.com/way-zer/maven/cf.wayzer%3AScriptAgent4Mindustry/_latestVersion)
# ScriptAgent for Mindustry
一个强大的Mindustry脚本插件,基于kts定义的DSL  
A strong script plugin for Mindustry by kts(for english README see [me](./README_en.md))

## features
- Powerful, based on **kotlin**, can access all Java interfaces (all plugins can do it, scripts can do it)
- Fast, after the script is loaded, it is converted to jvm byteCode, and there is no performance gap with plugin written in java
- Flexible, modules and scripts have a complete life cycle, and can be hot-loaded and hot-reloaded at any time
- Fast, a lot of helper functions commonly used for development, can be quickly deployed to the server without compilation
- Smart, with IDEA (or AndroidStudio) smart completion
- Customizable, except for the core loader, the plugin is implemented by scripts, which can be modified according to your own needs.  
    In addition, the module definition script can extend content scripts (DSL,library,manager,defaultImport)
## Install This Plugin
1. Download the **jar**, see **Download** badge
2. Install the plugin in server(Placed under config/mods)
3. Install the scripts and place them (files and folders in /src) directly into the plugin configuration directory (config/)
### Base Commands
There is no permission system, so only administrator and console can use these commands
- **sMod** \<reload/list\> \[name\] - List or reload modules
- **sReload** \<name\> \[modName\] - reload script
- **sList** \[modName\] - List all loaded scripts in the module
- **sLoad** \<path\> - Load a new module or script (specified to the full file name)
## How to develop scripts
1. Copy this repository (or configure gradle yourself, see build.gradle.kts)
2. Import the project in IDEA (recommended to import as Project to avoid interference)
3. Synchronous Gradle
## Directory Structure
- scripts.init.kts (Module definition script)
- scripts(Module root directory)
    - lib(Module library directory, write in **.kt**, shared by all scripts of the module, the same life cycle as the module)
    - .metadata(Module metadata for IDE and other compilers to analyze and compile, and can be generated when the plugin is run)
    - manager.content.kts(Script to implement your logic)
### Script properties
#### Common properties
Features of both scripts
```kotlin
@file:ImportByClass("mindustry.Vars") //Import a loaded library
//Import Maven dependencies (automatic download when can't find the cache , the dependencies will not be resolved)
@file:MavenDepends("de.tr7zw:item-nbt-api:2.2.0","https://repo.codemc.org/repository/maven-public/")
@file:ImportScript("") //Import other source code (often refer to the library outside the module library, the same life cycle as the script)
//Some attributes
name.set("Base Module")//Set current script name (for display purposes only)
val enabled:Boolean
sourceFile.get()//Get the current script source file (not recommended for abuse)
//Life cycle
onEnable{}
onDisable{}
```
#### init.kts(Module definition script)
Mainly responsible for extending the definition of subscripts and providing custom DSL  
Can be extended using extension functions (attributes) and DSLKey  
Within the lifecycle function, register or clean up for subscripts
```kotlin
import cf.wayzer.script_agent.mindustry.Helper.baseConfig
addLibraryByClass("mindustry.Vars")//Similar to ImportByClass, targeted for subscripts
addLibrary(File("xxxx"))//Import library files for subscripts
addLibraryByName("xxxx")//Provide names to find dependent libraries, for example: kotlin-stdlib
addDefaultImport("mindustry.Vars.*")//Add default import, no need for subscript import(cooperate with extension functions)
baseConfig()//Some basic extensions for Mindustry
generateHelper()//Generate metadata (runtime)

children.get() //Get all subscript instances
//Lifecycle functions related to subscripts
onBeforeContentEnable{script-> }
onAfterContentEnable{script-> }
onBeforeContentDisable{script-> }
onAfterContentDisable{script-> }
```
#### content.kts(Module content script)
The main bearer of business logic
```kotlin
module.get() //Get instance of module definition script (not recommended for abuse)
//interfaces for internals
import cf.wayzer.script_agent.MindustryMainImpl
Manager.scriptManager //Get Script Manager(not recommended for abuse)
MindustryMainImpl.clientCommand //commandHandler for client commands (not recommended for abuse)
MindustryMainImpl.serverCommand //commandHandler for console commands (not recommended for abuse)
//Mindustry basic extensions
this.PlaceHoldApi //Useful for sharing variables or exposing function interfaces across the entire plugin(don't expose classes out of lifecycle)
command(name,description,params="",type=Both){sender,arg->} //Register command (Type setting is client or background instruction, p==null when executed by console)
listen<Event>{e-> } //listen to Event
registerScheduleTask(name){firstRun->} //Register schedule tasks, the plugin manages an independent thread to run, does not start automatically
//Helper function
getScheduleTask(name).start(param) //Corresponds to registerAsyncTask
String.with(vararg vars):PlaceHoldContext //Convert string to PlaceHoldContext
Player?.sendMessage(text: PlaceHoldContext,type= MsgType.Message,time = 10f) //Send a message to the player, automatically handle the player variables, if the player is null, the console
broadcast(text:PlaceHoldContext, type= MsgType.Message, time10f, quite = false, players = Vars.playerGroup) //Broadcast message (quite: whether hidden from the console)
```
### Precautions
1. After reloading the script, the same class is not necessarily the same, pay attention to controlling the life cycle  
    If you need similar operations, you can set up an abstract interface to store variables in a longer life cycle(less frequent reloading)
## Existing modules
- scripts(Main module with basic extensions,you can write simple scripts there)
- wayzer(Server basic management module, Rewritten from [MyMindustryPlugin](https://github.com/way-zer/MyMindustryPlugin))
## copyright
- Plugin：Reprinting and other uses (including decompiling not for use) are prohibited without permission
- Script: belongs to the script maker, the reprint of scripts in this repository needs to indicate the link to this page(or this repository)