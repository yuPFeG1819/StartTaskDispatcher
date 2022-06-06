package com.yupfeg.dispatcher

import android.content.Context
import android.os.Looper
import android.os.MessageQueue
import androidx.annotation.MainThread
import com.yupfeg.dispatcher.monitor.delay.DelayTaskExecuteMonitor
import com.yupfeg.dispatcher.monitor.delay.OnDelayTaskRecordListener
import com.yupfeg.dispatcher.task.OnTaskStatusListener
import com.yupfeg.dispatcher.task.Task
import com.yupfeg.dispatcher.task.TaskWrapper
import java.util.*
import kotlin.properties.Delegates

/**
 * 延迟任务调度器
 * * 通常仅在主线程的cpu空闲时间分批执行任务，如需在其他线程启动，则需要在其他调用`Looper.loop`开启消息队列循环
 * * 按任务队列的添加顺序倒序执行，最后提交的任务后执行
 * * 注意，使用该调度器会忽略所有任务的前置任务，如有并发需求可以使用[TaskDispatcher]进行并发任务调度
 * @author yuPFeG
 * @date 2021/12/11
 */
@Suppress("unused")
class DelayTaskDispatcher private constructor(builder: Builder) : ITaskDispatcher {

    private val mTaskQueue: Deque<Task> = builder.taskQueue

    private val mTaskMonitor: DelayTaskExecuteMonitor = builder.taskMonitor

    /**
     * 任务调度器的执行状态监听
     * */
    private val mDispatcherStateListener: OnDispatcherStateListener? =
        builder.onDispatcherStatusListener

    /**每个任务执行状态监听*/
    private var mTaskExecuteListener: OnTaskStatusListener? = builder.taskExecuteListener

    /**是否处于正在运行状态*/
    @Suppress("MemberVisibilityCanBePrivate")
    var isRunning: Boolean = false
        private set

    /**
     * 是否支持等待前置依赖任务
     * */
    override val isSupportAwaitDepends: Boolean
        get() = false

    private val mIdleHandler = MessageQueue.IdleHandler {
        // 分批执行的好处在于每一个task占用主线程的时间相对
        // 来说很短暂，并且此时CPU是空闲的，这些能更有效地避免UI卡顿
        pollNextEnableTask()?.also { task ->
            TaskWrapper(task, mTaskMonitor, this).run()
        }
        val isTaskOver = mTaskQueue.isEmpty()
        if (isTaskOver) isRunning = false
        //如果所有延迟启动任务都结束，则移除该IdleHandler
        return@IdleHandler !isTaskOver
    }

    /**
     * 取出下一个可执行的延迟任务
     * */
    private fun pollNextEnableTask(): Task? {
        while (mTaskQueue.isNotEmpty()) {
            val newTask = mTaskQueue.poll()
            newTask ?: continue
            if (!newTask.isEnable) continue
            return newTask
        }
        return null
    }

    /**
     * 添加额外的延迟任务
     * - 所有任务执行时间尽可能控制在100ms以内，否则会导致后续任务的执行时机异常，只能在放置于后台任务后才执行
     * - 如果调度器已结束或未开启，则需要重新调用[start]方法重新启动调度器，才会执行新添加的任务
     * @param context
     * @param task 需要添加执行的额外任务，注意任务耗时情况
     * @return true-表示成功添加任务
     * */
    fun addTask(context: Context, task: Task): Boolean {
        task.setContext(context)
        task.setTaskStateListener(mTaskExecuteListener)
        return mTaskQueue.offer(task)
    }

    /**
     * 开启执行延迟任务
     * - 只能在looper循环所在的线程开启
     * */
    fun start() {
        if (Looper.myLooper()?.thread != Thread.currentThread()) {
            throw RuntimeException("must be called from looper thread , " +
                    "or use looper.loop start message queue")
        }

        if (isRunning) return
        isRunning = true
        //执行任务开始前的Head Task
        mDispatcherStateListener?.onStartBefore()
        mTaskMonitor.resetTaskExecuteRecord()
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

        internal val taskQueue: Deque<Task> = ArrayDeque()

        /**每个任务执行状态监听*/
        internal var taskExecuteListener: OnTaskStatusListener? = null

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
        fun setOnTaskStateListener(listener: OnTaskStatusListener): Builder {
            taskExecuteListener = listener
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
         * - 注意避免执行耗时任务
         * - 所有任务执行时间内，尽可能控制在100ms以内，否则会导致后续任务的执行时机异常，只能在放置于后台任务后才执行
         * @param task cpu空闲时间加载的任务
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
                task.setTaskStateListener(taskExecuteListener)
            }
            taskMonitor = DelayTaskExecuteMonitor(mMonitorRecordListener)
            return DelayTaskDispatcher(this)
        }
    }
}