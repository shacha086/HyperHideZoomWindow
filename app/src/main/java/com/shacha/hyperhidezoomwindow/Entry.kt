package com.shacha.hyperhidezoomwindow

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.paramTypes
import com.github.kyuubiran.ezxhelper.params
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class Entry : IXposedHookLoadPackage {
    @Throws(ClassNotFoundException::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                // Part 1: Hide it!
                loadClass(
                    "com.android.server.wm.WindowSurfaceController",
                    lpparam.classLoader
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

                            if (hookEnable) {
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
                XposedHelpers.findAndHookMethod("com.android.server.wm.Task",
                    lpparam.classLoader,
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

            "com.android.systemui" -> {
                // Hide background
                loadClass(
                    "com.android.wm.shell.miuimultiwinswitch.miuiwindowdecor.MiuiBaseWindowDecoration",
                    lpparam.classLoader
                ).constructorFinder().filterByParamCount(14).first().createHook {
                    before { param ->
                        param.args[4].objectHelper {
                            invokeMethod(
                                "setSkipScreenshot",
                                null,
                                paramTypes(Boolean::class.javaPrimitiveType),
                                params(true)
                            )
                        }
                    }
                }

                // may useless?
                loadClass(
                    "com.android.wm.shell.miuimultiwinswitch.miuiwindowdrag.MiuiCoverLayerController",
                    lpparam.classLoader
                ).constructorFinder().first().createHook {
                    after { param ->
                        param.thisObject.objectHelper {
                            getObjectOrNull("mColorLayer")?.objectHelper {
                                invokeMethod(
                                    "setSkipScreenshot",
                                    null,
                                    paramTypes(Boolean::class.javaPrimitiveType),
                                    params(true)
                                )
                            }
                            getObjectOrNull("mShapeLayer")?.objectHelper {
                                invokeMethod(
                                    "setSkipScreenshot",
                                    null,
                                    paramTypes(Boolean::class.javaPrimitiveType),
                                    params(true)
                                )
                            }
                        }
                    }
                }
            }
        }


    }

    companion object {
        const val TAG: String = "HyperHideZoomWindow"
    }
}
