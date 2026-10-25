package com.carlos.superhuman.ui

import androidx.compose.runtime.mutableStateListOf

data class Route(val name: String, val arg: String? = null)

object Nav {
    val tabs = listOf("home", "tasks", "finance", "invest", "more")
    val stack = mutableStateListOf(Route("home"))
    val current: Route get() = stack.last()

    fun go(name: String, arg: String? = null) {
        if (name in tabs) {
            stack.clear()
            stack.add(Route(name))
        } else stack.add(Route(name, arg))
    }

    fun back(): Boolean {
        if (stack.size > 1) { stack.removeAt(stack.lastIndex); return true }
        if (current.name != "home") { stack.clear(); stack.add(Route("home")); return true }
        return false
    }

    val isTab: Boolean get() = current.name in tabs
}
