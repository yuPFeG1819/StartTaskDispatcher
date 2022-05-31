package com.yupfeg.dispatcher.task

import android.os.Process
import com.yupfeg.dispatcher.TaskDispatcher
import com.yupfeg.dispatcher.monitor.TaskExecuteMonitor

/**
 * 抽象启动任务的包装类，实际执行的任务
 * @author yuPFeG
 * @date 2022/01/05
 */
internal class TaskWrapper @JvmOverloads constructor(
    private val originTask: Task,
    private val dispatcher: TaskDispatcher? = null,
) : Runnable {

    override fun run() {
        Process.setThreadPriority(originTask.taskPriority())
        originTask.onTaskWait(originTask.tag)
        val waitTime = TaskExecuteMonitor.measureTime {
            //等待前置任务完成
            originTask.awaitDependsTask()
        }
        originTask.onTaskStart(originTask.tag, waitTime)
        val runTime = TaskExecuteMonitor.measureTime {
            //执行当前任务
            originTask.run()
        }
        //记录任务执行时间
        recordTaskRunningInfo(waitTime, runTime)
        dispatcher?.apply {
            taskExecuteMonitor.recordTaskCostTime(
                originTask.tag, runTime, originTask.isRunOnMainThread
            )
            //通知后续任务可以执行
            markTaskOverDone(originTask)
        }
    }

    /**
     * 记录任务执行信息
     * @param waitTime 等待开始执行时间
     * @param runTime 任务实际执行时间
     * */
    private fun recordTaskRunningInfo(waitTime: Float, runTime: Float) {
        if (!originTask.isMonitorTaskOver) return
        val runningInfo = TaskRunningInfo(
            tag = originTask.tag,
            waitTime = waitTime,
            runTime = runTime,
            isNeedMainWait = originTask.isAsyncTaskNeedMainWaitOver(),
            threadId = Thread.currentThread().id,
            threadName = Thread.currentThread().name
        )
        originTask.onTaskFinish(runningInfo)
    }

}