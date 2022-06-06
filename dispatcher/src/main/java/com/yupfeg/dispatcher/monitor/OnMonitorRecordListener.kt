package com.yupfeg.dispatcher.monitor

import androidx.annotation.MainThread
import com.yupfeg.dispatcher.task.TaskRunningInfo
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
    val isPrintSortedList: Boolean
        get() = false

    /**
     * 任务排序完成回调
     * @param tasksInfo 任务集合与依赖关系的遍历信息字符串，用于快捷检查依赖关系
     * */
    fun onTaskSorted(tasksInfo: String) = Unit

    /**
     * 主线程消耗时间记录回调
     * - 包括 排序、head任务、调度异步任务、主线程任务执行、主线程等待 的时间
     * @param costTime 调度器实际占用的主线程时间 (ms)
     * */
    fun onMainThreadCostRecord(costTime: Float)

    /**
     * 在调度器执行完成后回调所有任务执行记录信息
     * - 在所有任务完成后回调，已切换在主线程执行
     * @param timeInfo 记录信息
     * */
    @MainThread
    fun onAllTaskRecordResult(timeInfo: ExecuteRecordInfo)
}

/**
 * 默认实现的调度器性能监控回调监听
 * */
class DefaultMonitorRecordListener : OnMonitorRecordListener {

    /**
     * 是否调试任务排序
     * 用于是否遍历输出所有任务的依赖关系，默认为false不会遍历输出依赖关系，提高性能
     * */
    @JvmField
    var isDebugTaskSort: Boolean = false

    /**
     * 任务排序完成回调，返回整理后的所有任务与其中依赖关系的字符串
     * - 仅在[isDebugTaskSort] = true 时才会回调
     * */
    @JvmField
    var onTaskSorted: ((String) -> Unit)? = null

    override val isPrintSortedList: Boolean
        get() = isDebugTaskSort

    /**
     * 主线程总计消耗时间记录(ms)
     * - 包括 任务排序、head任务、调度异步任务、主线程任务执行、主线程等待 的时间
     * */
    var onMainThreadOverRecord: ((Float) -> Unit)? = null

    /**
     * 所有任务完成后回调记录信息，在主线程运行
     */
    @JvmField
    var onAllTaskRecordResult: ((ExecuteRecordInfo) -> Unit)? = null

    override fun onTaskSorted(tasksInfo: String) {
        onTaskSorted?.invoke(tasksInfo)
    }

    override fun onMainThreadCostRecord(costTime: Float) {
        onMainThreadOverRecord?.invoke(costTime)
    }

    override fun onAllTaskRecordResult(timeInfo: ExecuteRecordInfo) {
        onAllTaskRecordResult?.invoke(timeInfo)
    }

}

/**
 * 启动任务调度器的执行性能记录类
 * */
data class ExecuteRecordInfo(
    /**所有主线程任务消耗时间(ms)*/
    val allMainTaskCostTime: Float,
    /**所有任务的消耗时间(ms)*/
    val allTaskCostTime: Float,
    /**主线程等待的异步任务数量*/
    val waitAsyncTaskCount: Int,
    /**主线程等待时间(ms)*/
    val mainThreadWaitTime : Float,
    /**拓扑排序任务列表消耗时间(ms)*/
    val sortTaskCostTime: Float,
    /**所有任务的耗时记录信息集合*/
    val allTaskRunInfoList: List<TaskRunningInfo>
) {
    override fun toString(): String {
        val builder = StringBuilder()
        var totalTime = 0f
        for (entry in allTaskRunInfoList) {
            if (builder.isNotEmpty()) builder.append("\n")
            builder.append(entry.toString())
            totalTime += (entry.runTime + entry.waitTime)
        }
        return "sort task list cost time : ${String.format("%.2f",sortTaskCostTime)} ms , \n" +
                "all main task cost time : ${String.format("%.2f",allMainTaskCostTime)} ms , \n" +
                "need main thread wait task count : $waitAsyncTaskCount , \n" +
                "main thread await time : ${String.format("%.2f",mainThreadWaitTime)} ms , \n" +
                "all task total cost time : ${String.format("%.2f",allTaskCostTime)} ms ,\n" +
                "$builder \n" +
                "serial execute all task total cost time : ${String.format("%.2f",totalTime)} ms"
    }
}