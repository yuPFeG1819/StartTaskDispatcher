package com.yupfeg.dispatcher.monitor.delay

import com.yupfeg.dispatcher.task.TaskRunningInfo
import java.lang.StringBuilder

/**
 * 延迟任务的执行性能记录监听
 * */
interface OnDelayTaskRecordListener{
    /**
     * 在调度器执行完成后回调所有任务执行记录信息
     * - 在所有任务完成后回调，在当前looper循环所在线程执行
     * @param timeInfo 记录信息
     * */
    fun onAllTaskRecordResult(timeInfo: DelayTaskRecordInfo)
}

/**
 * 延迟任务的执行记录信息
 * */
data class DelayTaskRecordInfo(
    /**所有任务消耗合计时间(ms)*/
    val allTaskCostTime: Float,
    /**所有任务的耗时记录信息集合*/
    val allTaskRunInfoList: List<TaskRunningInfo>
){
    override fun toString(): String {
        val builder = StringBuilder()
        var totalTime = 0f
        for (entry in allTaskRunInfoList) {
            if (builder.isNotEmpty()) builder.append("\n")
            builder.append(entry.toString())
            totalTime += (entry.runTime)
        }
        return "all delay task cost time : ${String.format("%.2f",allTaskCostTime)} ms ,\n" +
                "$builder\n" +
                "serial execute delay task cost time : $totalTime ms"
    }
}