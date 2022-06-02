package com.yupfeg.dispatcher.monitor

import android.os.SystemClock
import com.yupfeg.dispatcher.task.TaskRunningInfo

/**
 * 任务执行性能监控通用接口
 * @author yuPFeG
 * @date 2022/06/01
 */
internal interface ITaskExecuteMonitor {

    companion object {
        const val NANO_TIME_UNIT = 1000000f

        /**
         * 测量从指定开始时间到当前时间经过的时间(ms)
         * @param action 测量执行时间的代码块
         * @return 执行经过时间(ms)
         * */
        inline fun measureTime(action: () -> Unit): Float {
            val startTime = SystemClock.elapsedRealtimeNanos()
            action()
            return (SystemClock.elapsedRealtimeNanos() - startTime) / NANO_TIME_UNIT
        }
    }

    /**
     * 记录任务的运行信息
     * - 需要注意线程安全问题
     * @param runningInfo 任务实际执行时间（ms）
     * */
    fun recordTaskRunningInfo(runningInfo : TaskRunningInfo)

    /**
     * 分发调度任务执行记录信息
     * */
    fun dispatchExecuteRecordInfo()
}