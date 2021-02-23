![For Mindustry](https://img.shields.io/badge/For-Mindustry-orange)
![Lang CN](https://img.shields.io/badge/Lang-ZH--CN-blue)
![Support 6.0](https://img.shields.io/badge/Support_Version-6.0_lateset-success)
![GitHub Releases](https://img.shields.io/github/downloads/way-zer/ScriptAgent4MindustryExt/latest/total)
[![Build Status](https://travis-ci.com/way-zer/ScriptAgent4MindustryExt.svg?branch=1.1)](https://travis-ci.com/way-zer/ScriptAgent4MindustryExt)
[![BuildPlugin](https://github.com/way-zer/ScriptAgent4Mindustry/actions/workflows/buildPlugin.yml/badge.svg)](https://github.com/way-zer/ScriptAgent4Mindustry/actions/workflows/buildPlugin.yml)
[![CheckScript](https://github.com/way-zer/ScriptAgent4Mindustry/actions/workflows/checkScripts.yml/badge.svg)](https://github.com/way-zer/ScriptAgent4Mindustry/actions/workflows/checkScripts.yml)
# ScriptAgent for Mindustry
一个强大的Mindustry脚本插件,基于kts定义的DSL  
A strong script plugin for Mindustry by kts(for english README see [me](./README_en.md))
本仓库包含加载器及大量功能性脚本(可使用或做例子)
This repository contains the loader and lots of strong scripts(use or for example)

## 特性

- 强大,基于kotlin,可以访问所有Java接口(所有插件能干的，脚本都能干)
- 快速,脚本加载完成后，转换为jvm字节码，和java插件没有性能差距
- 灵活,模块与脚本都有完整的生命周期，随时可进行热加载和热重载
- 快速,一大堆开发常用的辅助函数,无需编译,即可快速部署到服务器
- 智能,开发时,拥有IDEA(或AndroidStudio)的智能补全
- 可定制,插件除核心部分外,均使用脚本实现,可根据自己需要进行修改,另外,模块定义脚本也可以为脚本扩充DSL

## 具体功能

本仓库共含5个模块coreLib,core,main,wayzer,mirai

* coreLib为该框架的标准库
* core为针对mindustry的具体实现
* main模块可用来存放简单脚本
* wayzer模块为一套完整的Mindustry服务器基础插件(By: WayZer)
  * 插件测试服务器: mdt.wayzer.cf
  * 交流QQ群: 1033116078 或者直接在Discussions讨论
* mirai为qq机器人库mirai的脚本封装

快速开始,功能介绍等请查阅[Wiki](https://github.com/way-zer/ScriptAgent4MindustryExt/wiki)

## 版权

- 插件本体：未经许可禁止转载和用作其他用途
- 脚本：归属脚本制作者，本仓库脚本转载需注明本页面链接
  - 脚本默认允许私人修改并使用，不允许修改原作者版权信息，公开请folk或引用该仓库(脚本作者声明优先)
  - mirai模块及依赖该模块的所有代码，遵循AGPLv3协议