package com.yupfeg.sampledispatcher.task

import com.yupfeg.logger.ext.logd

/**
 * 模拟异步初始化任务
 * @author yuPFeG
 * @date
 */
class AsyncInitTask : BaseAsyncTask(){
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
        logd("AsyncInitTask run")
        Thread.sleep(100)
    }

}