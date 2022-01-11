package com.yupfeg.dispatcher.ext

import android.content.Context
import com.yupfeg.dispatcher.DelayTaskDispatcher
import com.yupfeg.dispatcher.task.DefaultTaskStateListener

/**
 * kotlin-dsl方式构建延迟启动器
 * @param context
 */
fun buildDelayStartUp(
    context: Context,
    builder : DelayTaskDispatcher.Builder.()->Unit
) : DelayTaskDispatcher{
    return DelayTaskDispatcher.Builder(context).apply(builder).builder()
}

/**
 * [DelayTaskDispatcher.Builder]的拓展函数，添加新的延迟任务
 * - 通常延迟任务都与界面相关，可以避免手动创建任务实现类
 * @param block 实际执行任务，提供ApplicationContext作为函数参数
 * @return [DelayTaskDispatcher.Builder]
 */
@Suppress("unused")
fun DelayTaskDispatcher.Builder.addTask(block : (Context)->Unit) : DelayTaskDispatcher.Builder{
    addTask(delayTask { context ->  block(context) })
    return this
}

/**
 * [DelayTaskDispatcher.Builder]拓展函数，以dsl方式设置任务执行状态监听
 * @param init 初始化任务执行状态监听
 * @return [DelayTaskDispatcher.Builder]
 * */
@Suppress("unused")
fun DelayTaskDispatcher.Builder.setOnTaskStateListener(
    init : DefaultTaskStateListener.()->Unit
) : DelayTaskDispatcher.Builder{
    val listener = DefaultTaskStateListener().apply(init)
    setOnTaskStateListener(listener)
    return this
}