package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.loggd

/**
 * 模拟异步初始化任务
 * @author yuPFeG
 * @date
 */
class AsyncInitTask : Task(){
    companion object{
        const val TAG = "AsyncInitTask"
    }

    override val isNeedRunAsSoon: Boolean
        get() = true

    override val isOnlyMainProcess: Boolean
        get() = true

    override val tag: String
        get() = TAG

    override fun run() {
        loggd("AsyncInitTask run")
        Thread.sleep(50)
    }

}