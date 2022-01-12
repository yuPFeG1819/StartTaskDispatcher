package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.logd

/**
 * 模拟初始化Bulgy任务
 * @author yuPFeG
 * @date
 */
class InitBuglyTask : Task(){

    companion object{
        const val TAG = "InitBuglyTask"
    }

    override val tag: String
        get() = TAG

    //需要主线程等待
    override val isNeedWaitTaskOver: Boolean
        get() = true

    override fun run() {
        logd("InitBuglyTask run")
        Thread.sleep(100)
    }
}