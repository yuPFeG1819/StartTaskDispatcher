package com.yupfeg.dispatcher.annotation

/**
 * 任务调度器dsl的作用域限制注解
 * - 以此注解标记的dsl方法参数，只能隐式调用当前作用域的
 * */
@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class TaskDispatcherDslMarker