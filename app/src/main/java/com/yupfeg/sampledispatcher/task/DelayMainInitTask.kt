package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.MainTask
import com.yupfeg.logger.ext.loggd

/**
 * 模拟主线程延迟任务
 * @author yuPFeG
 * @date
 */
class DelayMainInitTask : MainTask() {

    companion object{
        const val TAG = "DelayMainInitTask"
    }

    override val tag: String
        get() = TAG

    override fun run() {
        loggd("DelayMainInitTask run")
        Thread.sleep(50)
    }
}