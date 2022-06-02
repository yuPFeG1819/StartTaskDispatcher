package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.loggd


/**
 * 模拟初始化友盟统计任务
 * @author yuPFeG
 * @date
 */
class InitUMTask : Task(){
    companion object{
        const val TAG = "initUmengTask"
    }

    override val isEnable: Boolean
        get() = true

    override val tag: String
        get() = TAG

    override val isNeedWaitTaskOver: Boolean
        get() = true

    override fun run() {
        if (!isEnable) return
        loggd("initUmengTask run")
        Thread.sleep(500)
    }
}