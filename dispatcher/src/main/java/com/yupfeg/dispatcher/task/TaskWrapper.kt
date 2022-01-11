package com.yupfeg.dispatcher.task

import android.os.Process
import android.os.SystemClock
import com.yupfeg.dispatcher.TaskDispatcher

/**
 * 抽象启动任务的包装类，实际执行的任务
 * @author yuPFeG
 * @date 2022/01/05
 */
internal class TaskWrapper @JvmOverloads constructor(
    private val originTask : Task,
    private val dispatcher : TaskDispatcher? = null,
) : Runnable{

    override fun run() {
        Process.setThreadPriority(originTask.taskPriority())
        //等待前置任务完成
        originTask.onTaskWait(originTask.tag)
        val startTime = SystemClock.elapsedRealtime()
        originTask.awaitDependsTask()
        val waitTime = SystemClock.elapsedRealtime() - startTime
        originTask.onTaskStart(originTask.tag,waitTime)

        //执行当前任务
        val taskStartTime = SystemClock.elapsedRealtime()
        originTask.run()
        val runTime = SystemClock.elapsedRealtime() - taskStartTime

        //记录执行时间
        recordTaskRunningInfo(waitTime, runTime)
        dispatcher?.apply {
            taskExecuteMonitor.recordTaskCostTime(originTask.tag,runTime)
            //通知后续任务可以执行
            dispatcher.markTaskOverDone(originTask)
        }
    }

    /**
     * 记录任务执行信息
     * @param waitTime 等待开始执行时间
     * @param runTime 任务实际执行时间
     * */
    private fun recordTaskRunningInfo(waitTime : Long, runTime : Long){
        if (!originTask.isMonitorTaskOver) return
        val runningInfo = TaskRunningInfo(
            tag = originTask.tag,
            waitTime = waitTime,
            runTime = runTime,
            isNeedMainWait = originTask.isNeedMainWaitOver(),
            threadId = Thread.currentThread().id,
            threadName = Thread.currentThread().name
        )
        originTask.onTaskFinish(runningInfo)
    }

}