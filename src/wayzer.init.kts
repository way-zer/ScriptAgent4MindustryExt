@file:DependsModule("coreMindustry")

import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile

name = "WayZer Mindustry Plugin"
/**
 * 移植自 https://github.com/way-zer/MyMindustryPlugin
 * 功能:
 * (maps) better Maps,GameOver,ChangeMap | 更好的地图管理系统
 * (admin) independent Admin System | 独立的管理员系统
 * (playerInfo) extend variables for PlayerInfo | 扩展info相关变量
 * (user/profileBind) user token generate,check and user bind | 账号令牌生成,检测及用户绑定
 * (ext/vote) Vote System includes: changeMap gameOver skipWave kick rollBack | 投票系统(换图,投降,跳波,踢人,回滚)
 * (ext/autoHost) autoHost after startup | 启动后自动开服
 * (ext/autoSave) autoSave every 10 minutes | 自动保存(10分钟)
 * (ext/welcomeMsg) join Welcome | 进服欢迎信息
 * (ext/alert) alert per interval | 定时轮播公告
 * (ext/betterTeam) | 更好的PVP队伍管理，管理员强制换队以及观察者支持
 * (ext/mapInfo) | 在游戏内显示地图信息
 * TODO: (ext/pvpProtect)
 * TODO: (ext/playerInfo) /info /mInfo(server)
 * (ext/serverStatus) /status | 获取服务器状态
 * (ext/mapSnap) /mapSnap | 保存地图快照到data/mapSnap下
 * TODO: (ext/playerLevel)
 * TODO: (ext/lang)
 * (ext/reGrief/history) get tile action history | 获取某个的操作记录
 * TODO: (ext/reGrief/unitLimit)
 * TODO: (ext/reGrief/reactor)
 * TODO: (ext/special/observer)
 * TODO: (ext/special/builderRobot)
 */
addDefaultImport("wayzer.lib.*")
addDefaultImport("wayzer.lib.dao.*")
generateHelper()
registerTable(PlayerProfile.T, PlayerData.T)