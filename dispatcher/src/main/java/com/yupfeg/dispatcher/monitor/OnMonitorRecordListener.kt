package com.yupfeg.dispatcher.monitor

import java.lang.StringBuilder

/**
 * 调度器性能监测回调监听
 * @author yuPFeG
 * @date 2022/01/08
 */
interface OnMonitorRecordListener {
    /**
     * 在调度器执行完成后回调所有记录信息
     * - 在所有任务完成后回调，在主线程执行
     * @param timeInfo 记录信息
     * */
    fun onMonitorRecordResult(timeInfo: ExecuteRecordInfo)
}

/**
 * 启动任务调度器的执行性能记录类
 * */
data class ExecuteRecordInfo(
    /**启动消耗时间(ms)*/
    val startCostTime : Long,
    /**主线程任务消耗时间(ms)*/
    val mainCostTime : Long,
    /**所有任务消耗时间(ms)*/
    val allTaskCostTime : Long,
    /**主线程等待的异步任务数量*/
    val waitAsyncTaskCount : Int,
    /**拓扑排序任务列表消耗时间(ms)*/
    val sortTaskCostTime : Long,
    /**所有任务的耗时记录*/
    val allTaskCostMap : Map<String,Long>
){
    override fun toString(): String {
        val builder = StringBuilder()
        var totalTime = 0L
        for (entry in allTaskCostMap) {
            if (builder.isNotEmpty()) builder.append("\n")
            builder.append("task tag : ${entry.key} , cost time : ${entry.value} ms")
            totalTime+=entry.value
        }
        return "real startTime : $startCostTime ms , \n" +
                "topological sort task list cost time : $sortTaskCostTime ms , \n" +
                "all main task cost time : $mainCostTime ms , \n" +
                "need main thread wait task count : $waitAsyncTaskCount , \n" +
                "real all task cost time : $allTaskCostTime ms ,\n$builder\n" +
                "serial total task cost time : $totalTime ms"
    }
}