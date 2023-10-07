/*
 * TSBattery - A new way to save your battery avoid cancer apps hacker it.
 * Copyright (C) 2017-2023 Fankes Studio(qzmmcn@163.com)
 * https://github.com/fankes/TSBattery
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2022/9/29.
 */
@file:Suppress("ConstPropertyName")

package com.fankes.tsbattery.hook.entity

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ServiceCompat
import androidx.fragment.app.Fragment
import com.fankes.tsbattery.R
import com.fankes.tsbattery.const.ModuleVersion
import com.fankes.tsbattery.const.PackageName
import com.fankes.tsbattery.data.ConfigData
import com.fankes.tsbattery.hook.HookEntry
import com.fankes.tsbattery.hook.factory.hookSystemWakeLock
import com.fankes.tsbattery.hook.factory.isQQNightMode
import com.fankes.tsbattery.hook.factory.jumpToModuleSettings
import com.fankes.tsbattery.hook.factory.startModuleSettings
import com.fankes.tsbattery.utils.factory.appVersionName
import com.fankes.tsbattery.utils.factory.dp
import com.highcapable.yukihookapi.hook.bean.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.buildOf
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.injectModuleAppResources
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.factory.processName
import com.highcapable.yukihookapi.hook.factory.registerModuleAppActivities
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.android.BuildClass
import com.highcapable.yukihookapi.hook.type.android.BundleClass
import com.highcapable.yukihookapi.hook.type.android.ContextClass
import com.highcapable.yukihookapi.hook.type.android.IntentClass
import com.highcapable.yukihookapi.hook.type.android.MessageClass
import com.highcapable.yukihookapi.hook.type.java.AnyArrayClass
import com.highcapable.yukihookapi.hook.type.java.AnyClass
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.CharSequenceClass
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.ListClass
import com.highcapable.yukihookapi.hook.type.java.LongType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.type.java.UnitType
import java.lang.reflect.Proxy

/**
 * Hook QQ、TIM
 */
object QQTIMHooker : YukiBaseHooker() {

    /** QQ、TIM 存在的类 */
    const val JumpActivityClassName = "${PackageName.QQ}.activity.JumpActivity"

    /** QQ、TIM 存在的类 */
    private val JumpActivityClass by lazyClassOrNull(JumpActivityClassName)

    /** QQ、TIM 存在的类 (NT 版本不再存在) */
    private val QQSettingSettingActivityClass by lazyClassOrNull("${PackageName.QQ}.activity.QQSettingSettingActivity")

    /** QQ 新版存在的类 (Pad 模式 - NT 版本不再存在) */
    private val QQSettingSettingFragmentClass by lazyClassOrNull("${PackageName.QQ}.fragment.QQSettingSettingFragment")

    /** QQ、TIM 存在的类 (NT 版本不再存在) */
    private val AboutActivityClass by lazyClassOrNull("${PackageName.QQ}.activity.AboutActivity")

    /** QQ 新版本存在的类 */
    private val GeneralSettingActivityClass by lazyClassOrNull("${PackageName.QQ}.activity.GeneralSettingActivity")

    /** QQ 新版本 (NT) 存在的类 */
    private val MainSettingFragmentClass by lazyClassOrNull("${PackageName.QQ}.setting.main.MainSettingFragment")

    /** QQ 新版本 (NT) 存在的类 */
    private val MainSettingConfigProviderClass by lazyClassOrNull("${PackageName.QQ}.setting.main.MainSettingConfigProvider")

    /** QQ、TIM 新版本存在的类 */
    private val FormSimpleItemClass by lazyClassOrNull("${PackageName.QQ}.widget.FormSimpleItem")

    /** QQ、TIM 旧版本存在的类 */
    private val FormCommonSingleLineItemClass by lazyClassOrNull("${PackageName.QQ}.widget.FormCommonSingleLineItem")

    /** QQ、TIM 存在的类 */
    private val CoreServiceClass by lazyClassOrNull("${PackageName.QQ}.app.CoreService")

    /** QQ、TIM 存在的类 */
    private val CoreService_KernelServiceClass by lazyClassOrNull("${PackageName.QQ}.app.CoreService\$KernelService")

