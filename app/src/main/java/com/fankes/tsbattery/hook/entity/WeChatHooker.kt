/*
 * TSBattery - A new way to save your battery avoid cancer apps hacker it.
 * Copyright (C) 2019-2022 Fankes Studio(qzmmcn@163.com)
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
 * This file is Created by fankes on 2022/9/29.
 */
package com.fankes.tsbattery.hook.entity

import android.app.Activity
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.fankes.tsbattery.R
import com.fankes.tsbattery.const.PackageName
import com.fankes.tsbattery.data.ConfigData
import com.fankes.tsbattery.hook.factory.hookSystemWakeLock
import com.fankes.tsbattery.hook.factory.jumpToModuleSettings
import com.fankes.tsbattery.hook.factory.startModuleSettings
import com.fankes.tsbattery.utils.factory.absoluteStatusBarHeight
import com.fankes.tsbattery.utils.factory.dp
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.current
import com.highcapable.yukihookapi.hook.factory.injectModuleAppResources
import com.highcapable.yukihookapi.hook.log.loggerD
import com.highcapable.yukihookapi.hook.type.android.BundleClass

/**
 * Hook 微信
 *
 * 具体功能还在画饼
 */
object WeChatHooker : YukiBaseHooker() {

    /** 微信存在的类 - 未测试每个版本是否都存在 */
    const val LauncherUIClass = "${PackageName.WECHAT}.ui.LauncherUI"

    /** 微信存在的类 - 未测试每个版本是否都存在 */
    private const val SettingsUIClass = "${PackageName.WECHAT}.plugin.setting.ui.setting.SettingsUI"

    override fun onHook() {
        /** Hook 跳转事件 */
        LauncherUIClass.hook {
            injectMember {
                method {
                    name = "onResume"
                    emptyParam()
                }
                afterHook { instance<Activity>().jumpToModuleSettings(isFinish = false) }
            }
        }
        /** 向设置界面右上角添加按钮 */
        SettingsUIClass.hook {
            injectMember {
                method {
                    name = "onCreate"
                    param(BundleClass)
                }
                afterHook {
                    method {
                        name = "get_fragment"
                        emptyParam()
                        superClass(isOnlySuperClass = true)
                    }.get(instance).call()?.current()
                        ?.field { name = "mController" }
                        ?.current()?.method { name = "getContentView" }
                        ?.invoke<ViewGroup>()?.addView(LinearLayout(instance()).apply {
                            context.injectModuleAppResources()
                            gravity = Gravity.END or Gravity.BOTTOM
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            addView(ImageView(context).apply {
                                layoutParams = ViewGroup.MarginLayoutParams(20.dp(context), 20.dp(context)).apply {
                                    topMargin = context.absoluteStatusBarHeight + 15.dp(context)
                                    rightMargin = 20.dp(context)
                                }
                                setColorFilter(ResourcesCompat.getColor(resources, R.color.colorTextGray, null))
                                setImageResource(R.drawable.ic_icon)
                                if (Build.VERSION.SDK_INT >= 26) tooltipText = "TSBattery 设置"
                                setOnClickListener { context.startModuleSettings() }
                            })
                        })
                }
            }
        }
        if (ConfigData.isDisableAllHook) return
        /** Hook 系统电源锁 */
        hookSystemWakeLock()
        /** 日志省电大法 */
        loggerD(msg = "ウイチャット：それが機能するかどうかはわかりませんでした")
    }
}