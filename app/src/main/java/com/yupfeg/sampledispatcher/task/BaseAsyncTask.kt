package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.executor.ExecutorProvider
import java.util.concurrent.ExecutorService

/**
 * 设置外部线程池的异步任务基类
 * @author yuPFeG
 * @date 2022/01/16
 */
abstract class BaseAsyncTask : Task(){

    override val isRunOnMainThread: Boolean
        get() = false

    override val dispatchOn: ExecutorService?
        get() = ExecutorProvider.getInstance().cpuExecutor

}