    /** 根据多个版本存的不同的类 */
    private val BaseChatPieClass by lazyClassOrNull(
        VariousClass(
            "${PackageName.QQ}.activity.aio.core.BaseChatPie",
            "${PackageName.QQ}.activity.BaseChatPie"
        )
    )

    /** 一个内部进程的名称 (与 X5 浏览器内核有关) */
    private val privilegedProcessName = "$packageName:privileged_process"

    /** 默认的 [Configuration] */
    var baseConfiguration: Configuration? = null

    /**
     * 当前是否为 QQ
     * @return [Boolean]
     */
    private val isQQ get() = packageName == PackageName.QQ

    /**
     * 当前是否为 QQ 的 NT 版本
     *
     * 在 QQ NT 中 [AboutActivityClass] 已被移除 - 以此作为判断条件
     * @return [Boolean]
     */
    private val isQQNTVersion get() = isQQ && AboutActivityClass == null

    /** 当前宿主的版本 */
    private var hostVersionName = "<unknown>"

    /**
     * 通过 [Activity] or [Fragment] 实例得到上下文
     * @return [Activity] or null
     */
    private fun Any.compatToActivity() = if (this is Activity) this else current().method { name = "getActivity"; superClass() }.invoke()

    /**
     * 这个类 QQ 的 BaseChatPie 是控制聊天界面的
     *
     * 里面有两个随机混淆的方法 ⬇
     *
     * remainScreenOn、cancelRemainScreenOn
     *
     * 这两个方法一个是挂起电源锁常驻亮屏
     *
     * 一个是停止常驻亮屏
     *
     * 不由分说每个版本混淆的方法名都会变
     *
     * 所以说每个版本重新适配 - 也可以提交分支帮我适配
     *
     * - ❗Hook 错了方法会造成闪退！
     */
    private fun hookQQBaseChatPie() {
        if (isQQ) when (hostVersionName) {
            "8.0.0" -> {
                hookBaseChatPie("bq")
                hookBaseChatPie("aL")
            }
            "8.0.5", "8.0.7" -> {
                hookBaseChatPie("bw")
                hookBaseChatPie("aQ")
            }
            "8.1.0", "8.1.3" -> {
                hookBaseChatPie("bE")
                hookBaseChatPie("aT")
            }
            "8.1.5" -> {
                hookBaseChatPie("bF")
                hookBaseChatPie("aT")
            }
            "8.1.8", "8.2.0", "8.2.6" -> {
                hookBaseChatPie("bC")
                hookBaseChatPie("aT")
            }
            "8.2.7", "8.2.8", "8.2.11", "8.3.0" -> {
                hookBaseChatPie("bE")
                hookBaseChatPie("aV")
            }
            "8.3.5" -> {
                hookBaseChatPie("bR")
                hookBaseChatPie("aX")
            }
            "8.3.6" -> {
                hookBaseChatPie("cp")
                hookBaseChatPie("aX")
            }
            "8.3.9" -> {
                hookBaseChatPie("cj")
                hookBaseChatPie("aT")
            }
            "8.4.1", "8.4.5" -> {
                hookBaseChatPie("ck")
                hookBaseChatPie("aT")
            }
            "8.4.8", "8.4.10", "8.4.17", "8.4.18", "8.5.0" -> {
                hookBaseChatPie("remainScreenOn")
                hookBaseChatPie("cancelRemainScreenOn")
            }
            "8.5.5" -> {
                hookBaseChatPie("bT")
                hookBaseChatPie("aN")
            }
            "8.6.0", "8.6.5", "8.7.0", "8.7.5", "8.7.8", "8.8.0", "8.8.3", "8.8.5" -> {
                hookBaseChatPie("ag")
                hookBaseChatPie("ah")
            }
            "8.8.11", "8.8.12" -> {
                hookBaseChatPie("bc")
                hookBaseChatPie("bd")
            }
            "8.8.17", "8.8.20" -> {
                hookBaseChatPie("bd")
                hookBaseChatPie("be")
            }
            "8.8.23", "8.8.28" -> {
                hookBaseChatPie("bf")
                hookBaseChatPie("bg")
            }
            "8.8.33" -> {
                hookBaseChatPie("bg")
                hookBaseChatPie("bh")
            }
            "8.8.35", "8.8.38" -> {
                hookBaseChatPie("bi")
                hookBaseChatPie("bj")
            }
            "8.8.50" -> {
                hookBaseChatPie("bj")
                hookBaseChatPie("bk")
            }
            "8.8.55", "8.8.68", "8.8.80" -> {
                hookBaseChatPie("bk")
                hookBaseChatPie("bl")
            }
            "8.8.83", "8.8.85", "8.8.88", "8.8.90" -> {
                hookBaseChatPie("bl")
                hookBaseChatPie("bm")
            }
            "8.8.93", "8.8.95" -> {
                hookBaseChatPie("J3")
                hookBaseChatPie("S")
            }
            "8.8.98" -> {
                hookBaseChatPie("M3")
                hookBaseChatPie("S")
            }
            "8.9.0", "8.9.1", "8.9.2" -> {
                hookBaseChatPie("N3")
                hookBaseChatPie("S")
            }
            "8.9.3", "8.9.5" -> {
                hookBaseChatPie("H3")
                hookBaseChatPie("P")
            }
            "8.9.8", "8.9.10" -> {
                hookBaseChatPie("H3")
                hookBaseChatPie("N")
            }
            "8.9.13" -> {
                hookBaseChatPie("y3")
                hookBaseChatPie("H")
            }
            "8.9.15", "8.9.18", "8.9.19", "8.9.20" -> {
                hookBaseChatPie("w3")
                hookBaseChatPie("H")
            }
            "8.9.23", "8.9.25" -> {
                hookBaseChatPie("z3")
                hookBaseChatPie("H")
            }
            "8.9.28", "8.9.30", "8.9.33" -> {
                hookBaseChatPie("A3")
                hookBaseChatPie("H")
            }
            "8.9.35", "8.9.38", "8.9.50" -> {
                hookBaseChatPie("B3")
                hookBaseChatPie("H")
            }
            "8.9.53", "8.9.55", "8.9.58" -> {
                hookBaseChatPie("C3")
                hookBaseChatPie("H")
            }
            "8.9.63", "8.9.68" -> {
                hookBaseChatPie("t3")
                hookBaseChatPie("J")
            }
            "8.9.70", "8.9.71", "8.9.73", "8.9.75", "8.9.76" -> {
                hookBaseChatPie("u3")
                hookBaseChatPie("J")
            }
            "8.9.78", "8.9.80", "8.9.83" -> {
                hookBaseChatPie("v3")
                hookBaseChatPie("I")
            }
            else -> {
                HookEntry.isHookClientSupport = false
                YLog.warn("$hostVersionName not supported!")
            }
        }
    }

