package com.yupfeg.sampledispatcher.task

import com.yupfeg.logger.ext.logd


/**
 * 模拟初始化极光推送任务
 * @author yuPFeG
 * @date
 */
class InitJPushTask : BaseAsyncTask(){

    companion object{
        const val TAG = "InitJPushTask"
    }

    override val tag: String
        get() = TAG

    override fun run() {
        logd("InitJPushTask run")
        Thread.sleep(200)
    }
}