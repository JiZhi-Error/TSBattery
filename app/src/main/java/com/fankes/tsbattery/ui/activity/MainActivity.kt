/*
 * TSBattery - A new way to save your battery avoid cancer apps hacker it.
 * Copyright (C) 2017 Fankes Studio(qzmmcn@163.com)
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
 * This file is created by fankes on 2021/9/4.
 */
@file:Suppress("SetTextI18n", "LocalVariableName", "SameParameterValue")

package com.fankes.tsbattery.ui.activity

import android.content.ComponentName
import android.content.Intent
import android.view.HapticFeedbackConstants
import androidx.core.view.isVisible
import com.fankes.projectpromote.ProjectPromote
import com.fankes.tsbattery.R
import com.fankes.tsbattery.const.JumpEvent
import com.fankes.tsbattery.const.ModuleVersion
import com.fankes.tsbattery.const.PackageName
import com.fankes.tsbattery.databinding.ActivityMainBinding
import com.fankes.tsbattery.hook.entity.QQTIMHooker
import com.fankes.tsbattery.hook.entity.WeChatHooker
import com.fankes.tsbattery.ui.activity.base.BaseActivity
import com.fankes.tsbattery.utils.factory.appVersionBrandOf
import com.fankes.tsbattery.utils.factory.hideOrShowLauncherIcon
import com.fankes.tsbattery.utils.factory.isInstall
import com.fankes.tsbattery.utils.factory.isLauncherIconShowing
import com.fankes.tsbattery.utils.factory.openBrowser
import com.fankes.tsbattery.utils.factory.showDialog
import com.fankes.tsbattery.utils.factory.snake
import com.fankes.tsbattery.utils.tool.GithubReleaseTool
import com.fankes.tsbattery.wrapper.BuildConfigWrapper
import com.highcapable.yukihookapi.YukiHookAPI

