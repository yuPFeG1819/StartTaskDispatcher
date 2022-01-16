package com.yupfeg.sampledispatcher.task

import com.yupfeg.logger.ext.logd

/**
 * 模拟初始化分享组件的任务
 * @author yuPFeG
 * @date
 */
class InitSharedTask : BaseAsyncTask() {

    companion object{
        const val TAG = "InitSharedTask"
    }

    override val tag: String
        get() = TAG

    override fun run() {
        logd("InitSharedTask run")
        Thread.sleep(100)
    }
}