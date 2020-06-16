@file:DependsModule("coreMindustry")

name = "WayZer Mindustry Plugin"
/**
 * 移植自 https://github.com/way-zer/MyMindustryPlugin
 * 功能:
 * (maps) better Maps,GameOver,ChangeMap | 更好的地图管理系统
 * (admin) independent Admin System | 独立的管理员系统
 * (playerInfo) extend variables for PlayerInfo | 扩展info相关变量
 * (ext/vote) Vote System includes: changeMap gameOver skipWave kick rollBack | 投票系统(换图,投降,跳波,踢人,回滚)
 * (ext/autoHost) autoHost after startup | 启动后自动开服
 * (ext/autoSave) autoSave every 10 minutes | 自动保存(10分钟)
 * TODO: (ext/welcomeMsg)
 * TODO: (ext/alert)
 * TODO: (ext/betterTeam)
 * TODO: (ext/pvpProtect)
 * TODO: (ext/playerInfo) /info /mInfo(server)
 * (ext/serverStatus) /status | 获取服务器状态
 * TODO: (ext/playerLevel)
 * TODO: (ext/lang)
 * TODO: (ext/reGrief/unitLimit)
 * TODO: (ext/reGrief/reactor)
 * TODO: (ext/special/observer)
 * TODO: (ext/special/builderRobot)
 */
addDefaultImport("wayzer.lib.*")
generateHelper()