    /**
     * 拦截 [BaseChatPieClass] 的目标方法体封装
     * @param methodName 方法名
     */
    private fun hookBaseChatPie(methodName: String) {
        BaseChatPieClass?.method {
            name = methodName
            emptyParam()
            returnType = UnitType
        }?.hook()?.intercept()
    }

    /** Hook CoreService QQ、TIM */
    private fun hookCoreService() {
        CoreServiceClass?.apply {
            if (isQQ) {
                method {
                    name = "startTempService"
                }.ignored().hook().intercept()
                method {
                    name = "startCoreService"
                    param(BooleanType)
                }.ignored().hook().intercept()
                method {
                    name = "onStartCommand"
                    param(IntentClass, IntType, IntType)
                }.ignored().hook().replaceTo(any = 2)
            }
            method {
                name = "onCreate"
            }.ignored().hook().after {
                if (ConfigData.isEnableKillQQTimCoreService)
                    instance<Service>().apply {
                        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        YLog.debug("Shutdown CoreService OK!")
                    }
            }
        }
        CoreService_KernelServiceClass?.apply {
            method {
                name = "onCreate"
            }.ignored().hook().after {
                if (ConfigData.isEnableKillQQTimCoreServiceChild)
                    instance<Service>().apply {
                        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        YLog.debug("Shutdown CoreService\$KernelService OK!")
                    }
            }
            method {
                name = "onStartCommand"
                param(IntentClass, IntType, IntType)
            }.ignored().hook().replaceTo(any = 2)
        }
    }

