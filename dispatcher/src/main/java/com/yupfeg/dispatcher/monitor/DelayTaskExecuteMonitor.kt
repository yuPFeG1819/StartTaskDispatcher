package com.yupfeg.dispatcher.monitor

import android.os.Looper
import java.lang.StringBuilder

/**
 * 延迟任务的执行性能监控类
 * @author yuPFeG
 * @date 2022/06/01
 */
class DelayTaskExecuteMonitor(
    private val onRecordListener: OnTaskRecordListener?
) : ITaskExecuteMonitor{

    /**
     * 记录所有任务的消耗时间Map
     * * key - 任务标识 , value - 消耗时间(ms)
     */
    private val mTaskExecuteCostTimeMap = HashMap<String, Float>()

    /**
     * 所有主线程任务完成消耗时间(ms)
     * */
    private var mAllMainTaskCostTime: Float = 0f

    override fun recordTaskCostTime(tag: String, runTime: Float) {
        synchronized(this){
            mTaskExecuteCostTimeMap[tag] = runTime
            if (Looper.getMainLooper().thread == Thread.currentThread()) {
                //当前任务处于主线程，累加主线程执行任务的总时间
                mAllMainTaskCostTime += runTime
            }
        }
    }

    /**
     * 分发调度任务执行记录信息
     * */
    override fun dispatchExecuteRecordInfo() {
        onRecordListener?.onAllTaskRecordResult(
            TaskRecordInfo(
                allTaskCostTime = mAllMainTaskCostTime,
                allTaskCostMap = mTaskExecuteCostTimeMap
            )
        )
    }

    /**
     * 主线程延迟任务执行记录监听
     * */
    interface OnTaskRecordListener{
        /**
         * 在调度器执行完成后回调所有任务执行记录信息
         * - 在所有任务完成后回调，在主线程执行
         * @param timeInfo 记录信息
         * */
        fun onAllTaskRecordResult(timeInfo: TaskRecordInfo)
    }

    /**
     * 延迟任务的执行记录信息
     * */
    data class TaskRecordInfo(
        /**所有任务消耗合计时间(ms)*/
        val allTaskCostTime: Float,
        /**所有任务的耗时记录map*/
        val allTaskCostMap: Map<String, Float>
    ){
        override fun toString(): String {
            val builder = StringBuilder()
            var totalTime = 0f
            for (entry in allTaskCostMap) {
                if (builder.isNotEmpty()) builder.append("\n")
                builder.append("task tag : ${entry.key} , cost time : ${entry.value} ms")
                totalTime += entry.value
            }
            return "real all task cost time : $allTaskCostTime ms ,\n$builder\n" +
                    "serial total task cost time : $totalTime ms"
        }
    }
}