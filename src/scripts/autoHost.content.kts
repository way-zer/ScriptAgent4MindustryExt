import cf.wayzer.script_agent.MindustryMainImpl
onEnable{
    Core.app.post{
        MindustryMainImpl.serverCommand.handleMessage("host")
    }
}