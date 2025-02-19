dependencyResolutionManagement {
    repositories {
        val inChina = System.getProperty("user.timezone") in arrayOf("Asia/Shanghai", "GMT+08:00")
//    mavenLocal()
        mavenCentral()
        maven(url = "https://www.jitpack.io") {
            content {
                excludeModule("cf.wayzer", "ScriptAgent")
            }
        }

        //ScriptAgent
        if (!inChina) {
            maven("https://maven.tinylake.top/") //cloudFlare mirror
        } else {
            maven {
                url = uri("https://packages.aliyun.com/maven/repository/2102713-release-0NVzQH/")
                credentials {
                    username = "609f6fb4aa6381038e01fdee"
                    password = "h(7NRbbUWYrN"
                }
            }
        }
    }
}

include("scripts")

file("scripts").listFiles()?.forEach {
    if (it.isDirectory && it.name.startsWith("@")) {
        include("scripts:${it.name.substring(1)}")
        project(":scripts:${it.name.substring(1)}").projectDir = it
    }
}