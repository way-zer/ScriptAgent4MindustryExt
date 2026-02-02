include("scripts")

file("scripts").listFiles()?.forEach {
    if (it.isDirectory && it.name.startsWith("@")) {
        include("scripts:@${it.name.substring(1)}")
        project(":scripts:@${it.name.substring(1)}").projectDir = it
    }
}