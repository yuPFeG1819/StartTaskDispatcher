package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.logd


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
        logd("InitJPushTask run")
        Thread.sleep(200)
    }
}