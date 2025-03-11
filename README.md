![For Mindustry](https://img.shields.io/badge/For-Mindustry-orange)
![Lang CN](https://img.shields.io/badge/Lang-ZH--CN-blue)
![Support 7.5](https://img.shields.io/badge/Support_Version-7.5(136+)-success)
![GitHub Releases](https://img.shields.io/github/downloads/way-zer/ScriptAgent4MindustryExt/latest/total)
[![BuildPlugin](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/buildPlugin.yml/badge.svg)](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/buildPlugin.yml)
[![CheckScript](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/checkScripts.yml/badge.svg)](https://github.com/way-zer/ScriptAgent4MindustryExt/actions/workflows/checkScripts.yml)

> For English README see [README_en](./README_en.md)

## ScriptAgent
一套基于Kotlin脚本(kts)的模块化框架
- 强大：基于Kotlin，可以访问所有Java接口（所有插件能实现的功能，脚本都能实现）
- 高效：脚本加载后转换为JVM字节码，与Java插件性能无异
- 灵活：模块和脚本具有完整生命周期，支持热加载和热重载
- 快速开发：提供大量实用辅助函数，无需编译即可快速部署到服务器
- 智能：开发时支持IDEA或Android Studio的智能补全
- 可定制：除核心部分外，插件功能均通过脚本实现，可根据需求自由修改，模块定义脚本还可扩展DSL

加载器（jar）本身无具体功能，仅负责脚本的加载与管理，所有功能均由脚本实现。

### ScriptAgent for Mindustry (SA4MDT)
该框架针对Mindustry的实现，包含加载器（Loader）和一系列功能脚本，具体分为以下6个模块：
- coreLib（coreLibrary）：框架的标准库
- core（coreMindustry）：针对Mindustry的具体实现
- main模块：用于存放简单脚本
- wayzer模块：一套完整的Mindustry服务器基础插件（By: WayZer）
  - 交流QQ群：1033116078 或直接在Discussions讨论
  - 插件测试服务器：cn.mindustry.top
- mapScript：专为MDT设计的特殊脚本，生命周期与单局游戏绑定，仅在需要时加载
- ~~mirai模块：QQ机器人库mirai的脚本封装（因上游不可控因素，计划移除）~~

### 客户端预览
![image](https://user-images.githubusercontent.com/15688938/132090295-59a57f81-cc72-4ab5-8c10-deadf7ae452a.png)
![image](https://user-images.githubusercontent.com/15688938/132090317-cc62339d-8ce5-4906-90d0-e8fda1bacf36.png)

### 服务器后台预览
![image](https://user-images.githubusercontent.com/15688938/132090197-e041d11c-e09a-49ee-94e8-d2cdae30038f.png)
![image](https://user-images.githubusercontent.com/15688938/132090212-1f924326-4ba7-43be-bbb8-e055599fa75c.png)
![image](https://user-images.githubusercontent.com/15688938/132090238-bbfcaf2e-154a-446c-9d1f-92f391835f0a.png)

## 快速入门
### 插件安装（推荐普通用户使用）
allInOne版本在加载器内集成了编译好的脚本
1. 从Release页面下载`xxx.allinone.jar`文件，并将其放置在`config/mods`目录下
2. 启动服务器（首次启动会从网络下载依赖，耗时较长）

### 加载器+脚本安装（高级用户）
1. 从Release页面下载预编译的jar和脚本包zip
2. 将jar文件放置在`config/mods`文件夹下，将脚本包解压到`config/scripts`文件夹（需自行创建）
3. 启动服务器（首次启动会从网络下载依赖，耗时较长）
4. 等待插件加载完成（脚本首次运行会进行编译，耗时较长，编译完成后会保存缓存）

### 独立运行/脚本开发 
请查阅[Wiki](https://github.com/way-zer/ScriptAgent4MindustryExt/wiki)

## 版权说明
- 加载器：免费使用，未经许可禁止转载和用作其他用途
- 本仓库脚本：
  - 默认允许私人修改并使用，但禁止修改原作者版权信息，公开使用需注明出处（fork或引用该仓库）
  - mirai模块及依赖该模块的所有代码，遵循AGPLv3协议
- 其他脚本：归脚本作者所有，作者可自行声明开源协议，不受加载器版权影响