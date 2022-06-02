package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.MainTask
import com.yupfeg.logger.ext.loggd

/**
 * 未捕获异常监听的初始化任务
 * @author yuPFeG
 * @date
 */
class UncaughtCrashTask : MainTask() {
    companion object{
        const val TAG = "UncaughtCrashTask"
    }

    override val tag: String
        get() = TAG

    override val isNeedRunAsSoon: Boolean
        get() = true

    override fun run() {
        loggd("init UncaughtCrash Task")
        Thread.sleep(20)
    }
}