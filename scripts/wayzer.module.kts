@file:DependsModule("coreMindustry")
@file:Import("com.google.guava:guava:30.1-jre", mavenDepends = true)

import wayzer.lib.dao.Achievement
import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile
import coreLibrary.lib.registerTable

name = "WayZer Mindustry Plugin"
/**
 * 移植自 https://github.com/way-zer/MyMindustryPlugin
 * 功能:
 * (maps) better Maps,GameOver,ChangeMap | 更好的地图管理系统
 * (admin) independent Admin System | 独立的管理员系统
 * (playerInfo) extend variables for PlayerInfo | 扩展info相关变量
 * (permission) permission system | 权限系统
 * (voteProvider) provider for vote service | 投票服务实现
 * (user/profileBind) user token generate,check and user bind | 账号令牌生成,检测及用户绑定
 * (user/level) user exp/level system | 用户经验等级系统
 * (user/expReward) exp/time reward | 经验等级给与(若没有，用户经验和在线时间不会改变)
 * (user/achievement) user achievement system | 用户成就系统
 * (user/infoCommand) get profile info /info /mInfo(server) | /info指令(查看个人信息)
 * (user/statistics) statistics of one game and give exp after game | 游戏结束贡献榜及经验分发
 * (user/skills) skills based on command | 技能
 * (map/autoHost) autoHost after startup | 启动后自动开服
 * (map/autoSave) autoSave every 10 minutes | 自动保存(10分钟)
 * (map/mapInfo) show map info in game| 在游戏内显示地图信息
 * (map/limitAir) ext [@limitAir] for map| 提供地图[@limitAir]标签
 * (map/mapSnap) get current map snapshot| 游戏缩略图生成,保存到data/mapSnap下
 * (ext/vote) Vote System includes: changeMap gameOver skipWave kick rollBack | 投票系统(换图,投降,跳波,踢人,回滚)
 * (ext/welcomeMsg) join Welcome | 进服欢迎信息
 * (ext/alert) alert per interval | 定时轮播公告
 * (ext/betterTeam) better management for pvp team and support for observer| 更好的PVP队伍管理，管理员强制换队以及观察者支持
 * (ext/pvpProtect) pvp protect time | 开局pvp保护功能
 * (ext/serverStatus) /status | 获取服务器状态
 * (ext/autoUpdate) auto update server jar and restart after finish game | 自动升级服务端
 * (ext/lang) i18n | 国际化多语言支持,语言文件保存在scripts/data/lang
 * (ext/reGrief/history) get tile action history | 获取某个的操作记录
 * (ext/reGrief/unitLimit) limit units pre team | 限制每个队伍的单位总数
 * TODO: (ext/reGrief/reactor)
 * TODO: (ext/special/builderRobot)
 */

addDefaultImport("wayzer.lib.*")
addDefaultImport("wayzer.lib.dao.*")
generateHelper()
registerTable(PlayerProfile.T, PlayerData.T, Achievement.T)