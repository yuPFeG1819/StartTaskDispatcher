package com.yupfeg.dispatcher.ext

import android.content.Context
import com.yupfeg.dispatcher.DefaultDispatcherStateListener
import com.yupfeg.dispatcher.DelayTaskDispatcher
import com.yupfeg.dispatcher.annotation.TaskDispatcherDslMarker
import com.yupfeg.dispatcher.monitor.delay.DelayTaskRecordInfo
import com.yupfeg.dispatcher.monitor.delay.OnDelayTaskRecordListener
import com.yupfeg.dispatcher.task.DefaultTaskStateListener
import com.yupfeg.dispatcher.task.MainTask

/**
 * kotlin-dsl方式构建延迟启动器
 * @param context
 */
fun startUpDelay(
    context: Context,
    builder: (@TaskDispatcherDslMarker DelayTaskDispatcher.Builder).() -> Unit
): DelayTaskDispatcher {
    return DelayTaskDispatcher.Builder(context).apply(builder).builder()
}

/**
 * [DelayTaskDispatcher.Builder]的拓展函数，添加新的延迟任务
 * - 通常延迟任务都与界面相关，可以避免手动创建任务实现类
 * @param block 实际执行任务，提供ApplicationContext作为函数参数
 * @return [DelayTaskDispatcher.Builder]
 */
@Suppress("unused")
fun DelayTaskDispatcher.Builder.addTask(
    tag: String = "DelayTask",
    block: (Context) -> Unit
): DelayTaskDispatcher.Builder {
    this.addTask(object : MainTask() {
        override val tag: String = tag
        override fun run() = block(context)
    })
    return this
}

/**
 * [DelayTaskDispatcher.Builder]拓展函数，以dsl方式设置任务执行状态监听
 * @param init 初始化任务执行状态监听
 * @return [DelayTaskDispatcher.Builder]
 * */
@Suppress("unused")
fun DelayTaskDispatcher.Builder.setOnTaskStateListener(
    init: (@TaskDispatcherDslMarker DefaultTaskStateListener).() -> Unit
): DelayTaskDispatcher.Builder {
    val listener = DefaultTaskStateListener().apply(init)
    setOnTaskStateListener(listener)
    return this
}

/**
 * [DelayTaskDispatcher.Builder]拓展函数，以dsl方式设置调度器状态监听
 * @param init 初始化调度器状态回调监听的高阶函数
 * @return [DelayTaskDispatcher.Builder]
 * */
@Suppress("unused")
fun DelayTaskDispatcher.Builder.setOnDispatcherStateListener(
    init: (@TaskDispatcherDslMarker DefaultDispatcherStateListener).() -> Unit
): DelayTaskDispatcher.Builder {
    setOnDispatcherStateListener(DefaultDispatcherStateListener().apply(init))
    return this
}

/**
 * [DelayTaskDispatcher.Builder]拓展函数，设置调度器性能监控记录监听
 * @param block 所有任务执行完成后回调的任务执行记录信息
 * @return [DelayTaskDispatcher.Builder]
 * */
@Suppress("unused")
fun DelayTaskDispatcher.Builder.setOnMonitorRecordListener(
    block: (DelayTaskRecordInfo) -> Unit
): DelayTaskDispatcher.Builder {
    setOnMonitorRecordListener(object : OnDelayTaskRecordListener {
        override fun onAllTaskRecordResult(timeInfo: DelayTaskRecordInfo) {
            block(timeInfo)
        }
    })
    return this
}