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

import com.chh2000day.mirai.plugins.moyu.serializable.Config
import com.chh2000day.mirai.plugins.moyu.serializable.Festival
import com.chh2000day.mirai.plugins.moyu.serializable.FestivalDate
import com.chh2000day.mirai.plugins.moyu.serializable.SingleConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info
import java.io.File
import java.lang.Integer.max
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

typealias JVMDayOfWeek = java.time.DayOfWeek
typealias FestivalDeltaInfo = Pair<String, Int>

object MoyuReminder : KotlinPlugin(
    JvmPluginDescription.loadFromResource()
) {
    private const val millisecondsPerDay = 1000L * 60 * 60 * 24
    private const val startUpDelay = 30000L

    val templateRegex = Regex("(\\{.*?})")

    /**
     * 配置问价路径,不适用mirai的配置文件系统
     */
    private val confFile = File(configFolder, "config.json")
    private val json = Json { prettyPrint = true }
    private val timer = Timer()
    private val coroutineScope = CoroutineScope(coroutineContext)
    private var config: Config? = null

    override fun onEnable() {
        logger.info { "Plugin loaded" }
        init()
    }

    override fun onDisable() {
        super.onDisable()
        unregisterTasks()
    }

    @OptIn(ExperimentalTime::class)
    private fun registerTasks() {
        if (config == null) {
            logger.info("No config loader")
            return
        }
        for (singleConfig in config!!.subConfigs) {
            val sendTimeHour: Int
            val sendTimeMin: Int
            val sendTimeSec: Int
            //转换成HHMMSS的形式
            val sendTimeDigits = singleConfig.sendTime.filter {
                it.isDigit()
            }
            with(sendTimeDigits) {
                if (this.length != 6) {
                    logger.error("无效的发送时间:${singleConfig.sendTime}")
                    return
                }
                sendTimeHour = substring(0, 2).toInt()
                sendTimeMin = substring(2, 4).toInt()
                sendTimeSec = substring(4, 6).toInt()
            }
            //计算时间差
            val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            var sendTime = with(currentTime) {
                LocalDateTime(
                    year,
                    monthNumber,
                    dayOfMonth,
                    sendTimeHour,
                    sendTimeMin,
                    sendTimeSec,
                    nanosecond
                ).toInstant(
                    TimeZone.currentSystemDefault()
                )
            }
            var timeDiff = sendTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
            //至少离任务启动还有[startUpDelay]时间再启动任务
            if (timeDiff < startUpDelay) {
                timeDiff += millisecondsPerDay
                sendTime = sendTime.plus(Duration.Companion.days(1))
            }
            timer.scheduleAtFixedRate(Worker(singleConfig), timeDiff, millisecondsPerDay)
            logger.info("下次运行任务${singleConfig}的时间:${sendTime}")
        }
    }

    private fun unregisterTasks() {
        timer.cancel()
    }

    private fun init() {
        if (!confFile.exists()) {
            logger.info("Config file ${confFile.path} doesn't exist,creating sample")
            config = Config(listOf())
            writeSampleConfig()
        } else {
            loadConfig()
            registerTasks()
        }
    }

    private fun loadConfig() {
        kotlin.runCatching {
            val jsonString = confFile.readText(Charsets.UTF_8)
            config = json.decodeFromString(Config.serializer(), jsonString)
        }.onFailure {
            logger.error("Failed to load config", it)
        }
    }

    private fun writeSampleConfig() {
        kotlin.runCatching {
            val sampleFile = File(configFolder, "config.json.sample")
            val sampleConfig = Config(
                listOf(
                    SingleConfig(
                        groupList = listOf(10000),
                        festivalList = listOf(Festival("loli节", "1011")),
                        sendTime = "00:10:20"
                    )
                )
            )
            val sampleString = json.encodeToString(Config.serializer(), sampleConfig)
            sampleFile.writeText(sampleString, Charsets.UTF_8)
        }.onFailure {
            logger.error("Failed to create sample file", it)
        }
    }

    internal class Worker(private val singleConfig: SingleConfig) : TimerTask() {

        private val festivalDateList: List<FestivalDate> = singleConfig.festivalList.map(Festival::toFestivalDate)

        companion object {
            private val dayOfWeekList = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        }

        /**
         * The action to be performed by this timer task.
         */
        override fun run() {
            val currentDate = with(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())) {
                LocalDate(year, month, dayOfMonth)
            }

            val festivalToShowList = findAllFestivalsToShow(currentDate)
            val mainContent = parseMainContent(festivalToShowList)
            val stringToSend = singleConfig.template.parseTemplate {
                //大小写不敏感
                when (it) {
                    "{content}" -> {
                        mainContent
                    }
                    "{month}" -> {
                        currentDate.monthNumber.toString()
                    }
                    "{dayOfMonth}" -> {
                        currentDate.dayOfMonth.toString()
                    }
                    "{dayOfWeek}" -> {
                        dayOfWeekList[currentDate.dayOfWeek.ordinal]
                    }
                    "{dayOfYear}" -> {
                        currentDate.dayOfYear.toString()
                    }
                    else -> {
                        it
                    }
                }
            }
            //使用获取到的第一个bot实例发送
            coroutineScope.launch {
                val bot = Bot.instances.first()
                for (groupID in singleConfig.groupList) {
                    bot.getGroup(groupID).also {
                        if (it == null) {
                            logger.warning("bot:${bot.id}未加群:$groupID")
                        } else {
                            it.sendMessage(stringToSend)
                            delay(singleConfig.interval)
                        }
                    }
                }
            }
        }

        private fun findAllFestivalsToShow(currentDate: LocalDate): List<FestivalDeltaInfo> {
            /**
             * 计算与当前时间的差值(单位为天)
             */
            fun FestivalDate.calcDelta(): Int {
                var diff = currentDate.daysUntil(this.time)
                //处理已过去的节日,阈值为max(-1.5*maxNumOfDaysExceed+1,-90)
                if (diff < max((-1.5 * singleConfig.maxNumOfDaysExceed + 1).toInt(), -90)) {
                    diff = currentDate.daysUntil(this.time.plus(DatePeriod(years = 1)))
                }
                return diff
            }
            //与周末的差值
            val diffToWeekend = with(JVMDayOfWeek.SATURDAY.value - currentDate.dayOfWeek.value) {
                //处理周日的情况
                max(this, 0)
            }
            //与节假日的时间差
            val festivalDiffList = mutableListOf<Pair<String, Int>>()
            festivalDiffList.addAll(
                festivalDateList.map {
                    it.name to it.calcDelta()
                }
            )
            festivalDiffList.add("周末" to diffToWeekend)
            //排序.筛选
            return festivalDiffList.filter {
                it.second in -singleConfig.maxNumOfDaysExceed..singleConfig.maxNumOfDaysPrior
            }.sortedBy {
                it.second
            }
        }

        private fun parseMainContent(festivalToShowList: List<FestivalDeltaInfo>): String {
            return buildString {
                festivalToShowList.forEach {
                    append("距${it.first}还有:${it.second}天\n")
                }
            }
        }

        private inline fun String.parseTemplate(
            crossinline handler: (name: String) -> String
        ): String {
            var lastIndex = 0
            val stringBuilder = StringBuilder()
            for (result in templateRegex.findAll(this)) {
                if (result.range.first != lastIndex) {
                    stringBuilder.append(this.substring(lastIndex, result.range.first))
                }
                lastIndex = result.range.last + 1
                stringBuilder.append(handler(result.value))
            }
            if (lastIndex != this.length) {
                stringBuilder.append(handler(substring(lastIndex, this.length)))
            }
            return stringBuilder.toString()
        }
    }
}

