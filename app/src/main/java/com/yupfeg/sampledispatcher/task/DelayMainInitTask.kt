package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.MainTask
import com.yupfeg.logger.ext.logd

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
        logd("DelayMainInitTask run")
        Thread.sleep(50)
    }
}