    /** Hook QQ 不省电的功能 */
    private fun hookQQDisgusting() {
        if (isQQ.not()) return
        /**
         * 干掉消息收发功能的电源锁
         * 每个版本的差异暂未做排查
         * 旧版本理论上没有这个类
         */
        "${PackageName.QQ}.msf.service.y".toClassOrNull()
            ?.method {
                name = "a"
                param(StringClass, LongType)
                returnType = UnitType
            }?.ignored()?.hook()?.intercept()
        /**
         * 干掉自动上传服务的电源锁
         * 每个版本的差异暂未做排查
         */
        "com.tencent.upload.impl.UploadServiceImpl".toClassOrNull()
            ?.method {
                name = "acquireWakeLockIfNot"
            }?.ignored()?.hook()?.intercept()
        /**
         * Hook 掉一个一像素保活 Activity 真的我怎么都想不到讯哥的程序员做出这种事情
         * 这个东西经过测试会在锁屏的时候吊起来，解锁的时候自动 finish()，无限耍流氓耗电
         * 2022/1/25 后期查证：锁屏界面消息快速回复窗口的解锁后拉起保活界面，也是毒瘤
         */
        "${PackageName.QQ}.activity.QQLSUnlockActivity".toClassOrNull()
            ?.method {
                name = "onCreate"
                param(BundleClass)
            }?.ignored()?.hook {
                var origDevice = ""
                before {
                    /** 由于在 onCreate 里有一行判断只要型号是 xiaomi 的设备就开电源锁，所以说这里临时替换成菊花厂 */
                    origDevice = Build.MANUFACTURER
                    if (Build.MANUFACTURER.lowercase() == "xiaomi")
                        BuildClass.field { name = "MANUFACTURER" }.get().set("HUAWEI")
                }
                after {
                    instance<Activity>().finish()
                    /** 这里再把型号替换回去 - 不影响应用变量等 Xposed 模块修改的型号 */
                    BuildClass.field { name = "MANUFACTURER" }.get().set(origDevice)
                }
            }
        /**
         * 这个东西同上
         * 反正也是一个一像素保活的 Activity
         * 讯哥的程序员真的有你的
         * 2022/1/25 后期查证：锁屏界面消息快速回复窗口
         */
        VariousClass("${PackageName.QQ}.activity.QQLSActivity\$14", "ktq").toClassOrNull()
            ?.method {
                name = "run"
            }?.ignored()?.hook()?.intercept()
        /**
         * 这个是毒瘤核心类
         * WakeLockMonitor
         * 这个名字真的起的特别诗情画意
         * 带给用户的却是 shit 一样的体验
         * 里面有各种使用 Handler 和 Timer 的各种耗时常驻后台耗电办法持续接收消息
         * 直接循环全部方法全部干掉
         * 👮🏻 经过排查 Play 版本没这个类...... Emmmm 不想说啥了
         * ✅ 备注：8.9.x 版本已经基本移除了这个功能，没有再发现存在这个类
         */
        "com.tencent.qapmsdk.qqbattery.monitor.WakeLockMonitor".toClassOrNull()?.apply {
            method {
                name = "onHook"
                param(StringClass, AnyClass, AnyArrayClass, AnyClass)
            }.ignored().hook().intercept()
            method {
                name = "doReport"
                param("com.tencent.qapmsdk.qqbattery.monitor.WakeLockMonitor\$WakeLockEntity", IntType)
            }.ignored().hook().intercept()
            method {
                name = "afterHookedMethod"
                param("com.tencent.qapmsdk.qqbattery.monitor.MethodHookParam")
            }.ignored().hook().intercept()
            method {
                name = "beforeHookedMethod"
                param("com.tencent.qapmsdk.qqbattery.monitor.MethodHookParam")
            }.ignored().hook().intercept()
            method {
                name = "onAppBackground"
            }.ignored().hook().intercept()
            method {
                name = "onOtherProcReport"
                param(BundleClass)
            }.ignored().hook().intercept()
            method {
                name = "onProcessRun30Min"
            }.ignored().hook().intercept()
            method {
                name = "onProcessBG5Min"
            }.ignored().hook().intercept()
            method {
                name = "writeReport"
                param(BooleanType)
            }.ignored().hook().intercept()
        }
        /**
         * 这个是毒瘤核心操作类
         * 功能同上、全部拦截
         * 👮🏻 经过排查 Play 版本也没这个类...... Emmmm 不想说啥了
         * ✅ 备注：8.9.x 版本已经基本移除了这个功能，没有再发现存在这个类
         */
        "com.tencent.qapmsdk.qqbattery.QQBatteryMonitor".toClassOrNull()?.apply {
            method {
                name = "start"
            }.ignored().hook().intercept()
            method {
                name = "stop"
            }.ignored().hook().intercept()
            method {
                name = "handleMessage"
                param(MessageClass)
            }.ignored().hook().intercept()
            method {
                name = "startMonitorInner"
            }.ignored().hook().intercept()
            method {
                name = "onAppBackground"
            }.ignored().hook().intercept()
            method {
                name = "onAppForeground"
            }.ignored().hook().intercept()
            method {
                name = "setLogWhite"
                paramCount = 2
            }.ignored().hook().intercept()
            method {
                name = "setCmdWhite"
                paramCount = 2
            }.ignored().hook().intercept()
            method {
                name = "onWriteLog"
                param(StringClass, StringClass)
            }.ignored().hook().intercept()
            method {
                name = "onCmdRequest"
                param(StringClass)
            }.ignored().hook().intercept()
            method {
                name = "addData"
                paramCount = 4
            }.ignored().hook().intercept()
            method {
                name = "onGpsScan"
                paramCount = 2
            }.ignored().hook().intercept()
        }
    }

