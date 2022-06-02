package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.loggd


/**
 * 模拟初始化极光推送任务
 * @author yuPFeG
 * @date
 */
class InitJPushTask : Task(){

    companion object{
        const val TAG = "InitJPushTask"
    }

    override val isEnable: Boolean
        get() = false

    override val tag: String
        get() = TAG

    override fun run() {
        if (!isEnable) return
        loggd("InitJPushTask run")
        Thread.sleep(120)
    }
}