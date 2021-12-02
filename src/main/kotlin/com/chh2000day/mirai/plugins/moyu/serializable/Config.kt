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

package com.chh2000day.mirai.plugins.moyu.serializable

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * @Author CHH2000day
 * @Date 2021/11/26 19:18
 **/
@Serializable
data class Config(val subConfigs: List<SingleConfig>)


/**
 * @param groupList 需要启用的群组的列表
 * @param festivalList 手动指定的节假日列表
 * @param sendTime 开始发送的时间,格式为HH:MM:SS
 * @param maxNumOfDaysExceed 最大允许已超过节假日多少天(发送)
 * @param maxNumOfDaysPrior 最大允许离节假日多少天(发送)
 * @param interval 每条信息之间的发送间隔
 * @param template 模板,支持以下功能:
 *                  {content} 正文(距XX还有XX天)
 *                  {month} 当前月份
 *                  {dayOfMonth} 当前日
 *                  {dayOfWeek} 周X
 *                  {dayOfYear} 今年的第X天
 */
@Serializable
data class SingleConfig(
    val groupList: List<Long>,
    val festivalList: List<Festival>,
    val sendTime: String,
    val template: String = "{content}",
    val maxNumOfDaysExceed: Int = 0,
    val maxNumOfDaysPrior: Int = 300,
    val interval: Long = 3000
)

/**
 * @param name 节假日的名称
 * @param time 节假日时间,格式为 YYMMDD 或 MMDD
 */
@Suppress("SpellCheckingInspection")
@Serializable
data class Festival(val name: String, val time: String) {
    fun toFestivalDate(): FestivalDate {
        //兼容YY-MM-DD,MM-DD,YY/MM/DD,MM/DD等写法,但是不支持21-5-1/11-1-2这些写法
        val digits = time.filter {
            it.isDigit()
        }
        val yearInt: Int
        val monthInt: Int
        val dayInt: Int
        when (digits.length) {
            //MMDD
            4 -> {
                val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                yearInt = currentDate.year
                val monthStr = digits.substring(0, 2)
                val dayStr = digits.substring(2, 4)
                monthInt = monthStr.toInt()
                dayInt = dayStr.toInt()
            }
            //YYMMDD
            6 -> {
                val yearStr = digits.substring(0, 2)
                val monthStr = digits.substring(2, 4)
                val dayStr = digits.substring(4, 6)
                yearInt = yearStr.toInt()
                monthInt = monthStr.toInt()
                dayInt = dayStr.toInt()
            }
            else -> {
                throw IllegalArgumentException("Invalid date:$time")
            }
        }
        return FestivalDate(name, LocalDate(yearInt, monthInt, dayInt))
    }
}

data class FestivalDate(val name: String, val time: LocalDate)