    /** Hook QQ 的设置界面添加模块设置入口 (新版) */
    private fun hookQQSettingsUi() {
        if (MainSettingFragmentClass == null) return YLog.error("Could not found main setting class, hook aborted")
        val kotlinUnit = "kotlin.Unit"
        val kotlinFunction0 = "kotlin.jvm.functions.Function0"
        val simpleItemProcessorClass = searchClass {
            from("${PackageName.QQ}.setting.processor").absolute()
            constructor { param(ContextClass, IntType, CharSequenceClass, IntType) }
            method {
                param(kotlinFunction0)
                returnType = UnitType
            }
            field().count { it >= 6 }
        }.get() ?: return YLog.error("Could not found processor class, hook aborted")

        /**
         * 创建入口点条目
         * @param context 当前实例
         * @return [Any]
         */
        fun createTSEntryItem(context: Context): Any {
            /** 为了使用图标资源 ID - 这里需要重新注入模块资源防止不生效 */
            context.injectModuleAppResources()
            val iconResId = if (context.isQQNightMode()) R.mipmap.ic_tsbattery_entry_night else R.mipmap.ic_tsbattery_entry_day
            return simpleItemProcessorClass.buildOf(context, R.id.tsbattery_qq_entry_item_id, "TSBattery", iconResId) {
                param(ContextClass, IntType, CharSequenceClass, IntType)
            }?.also { entryItem ->
                val onClickMethod = simpleItemProcessorClass.method {
                    param { it[0].name == kotlinFunction0 }
                    paramCount = 1
                    returnType = UnitType
                }.giveAll().firstOrNull() ?: error("Could not found processor method")
                val proxyOnClick = Proxy.newProxyInstance(appClassLoader, arrayOf(onClickMethod.parameterTypes[0])) { any, method, args ->
                    if (method.name == "invoke") {
                        context.startModuleSettings()
                        kotlinUnit.toClass().field { name = "INSTANCE" }.get().any()
                    } else method.invoke(any, args)
                }; onClickMethod.invoke(entryItem, proxyOnClick)
            } ?: error("Could not create TSBattery entry item")
        }
        MainSettingConfigProviderClass?.method {
            param(ContextClass)
            returnType = ListClass
        }?.hook()?.after {
            val context = args().first().cast<Context>() ?: return@after
            val processor = result<MutableList<Any?>>() ?: return@after
            processor.add(1, processor[0]?.javaClass?.buildOf(arrayListOf<Any>().apply { add(createTSEntryItem(context)) }, "", "") {
                param(ListClass, CharSequenceClass, CharSequenceClass)
            })
        }
    }

