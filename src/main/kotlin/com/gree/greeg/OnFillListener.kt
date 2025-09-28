package com.gree.greeg

/**
 * @Author GZDong
 * @Date 2023/7/3
 */
interface OnFillListener {
    fun onFinished(tempActivityName: String, tempPath: String, chooseRep: Boolean, isComposeVersion: Boolean, isPageSelected: Boolean)
}