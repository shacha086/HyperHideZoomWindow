package com.shacha.hyperhidezoomwindow

object Util {
    @OptIn(ExperimentalStdlibApi::class)
    fun Any?.toIdentityString() =
        System.identityHashCode(this).toHexString()
}