package com.yupfeg.dispatcher.ext

import android.content.Context
import com.yupfeg.dispatcher.DefaultDispatcherStateListener
import com.yupfeg.dispatcher.TaskDispatcher
import com.yupfeg.dispatcher.TaskDispatcherBuilder
import com.yupfeg.dispatcher.annotation.TaskDispatcherDslMarker
import com.yupfeg.dispatcher.monitor.DefaultMonitorRecordListener
import com.yupfeg.dispatcher.task.DefaultTaskStateListener
import com.yupfeg.dispatcher.task.Task


/**
 * 构建启动任务调度器
 * @param context
 * @param init kotlin-dsl的构造函数
 * */
fun startUp(
    context: Context,
    init : (@TaskDispatcherDslMarker TaskDispatcherBuilder).()->Unit
) : TaskDispatcher{
    return TaskDispatcherBuilder(context).apply(init).build()
}

// <editor-fold desc="添加任务依赖">

/**
 * [TaskDispatcherBuilder]的拓展函数，dsl方式添加启动任务
 * @param task 需要启动的任务
 * @param init 构建任务依赖关系的函数
 * */
fun TaskDispatcherBuilder.addTask(
    task: Task,
    init : (@TaskDispatcherDslMarker MutableList<String>).()->Unit
) : TaskDispatcherBuilder{
    task.addDependsOnList(mutableListOf<String>().apply(init))
    this.addTask(task)
    return this
}

/**
 * [TaskDispatcherBuilder]的拓展函数，添加锚点任务
 * - 锚点任务本身不执行任务操作
 * - 锚点任务整合一些通用的依赖关系，后续任务只需要依赖该任务即可，避免所有任务都要写上基础依赖
 * @param tag 锚点任务标识
 * @param init 依赖关系集合dsl函数
 * @return [TaskDispatcherBuilder]
 * */
fun TaskDispatcherBuilder.addAnchorTask(
    tag : String,
    init : (@TaskDispatcherDslMarker MutableList<String>).()->Unit
) : TaskDispatcherBuilder{
    val anchorTask = anchorTask(tag, init)
    addTask(anchorTask)
    return this
}

// </editor-fold>

// <editor-fold desc="性能监控">

/**
 * [TaskDispatcherBuilder]拓展函数，设置调度器性能监控记录监听
 * @param block 任务执行完成后回调的任务执行记录信息
 * @return [TaskDispatcherBuilder]
 * */
@Suppress("unused")
fun TaskDispatcherBuilder.setOnMonitorRecordListener(
    block : (@TaskDispatcherDslMarker DefaultMonitorRecordListener).()->Unit
) :  TaskDispatcherBuilder{
    setOnMonitorRecordListener(DefaultMonitorRecordListener().apply(block))
    return this
}

// </editor-fold>

// <editor-fold desc="状态监听">

/**
 * [TaskDispatcherBuilder]拓展函数，以dsl方式设置调度器性能监控记录监听
 * @param init 初始化调度器状态回调监听的高阶函数
 * @return [TaskDispatcherBuilder]
 * */
@Suppress("unused")
fun TaskDispatcherBuilder.setOnDispatcherStateListener(
    init : (@TaskDispatcherDslMarker DefaultDispatcherStateListener).()->Unit
) : TaskDispatcherBuilder{
    setOnDispatcherStateListener(DefaultDispatcherStateListener().apply(init))
    return this
}

/**
 * [TaskDispatcherBuilder]拓展函数，以dsl方式设置任务执行状态监听
 * @param init 初始化任务执行状态监听
 * @return [TaskDispatcherBuilder]
 * */
@Suppress("unused")
fun TaskDispatcherBuilder.setOnTaskStateListener(
    init : (@TaskDispatcherDslMarker DefaultTaskStateListener).()->Unit
) : TaskDispatcherBuilder{
    setOnTaskStateListener(DefaultTaskStateListener().apply(init))
    return this
}

// </editor-fold>

