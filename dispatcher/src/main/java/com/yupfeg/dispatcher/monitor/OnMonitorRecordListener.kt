package com.yupfeg.dispatcher.monitor

import java.lang.StringBuilder

/**
 * 调度器性能监控回调监听
 * @author yuPFeG
 * @date 2022/01/08
 */
interface OnMonitorRecordListener {

    /**
     * 是否输出排序后的任务集合
     * */
    val isPrintSortedList : Boolean
        get() = false

    /**
     * 任务排序完成回调
     * @param tasksInfo 任务集合与依赖关系的遍历信息字符串，用于快捷检查依赖关系
     * */
    fun onTaskSorted(tasksInfo : String) = Unit

    /**
     * 在调度器执行完成后回调所有任务执行记录信息
     * - 在所有任务完成后回调，在主线程执行
     * @param timeInfo 记录信息
     * */
    fun onAllTaskRecordResult(timeInfo: ExecuteRecordInfo)
}

/**
 * 默认实现的调度器性能监控回调监听
 * */
class DefaultMonitorRecordListener : OnMonitorRecordListener{

    /**
     * 是否处于调试状态
     * 用于是否遍历输出所有任务的依赖关系，默认为false不会遍历输出依赖关系，提高性能
     * */
    @JvmField
    var isDebug : Boolean = false

    /**
     * 任务排序完成回调，返回整理后的所有任务与其中依赖关系的字符串
     * */
    @JvmField
    var onTaskSorted : ((String)->Unit)? = null

    override val isPrintSortedList: Boolean
        get() = isDebug

    /**
     * 所有任务完成后回调记录信息，在主线程运行
     */
    @JvmField
    var onAllTaskRecordResult : ((ExecuteRecordInfo)->Unit)? = null

    override fun onTaskSorted(tasksInfo: String) {
        onTaskSorted?.invoke(tasksInfo)
    }

    override fun onAllTaskRecordResult(timeInfo: ExecuteRecordInfo) {
        onAllTaskRecordResult?.invoke(timeInfo)
    }

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