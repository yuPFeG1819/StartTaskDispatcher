package com.yupfeg.dispatcher.ext

import android.content.Context
import com.yupfeg.dispatcher.annotation.TaskDispatcherDslMarker
import com.yupfeg.dispatcher.task.MainTask
import com.yupfeg.dispatcher.task.Task

/**
 * 快捷创建主线程运行的任务
 * - 仅为便捷方法，推荐在任务模块内实现任务类，更便于解耦使用
 * @param tag 任务唯一标识
 * @param block 任务执行操作，提供ApplicationContext作为函数参数
 * @return 主线程启动任务实例
 * */
fun mainTask(tag : String,block : (Context)->Unit) : Task{
    return object : MainTask(){
        override val tag: String = tag
        override fun run() = block(context)
    }
}

/**
 * 快捷创建异步任务
 * - 仅为便捷方法，推荐在任务模块内实现任务类，更便于解耦使用
 * @param tag 任务唯一标识
 * @param block 任务执行操作，提供ApplicationContext作为函数参数
 * @return 异步启动任务实例
 * */
@Suppress("unused")
fun asyncTask(tag: String, block: (Context) -> Unit) : Task{
    return object : Task(){
        override val isRunOnMainThread: Boolean = false
        override val tag: String = tag
        override fun run() = block(context)
    }
}

/**
 * 快捷创建需要主线程等待的任务
 * - 仅为便捷方法，推荐在任务模块内实现任务类，方便组件化解耦时依赖使用
 * @param tag 任务唯一标识
 * @param block 任务执行操作，提供ApplicationContext作为函数参数
 * @return 启动任务实例
 */
@Suppress("unused")
fun needWaitOverAsyncTask(tag: String,block: (Context) -> Unit) : Task{
    return object : Task(){
        override val tag: String = tag
        override fun run() = block(context)
        override val isRunOnMainThread: Boolean = false
        override val isNeedWaitTaskOver: Boolean = true
    }
}

/**
 * 创建锚点任务
 * - 可利用该任务整合一些通用的依赖关系，后续任务只需要依赖该任务即可，避免所有任务都要写上基础依赖
 * @param tag 任务的唯一标识
 * @param isNeedMainWait 是否需要主线程等待该任务完成
 * @param init dsl方式初始化任务，以可变集合作为函数类型接收者
 * */
fun anchorTask(
    tag: String,isNeedMainWait : Boolean = true,
    init : (@TaskDispatcherDslMarker MutableList<String>).()->Unit
) : Task{
    return anchorTask(tag, isNeedMainWait,dependsOnList = mutableListOf<String>().apply(init))
}

/**
 * 创建锚点任务
 * - 可利用该任务整合一些通用的依赖关系，后续任务只需要依赖该任务即可，避免所有任务都要写上基础依赖
 * @param tag 任务标识
 * @param isNeedMainWait 是否需要主线程等待
 * @param dependsOnList 依赖的前置任务标识集合
 * */
fun anchorTask(tag: String,isNeedMainWait : Boolean,dependsOnList : List<String>) : Task{
    return object : Task(){

        init {
            addDependsOnList(dependsOnList)
        }

        override val isNeedWaitTaskOver: Boolean
            get() = isNeedMainWait

        override val tag: String = tag
        override fun run() = Unit
    }
}
