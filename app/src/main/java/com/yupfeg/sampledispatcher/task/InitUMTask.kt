package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.logd


/**
 * 模拟初始化友盟统计任务
 * @author yuPFeG
 * @date
 */
class InitUMTask : Task(){
    companion object{
        const val TAG = "initUmengTask"
    }

    override val tag: String
        get() = TAG

    override fun run() {
        logd("initUmengTask run")
        Thread.sleep(400)
    }
}