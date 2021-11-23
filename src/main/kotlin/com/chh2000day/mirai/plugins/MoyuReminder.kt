/*
 * MoyuReminder
 * Copyright (C)  2021  CHH2000day .
 * Permission is granted to copy, distribute and/or modify this document
 * under the terms of the GNU Free Documentation License, Version 1.3
 * or any later version published by the Free Software Foundation;
 * with no Invariant Sections, no Front-Cover Texts, and no Back-Cover Texts.
 * A copy of the license is included in the section entitled "GNU
 * Free Documentation License".
 */

package com.chh2000day.mirai.plugins

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info

object MoyuReminder : KotlinPlugin(
    JvmPluginDescription.loadFromResource()
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
    }
}