package com.yupfeg.dispatcher

import com.yupfeg.dispatcher.task.Task

/**
 * 任务调度器的通用接口
 * @author yuPFeG
 * @date 2022/06/01
 */
interface ITaskDispatcher {

    /**
     * 是否支持等待前置依赖任务
     * */
    val isSupportAwaitDepends : Boolean

    /**
     * 标记指定任务已完成
     * @param task 已完成的任务
     * */
    fun markTaskOverDone(task: Task)
}