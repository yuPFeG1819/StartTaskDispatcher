package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.loggd

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

    override val isEnable: Boolean
        get() = false

    //需要主线程等待
    override val isNeedWaitTaskOver: Boolean
        get() = true

    override fun run() {
        if (!isEnable) return
        loggd("InitBuglyTask run")
        Thread.sleep(300)
    }
}