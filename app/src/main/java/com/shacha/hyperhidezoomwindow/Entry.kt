package com.shacha.hyperhidezoomwindow

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.github.kyuubiran.ezxhelper.paramTypes
import com.github.kyuubiran.ezxhelper.params
import com.shacha.hyperhidezoomwindow.Holder.systemUIWindowDecorationList
import com.shacha.hyperhidezoomwindow.Util.toIdentityString
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class Entry : IXposedHookLoadPackage {
    @Throws(ClassNotFoundException::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                // Part 1: Hide it!
                loadClass(
                    "com.android.server.wm.WindowSurfaceController", lpparam.classLoader
                ).constructorFinder().filterByParamTypes(
                    String::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    null, //"com.android.server.wm.WindowStateAnimator"
                    Int::class.javaPrimitiveType
                ).first().createHook {
                    before { param ->
                        val windowStateAnimator = param.args[3]
                        val windowState = windowStateAnimator.objectHelper().getObjectOrNull("mWin")
                        windowState?.objectHelper {
                            val isFreeformWindow = invokeMethodBestMatch(
                                "inFreeformWindowingMode"
                            ) as Boolean
//                            val owningPackage = invokeMethodBestMatch(
//                                "getOwningPackage"
//                            ) as String
                            val typeName = invokeMethodBestMatch("getWindowTag").toString()
                            var hookEnable = false
                            if ("InputMethod" == typeName) {
                                val target = invokeMethodBestMatch("getImeInputTarget")
                                target?.objectHelper {
                                    if (invokeMethodBestMatch(
                                            "inFreeformWindowingMode",
                                            Boolean::class.javaPrimitiveType
                                        ) as Boolean
                                    ) {
                                        hookEnable = true
                                    }
                                }

                            } else if (isFreeformWindow || "ScreenshotAnimation" == typeName || "com.miui.securitycenter/.FloatingWindow" == typeName) {
                                hookEnable = true
                            }

                            if (isEnabled && hookEnable) {
                                val flags = param.args[2] as Int
                                param.args[2] = flags or 0x00000040
                            }
                        }
                    }
                }


                // Part2: Do not render shadow behind zoom window, otherwise it'll still appear.
//        XposedHelpers.findAndHookMethod("com.android.server.wm.Task",
//                lpparam.classLoader, "getShadowRadius", boolean.class,
//                XC_MethodReplacement.returnConstant(0.0f));

                // Part3: Rebuild SurfaceController on window mode changes to make sure Part1 will get
                // applied instantly.
                lpparam.rebuildSurfaceController()

                @Suppress("DEPRECATION")
                xSharedPreferences?.registerOnSharedPreferenceChangeListener { _, _ ->
                    lpparam.rebuildSurfaceController()
                }
            }

            "com.android.systemui" -> {

                // Hide background
                @Suppress("LocalVariableName") val MiuiBaseWindowDecoration = loadClass(
                    "com.android.wm.shell.miuimultiwinswitch.miuiwindowdecor.MiuiBaseWindowDecoration",
                    lpparam.classLoader
                )
                loadClass("com.android.systemui.SystemUIApplication", lpparam.classLoader).methodFinder().filterByName("onCreate").first()
                    .createHook {
                        after { param ->
                            val application = param.thisObject as Application
                            application.registerReceiver(object : BroadcastReceiver() {
                                override fun onReceive(context: Context, intent: Intent) {
                                    if (intent.action != BROADCAST_ACTION) {
                                        return
                                    }
                                    systemUIWindowDecorationList.forEach {
                                        MiuiBaseWindowDecoration.getDeclaredField("mTaskSurface").apply { isAccessible = true }.get(it)!!
                                            .objectHelper().setSkipScreenshot(intent.getBooleanExtra("enabled", false))
                                    }
                                }

                            }, IntentFilter(BROADCAST_ACTION))

                        }
                    }
                MiuiBaseWindowDecoration.constructorFinder().filterByParamCount(14).first()
                    .createHook {
                        before { param ->
                            if (!isEnabled) {
                                return@before
                            }
                            param.args[4].objectHelper {
                                setSkipScreenshot(true)
                            }
                        }
                        after { param ->
                            XposedBridge.log("hooked MiuiBaseWindowDecoration")
                            val self = param.thisObject
                            systemUIWindowDecorationList.add(self)
                        }
                    }

                XposedBridge.log("Registered PreferenceChangeListener")
                // may useless?
                loadClass(
                    "com.android.wm.shell.miuimultiwinswitch.miuiwindowdrag.MiuiCoverLayerController",
                    lpparam.classLoader
                )

                    .constructorFinder().first().createHook {
                        after { param ->
                            XposedBridge.log("MiuiCoverLayerController: $isEnabled")
                            if (!isEnabled) {
                                return@after
                            }
                            param.thisObject.objectHelper {
                                hookLayerController(true)
                            }
                        }
                    }
            }
        }
    }

    private fun ObjectHelper.setSkipScreenshot(enabled: Boolean) {
        invokeMethod(
            "setSkipScreenshot", null, paramTypes(Boolean::class.javaPrimitiveType), params(enabled)
        )
    }

    private fun ObjectHelper.hookLayerController(enabled: Boolean) {
        getObjectOrNull("mColorLayer")?.objectHelper()?.setSkipScreenshot(enabled)
        getObjectOrNull("mShapeLayer")?.objectHelper()?.setSkipScreenshot(enabled)
    }

    private fun LoadPackageParam.rebuildSurfaceController() {
        XposedHelpers.findAndHookMethod("com.android.server.wm.Task",
            classLoader,
            "setWindowingModeInSurfaceTransaction",
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val windowState = XposedHelpers.callMethod(
                        param.thisObject, "getTopVisibleAppMainWindow"
                    )
                    val activityRecord = XposedHelpers.callMethod(
                        param.thisObject, "getTopVisibleActivity"
                    )
                    val rootWindowContainer = XposedHelpers.getObjectField(
                        param.thisObject, "mRootWindowContainer"
                    )
                    if (windowState == null) return
                    XposedHelpers.callMethod(windowState, "destroySurfaceUnchecked")
                    XposedHelpers.callMethod(activityRecord, "stopIfPossible")
                    XposedHelpers.callMethod(
                        rootWindowContainer, "resumeFocusedTasksTopActivities"
                    )
                }
            })
    }

    private val xSharedPreferences
        get() = XSharedPreferences(BuildConfig.APPLICATION_ID, "settings").let {
            if (it.file.canRead()) it else {
                if (!it.makeWorldReadable()) {
                    null
                } else it
            }
        }


    private val isEnabled: Boolean
        get() = xSharedPreferences?.getBoolean("enabled", false) ?: false

    companion object {
        const val TAG: String = "HyperHideZoomWindow"
    }
}
