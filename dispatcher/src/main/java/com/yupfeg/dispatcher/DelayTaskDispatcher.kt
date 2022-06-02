package com.yupfeg.dispatcher

import android.content.Context
import android.os.Looper
import android.os.MessageQueue
import androidx.annotation.MainThread
import com.yupfeg.dispatcher.monitor.delay.DelayTaskExecuteMonitor
import com.yupfeg.dispatcher.monitor.delay.OnDelayTaskRecordListener
import com.yupfeg.dispatcher.task.OnTaskStateListener
import com.yupfeg.dispatcher.task.Task
import com.yupfeg.dispatcher.task.TaskWrapper
import java.util.*
import kotlin.properties.Delegates

/**
 * 延迟任务调度器
 * * 仅在主线程执行任务
 * * 注意，使用该调度器会忽略所有任务的前置任务，如有并发需求可以使用[TaskDispatcher]进行并发任务调度
 * @author yuPFeG
 * @date 2021/12/11
 */
@Suppress("unused")
class DelayTaskDispatcher private constructor(builder: Builder) : ITaskDispatcher {

    private var mTaskQueue: Queue<Task> = builder.taskQueue

    private var mTaskMonitor: DelayTaskExecuteMonitor = builder.taskMonitor

    /**
     * 任务调度器的执行状态监听
     * */
    private val mDispatcherStateListener: OnDispatcherStateListener? =
        builder.onDispatcherStatusListener

    /**是否处于正在运行状态*/
    @Suppress("MemberVisibilityCanBePrivate")
    var isRunning: Boolean = false
        private set

    private val mIdleHandler = MessageQueue.IdleHandler {
        // 分批执行的好处在于每一个task占用主线程的时间相对
        // 来说很短暂，并且此时CPU是空闲的，这些能更有效地避免UI卡顿
        pollNextEnableTask()?.also { task ->
            TaskWrapper(task, mTaskMonitor, this).run()
        }
        //如果所有延迟启动任务都结束，则移除该IdleHandler
        return@IdleHandler !mTaskQueue.isEmpty()
    }

    /**
     * 取出下一个可执行的延迟任务
     * */
    private fun pollNextEnableTask(): Task? {
        while (mTaskQueue.isNotEmpty()) {
            val newTask = mTaskQueue.poll()
            newTask ?: return null
            if (!newTask.isEnable) continue
            return newTask
        }
        return null
    }

    /**
     * 开启执行延迟任务
     * - 只能在looper循环所在的线程开启
     * */
    fun start() {
        if (Looper.myLooper()?.thread != Thread.currentThread()) {
            throw RuntimeException("must be called from looper thread")
        }

        if (isRunning) return
        isRunning = true
        //执行任务开始前的Head Task
        mDispatcherStateListener?.onStartBefore()
        Looper.myQueue().addIdleHandler(mIdleHandler)
    }

    /**
     * 清空所有延迟任务
     * - 通常在视图销毁时调用，防止任务未执行完造成内存泄漏
     * */
    @MainThread
    fun clearTask() {
        if (!isRunning) return
        mTaskQueue.clear()
        isRunning = false
        Looper.myQueue().removeIdleHandler(mIdleHandler)
    }

    /**
     * 标记指定任务已完成
     * @param task 已完成的任务
     * */
    override fun markTaskOverDone(task: Task) {
        if (mTaskQueue.isNotEmpty()) return
        //所有任务已完成
        isRunning = false
        mTaskMonitor.dispatchExecuteRecordInfo()
        //在所有任务执行完成后的Tail Task
        mDispatcherStateListener?.onFinish()
    }

    /**
     * 延迟任务调度器构筑器
     * - 不允许添加依赖任务，延迟任务只是在主线程空闲时执行
     * */
    class Builder(private val context: Context) {

        internal val taskQueue: Queue<Task> = LinkedList()

        /**每个任务执行状态监听*/
        private var mTaskExecuteListener: OnTaskStateListener? = null

        /**任务执行性能监控器*/
        internal var taskMonitor: DelayTaskExecuteMonitor by Delegates.notNull()

        /**任务执行监控回调*/
        private var mMonitorRecordListener: OnDelayTaskRecordListener? = null

        /**
         * 调度器状态回调监听
         * */
        internal var onDispatcherStatusListener: OnDispatcherStateListener? = null

        /**
         * 设置每个任务执行状态监听
         * @param listener
         * @return 延迟任务调度器本身，便于链式调用
         * */
        fun setOnTaskStateListener(listener: OnTaskStateListener): Builder {
            mTaskExecuteListener = listener
            return this
        }

        /**
         * 设置任务执行记录回调监听
         * @param listener 性能监控记录回调
         * */
        fun setOnMonitorRecordListener(
            listener: OnDelayTaskRecordListener
        ): Builder {
            mMonitorRecordListener = listener
            return this
        }

        /**
         * 设置调度器运行状态监听
         * @param listener 调度器状态回调
         * @return builder类型，便于链式调用
         * */
        @Suppress("unused")
        fun setOnDispatcherStateListener(
            listener: OnDispatcherStateListener
        ): Builder {
            this.onDispatcherStatusListener = listener
            return this
        }

        /**
         * 添加延迟任务
         * @param task
         * @return 延迟任务调度器本身，便于链式调用
         * */
        fun addTask(task: Task): Builder {
            task.setContext(context.applicationContext)
            taskQueue.offer(task)
            return this
        }

        /**
         * 构建延迟任务调度器
         * */
        @MainThread
        fun builder(): DelayTaskDispatcher {
            for (task in taskQueue) {
                task.setTaskStateListener(mTaskExecuteListener)
            }
            taskMonitor = DelayTaskExecuteMonitor(mMonitorRecordListener)
            return DelayTaskDispatcher(this)
        }
    }
}