    /**
     * Hook QQ 的设置界面添加模块设置入口 (旧版)
     * @param instance 当前设置界面实例
     */
    private fun hookQQSettingsUiLegacy(instance: Any?) {
        /** 当前的顶级 Item 实例 */
        val formItemRefRoot = instance?.current()?.field {
            type { it == FormSimpleItemClass || it == FormCommonSingleLineItemClass }.index(num = 1)
        }?.cast<View?>()
        /** 创建一个新的 Item */
        FormSimpleItemClass?.buildOf<View>(instance?.compatToActivity()) { param(ContextClass) }?.current {
            method {
                name = "setLeftText"
                param(CharSequenceClass)
            }.call("TSBattery")
            method {
                name = "setRightText"
                param(CharSequenceClass)
            }.call(ModuleVersion.toString())
            method {
                name = "setBgType"
                param(IntType)
            }.call(if (isQQ) 0 else 2)
        }?.apply { setOnClickListener { context.startModuleSettings() } }?.also { item ->
            var listGroup = formItemRefRoot?.parent as? ViewGroup?
            val lparam = (if (listGroup?.childCount == 1) {
                listGroup = listGroup.parent as? ViewGroup
                (formItemRefRoot?.parent as? View?)?.layoutParams
            } else formItemRefRoot?.layoutParams)
                ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            /** 设置圆角和间距 */
            if (isQQ) (lparam as? ViewGroup.MarginLayoutParams?)?.setMargins(0, 15.dp(item.context), 0, 0)
            /** 将 Item 添加到设置界面 */
            listGroup?.also { if (isQQ) it.addView(item, lparam) else it.addView(item, 0, lparam) }
        }
    }

    override fun onHook() {
        onAppLifecycle(isOnFailureThrowToApp = false) {
            attachBaseContext { baseContext, hasCalledSuper ->
                if (hasCalledSuper.not()) baseConfiguration = baseContext.resources.configuration
            }
            onCreate {
                hostVersionName = appVersionName
                /** 不注入此进程防止部分系统发生 X5 浏览器内核崩溃问题 */
                if (processName.startsWith(privilegedProcessName)) return@onCreate
                ConfigData.init(context = this)
                registerModuleAppActivities(when {
                    isQQNTVersion -> GeneralSettingActivityClass
                    else -> AboutActivityClass
                })
                if (ConfigData.isDisableAllHook) return@onCreate
                hookSystemWakeLock()
                hookQQBaseChatPie()
                hookCoreService()
                hookQQDisgusting()
                YLog.info("All processes are completed for \"${processName.takeIf { it != packageName } ?: packageName}\"")
            }
        }
        /** 仅注入主进程 */
        withProcess(mainProcessName) {
            /** Hook 跳转事件 */
            JumpActivityClass?.method {
                name = "doOnCreate"
                param(BundleClass)
            }?.hook()?.after { instance<Activity>().jumpToModuleSettings() }
            /** Hook 设置界面入口点 */
            if (isQQNTVersion) hookQQSettingsUi()
            else {
                /** 将条目注入设置界面 (Activity) */
                QQSettingSettingActivityClass?.method {
                    name = "doOnCreate"
                    param(BundleClass)
                }?.hook()?.after { hookQQSettingsUiLegacy(instance) }
                /** 将条目注入设置界面 (Fragment) */
                QQSettingSettingFragmentClass?.method {
                    name = "doOnCreateView"
                    paramCount = 3
                }?.hook()?.after { hookQQSettingsUiLegacy(instance) }
            }
        }
    }
}