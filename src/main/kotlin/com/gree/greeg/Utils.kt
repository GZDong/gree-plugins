package com.gree.greeg

/**
 * 获取包名
 */
fun getPackageName(path: String): String {
    return path.substring(path.indexOf("java") + 5, path.length).replace("/", ".")
}

fun getXmlPath(path: String): String {
    return path.substring(0, path.indexOf("main") + 5) + "res/layout"
}

fun getModelName(path: String): String {
    val parts = path.split("feature/")
    if (parts.size < 2) {
        return "firstTest"
    }
    val name = parts[1].split("/")
    if (name.isEmpty()) {
        return "firstTest"
    }
    return name[0]
}

fun getModelPath(moduleName: String, path: String): String {
    val str = "module/$moduleName"
    return path.substring(0, path.indexOf(str) + str.length)
}