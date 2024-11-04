package com.shacha.hyperhidezoomwindow

object Holder {
    val systemUIWindowDecorationList by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableListOf<Any>()
    }
}