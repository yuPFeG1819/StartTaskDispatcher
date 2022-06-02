package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.loggd

/**
 * 模拟初始化分享组件的任务
 * @author yuPFeG
 * @date
 */
class InitSharedTask : Task() {

    companion object{
        const val TAG = "InitSharedTask"
    }

    override val tag: String
        get() = TAG

    override val isNeedWaitTaskOver: Boolean
        get() = false

    override fun run() {
        loggd("InitSharedTask run")
        Thread.sleep(80)
    }
}