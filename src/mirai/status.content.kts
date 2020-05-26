package mirai

subscribeGroupMessages {
    case("服务器状态").quoteReply(
            """
                当前地图为: {map.name} 波数: {state.wave}
                服务器FPS: {fps} 内存占用(MB) {heapUse}
                当前人数: {state.playerSize} 总单位数: {state.allUnit}
            """.trimIndent().with().toString()
    )
}