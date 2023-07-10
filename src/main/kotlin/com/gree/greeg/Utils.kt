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
        return "unknown"
    }
    val name = parts[1].split("/")
    if (name.isEmpty()) {
        return  "unknown"
    }
    return name[0]
}