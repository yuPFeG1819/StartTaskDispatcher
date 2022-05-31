package com.yupfeg.dispatcher

import android.content.Context
import android.os.Looper
import android.os.MessageQueue
import androidx.annotation.MainThread
import com.yupfeg.dispatcher.task.OnTaskStateListener
import com.yupfeg.dispatcher.task.Task
import com.yupfeg.dispatcher.task.TaskWrapper
import java.util.*

/**
 * 延迟任务调度器
 * * 仅在主线程执行任务
 * * 注意，使用该调度器会忽略所有任务的前置任务，如有并发需求可以使用[TaskDispatcher]进行并发任务调度
 * @author yuPFeG
 * @date 2021/12/11
 */
@Suppress("unused")
class DelayTaskDispatcher private constructor(builder: Builder) {

    private var mTaskQueue: Queue<Task> = builder.taskQueue

    private val mIdleHandler = MessageQueue.IdleHandler {
        // 分批执行的好处在于每一个task占用主线程的时间相对
        // 来说很短暂，并且此时CPU是空闲的，这些能更有效地避免UI卡顿
        mTaskQueue.poll()?.also { task ->
            TaskWrapper(task).run()
        }
        //如果所有延迟启动任务都结束，则移除该IdleHandler
        return@IdleHandler !mTaskQueue.isEmpty()
    }

    /**
     * 开启执行延迟任务
     * */
    @MainThread
    fun start() {
        Looper.myQueue().addIdleHandler(mIdleHandler)
    }

    /**
     * 清空所有延迟任务
     * - 通常在视图销毁时调用，防止任务未执行完造成内存泄漏
     * */
    @MainThread
    fun clearTask() {
        mTaskQueue.clear()
        Looper.myQueue().removeIdleHandler(mIdleHandler)
    }

    /**
     * 延迟任务调度器构筑器
     * - 不允许添加依赖任务，延迟任务只是在主线程空闲时执行
     * */
    class Builder(private val context: Context) {

        internal val taskQueue: Queue<Task> = LinkedList()

        private var mTaskExecuteListener: OnTaskStateListener? = null

        /**
         * 设置任务执行状态监听
         * @param listener
         * @return 延迟任务调度器本身，便于链式调用
         * */
        fun setOnTaskStateListener(listener: OnTaskStateListener): Builder {
            mTaskExecuteListener = listener
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
            return DelayTaskDispatcher(this)
        }
    }
}