class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {
        private const val QQ_SUPPORT_VERSION = "理论支持 8.0.0+ 及以上版本。"
        private const val TIM_SUPPORT_VERSION = "2+、3+ (并未完全测试每个版本)。"
        private const val WECHAT_SUPPORT_VERSION = "全版本仅支持基础省电，更多功能依然画饼。"
    }

    override fun onCreate() {
        /** 检查更新 */
        GithubReleaseTool.checkingForUpdate(context = this, ModuleVersion.NAME) { version, function ->
            binding.mainTextReleaseVersion.apply {
                text = "点击更新 $version"
                isVisible = true
                setOnClickListener { function() }
            }
        }
        /** 判断 Hook 状态 */
        if (YukiHookAPI.Status.isModuleActive) {
            binding.mainLinStatus.setBackgroundResource(R.drawable.bg_green_round)
            binding.mainImgStatus.setImageResource(R.drawable.ic_success)
            binding.mainTextStatus.text = "模块已激活"
            binding.mainTextApiWay.isVisible = true
            refreshActivateExecutor()
            /** 推广、恰饭 */
            ProjectPromote.show(activity = this, ModuleVersion.toString())
        } else
            showDialog {
                title = "模块没有激活"
                msg = "检测到模块没有激活，若你正在使用免 Root 框架例如 LSPatch、太极或无极，你可以忽略此提示。"
                confirmButton(text = "我知道了")
                noCancelable()
            }
        /** 设置安装状态 */
        binding.mainTextQqVer.text = PackageName.QQ.takeIf { isInstall(it) }?.let { appVersionBrandOf(it) } ?: "未安装"
        binding.mainTextTimVer.text = PackageName.TIM.takeIf { isInstall(it) }?.let { appVersionBrandOf(it) } ?: "未安装"
        binding.mainTextWechatVer.text = PackageName.WECHAT.takeIf { isInstall(it) }?.let { appVersionBrandOf(it) } ?: "未安装"
        /** 设置文本 */
        binding.mainTextVersion.text = "模块版本：${ModuleVersion.NAME}"
        /** 设置 CI 自动构建标识 */
        if (ModuleVersion.isCiMode)
            binding.mainTextReleaseVersion.apply {
                text = "CI ${ModuleVersion.GITHUB_COMMIT_ID}"
                isVisible = true
                setOnClickListener {
                    showDialog {
                        title = "CI 自动构建说明"
                        msg = """
                          你正在使用的是 CI 自动构建版本，Commit ID 为 ${ModuleVersion.GITHUB_COMMIT_ID}。
                          
                          它是由代码提交后自动触发并构建、自动编译发布的，并未经任何稳定性测试，使用风险自负。
                          
                          CI 构建的版本不支持太极 (也请不要提交 CI 版本的适配，因为它们是不稳定的)，你可以使用 LSPosed / LSPatch。
                        """.trimIndent()
                        confirmButton(text = "我知道了")
                        noCancelable()
                    }
                }
            }
        binding.mainQqItem.setOnClickListener {
            showDialog {
                title = "兼容的 QQ 版本"
                msg = QQ_SUPPORT_VERSION
                confirmButton(text = "我知道了")
            }
            /** 振动提醒 */
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        binding.mainTimItem.setOnClickListener {
            showDialog {
                title = "兼容的 TIM 版本"
                msg = TIM_SUPPORT_VERSION
                confirmButton(text = "我知道了")
            }
            /** 振动提醒 */
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        binding.mainWechatItem.setOnClickListener {
            showDialog {
                title = "兼容的微信版本"
                msg = WECHAT_SUPPORT_VERSION
                confirmButton(text = "我知道了")
            }
            /** 振动提醒 */
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        /** 快捷操作 QQ */
        binding.quickQqButton.setOnClickListener { startModuleSettings(PackageName.QQ) }
        /** 快捷操作 TIM */
        binding.quickTimButton.setOnClickListener { startModuleSettings(PackageName.TIM) }
        /** 快捷操作微信 */
        binding.quickWechatButton.setOnClickListener { startModuleSettings(PackageName.WECHAT) }
        /** 项目地址按钮点击事件 */
        binding.titleGithubIcon.setOnClickListener { openBrowser(url = "https://github.com/fankes/TSBattery") }
        /** 恰饭！ */
        binding.linkWithFollowMe.setOnClickListener {
            openBrowser(url = "https://www.coolapk.com/u/876977", packageName = "com.coolapk.market")
        }
        /** 设置桌面图标显示隐藏 */
        binding.hideIconInLauncherSwitch.isChecked = isLauncherIconShowing.not()
        binding.hideIconInLauncherSwitch.setOnCheckedChangeListener { btn, b ->
            if (btn.isPressed.not()) return@setOnCheckedChangeListener
            hideOrShowLauncherIcon(b)
        }
        /** 判断当前启动模式 */
        if (packageName != BuildConfigWrapper.APPLICATION_ID) {
            binding.quickActionItem.isVisible = false
            binding.displaySettingItem.isVisible = false
        }
    }

    /**
     * 启动模块设置界面
     * @param packageName 包名
     */
    private fun startModuleSettings(packageName: String) {
        if (isInstall(packageName)) runCatching {
            startActivity(Intent().apply {
                component = ComponentName(
                    packageName,
                    if (packageName != PackageName.WECHAT) QQTIMHooker.JumpActivityClassName else WeChatHooker.LauncherUIClassName
                )
                putExtra(JumpEvent.OPEN_MODULE_SETTING, YukiHookAPI.Status.compiledTimestamp)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.onFailure { snake(msg = "启动模块设置失败\n$it") } else snake(msg = "你没有安装此应用")
    }

    /** 刷新模块激活使用的方式 */
    private fun refreshActivateExecutor() {
        if (YukiHookAPI.Status.Executor.apiLevel > 0)
            binding.mainTextApiWay.text = "Activated by ${YukiHookAPI.Status.Executor.name} API ${YukiHookAPI.Status.Executor.apiLevel}"
        else binding.mainTextApiWay.text = "Activated by ${YukiHookAPI.Status.Executor.name}"
    }
}