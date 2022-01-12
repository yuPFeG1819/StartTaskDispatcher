# StartTaskDispatcher

启动任务调度器，充分发挥多线程并发优势，优化启动任务加载。

自动梳理任务执行顺序，内部通过有向无环图进行[拓扑排序](https://juejin.cn/post/6873233326186954765)。

- 外部声明任务的前置依赖关系，便于各启动任务解耦
- 提供内置线程池库，收敛线程应用，控制最大并发数，每个任务**可单独设置运行线程**
- 利用`CountDownLatch`支持异步**任务等待**，支持主线程**任务等待**，在前置依赖任务完成后才执行
- 以字符串作为任务唯一标识，区分任务与建立任务的依赖关系
- 提供独立的任务执行性能、各任务执行状态的监听
- 支持在调度器执行前后执行 **Head Task** 与 **Tail Task**
- 支持特殊任务提升执行优先级
- 支持Kotlin-DSL方式便捷构建任务调度器
- 提供`IdleHanler`延迟任务调度器，便于统一管理延迟任务





# 简单使用

1. 实现抽象类`Task`，设置任务唯一标识与具体执行的任务。

   > 可以覆写父类的一些方法函数，来修改运行线程、是否仅在主进程初始化、是否需要提高优先级等功能，具体可以看`Task`类源码，注释已经清晰表明其功能。
   >
   > - 启动任务的颗粒度推荐尽可能的细，最好是一个任务初始化一个SDK，这样才能更好发挥并发初始化的功能
   > - 相同标识的任务**有且只能出现一次**，避免**依赖成环**，这会导致无法排序，内部会抛出异常

   ``` kotlin
   class AsyncInitTask : Task(){
       companion object{
           const val TAG = "AsyncInitTask" //以静态常量交由外部设置依赖
       }
   
       override val tag: String
           get() = TAG
       
       override fun run() {
          	... 	//具体任务
       }
   	
   }
   ```

2. 通过`TaskDispatcherBuilder`构建任务调度器，添加需要需要执行的各项启动任务。

   通过`TaskDispatcherBuilder.dependsOn`函数，对最近调用`addTask`添加的任添加前置依赖任务，接收可变参数

   > 限制只能在`TaskDispatcherBuilder`添加任务、设置任务的前置依赖等操作

   ``` kotlin
   fun initBuilder(){
       TaskDispatcherBuilder(context).apply{
           addTask(...) //添加声明的任务
           .dependsOn(...) //添加上面任务的前置依赖任务标识
       }
   }
   ```

4. 如果需要监听任务运行状态，可以设置`OnMonitorRecordListener`进行监听，在所有任务调度完成后回调其中记录的所有任务执行性能信息`ExecuteRecordInfo`。

   ``` kotlin
   setOnMonitorRecordListener(object : OnMonitorRecordListener {
       override fun onMonitorRecordResult(timeInfo: ExecuteRecordInfo) {
         	...	//所有任务执行信息
       }
   })
   ```

5. 如果需要在任务调度前后执行额外任务，可以设置`OnDispatcherStateListener`并实现其中的`onStartBefore`与`onFinish`函数。

   ``` kotlin
   setOnDispatcherStateListener(object : OnDispatcherStateListener {
   
       override val isPrintDependsMap: Boolean
       	get() = true
   
       override fun onTaskSorted(dependsInfo: String) {
           ... //
       }
   
       override fun onStartBefore() {
           //Head Task
           ...
       }
   
       override fun onFinish() {
           //Tail Task 所有任务执行完成后执行
          	... 
       }
   })
   ```

   

6. 如需要单独监听每个任务执行状态，可以设置`OnTaskStateListener`监听每个任务执行状态。

   ``` kotlin
   setOnTaskStateListener(object : OnTaskStateListener {
       override fun onTaskWait(tag: String) {
           logi("$tag 任务开始等待")
       }
   
       override fun onTaskStart(tag: String, waitTime: Long){
           logi("$tag 任务开始执行 , 已等待前置任务 $waitTime ms")
       }
   
       override fun onTaskFinish(runningInfo: TaskRunningInfo) {
           logi("任务已执行完成 : $runningInfo")
       }
   
   })
   ```

6. 通过`TaskDispatcherBuilder.build`方法构建`TaskDispatcher`，并对所有任务进行**拓扑排序**。

   > 如果存在需要提高优先级的特殊任务，在拓扑排序后，会根据**被别人依赖 > 需要提升自己优先级 > 需要被等待 > 没有依赖**的顺序再次进行排序

   ``` kotlin
   val dispatcher = TaskDispatcherBuilder(context).build()
   ```

8. 调用`TaskDispatcher.strat`函数对所有任务进行调度。

## Kotlin-DSL
如果使用Kotlin，同样提供了便捷的DSL函数执行上述功能
```kotlin
//构建调度器
val dispatcher = startUp(this) {
   //this表示TaskDispatcherBuilder
   addTask(...){
       //this为MutableList，添加依赖任务标识
       add(...) //添加前置任务的标识
   }
   ...
   
   addTask(
       //简易快捷构建主线程任务，不太推荐使用这种方式，尽可能声明启动任务类，便于后续抽取解耦
       mainTask("initWebView"){ logd("run init WebView") }
   )
    
    //调度器状态监听，按需实现
    setOnDispatcherStateListener {
        isDebug = true
        
        onTaskSorted = {
            ...
        }
        
        onStartBefore = { 
            ... // Head Task
        }
        onFinish = {
            ... // Tail Task
        }
    }
    //任务执行性能记录监听
    setOnMonitorRecordListener {recordInfo->
       ...
    }
    
    //任务执行状态监听
    setOnTaskStateListener {
        onWait = {tag ->
           ... //任务开始等待前置任务
        }

        onStart = {tag,waitTime->
           ... //任务开始执行
        }
        onFinished = {runningInfo->
           ... //任务执行完成
        }
    }
}
    
//开启任务调度
dispatcher.start()
```

## 延迟调度器
延迟任务调度器`DelayTaskDispatcher`，同样使用`build`模式的`DelayTaskDispatcher.Builder`进行构建。

``` kotlin
val dispatcher = DelayTaskDispatcher.Builder(context)
	.addTask(...)
	.addTask(...)
	.build()
dispatcher.start()
```

`DelayTaskDispatcher`只能运行主线程任务，且不支持前置依赖任务。

可使用`setOnTaskStateListener`函数设置`OnTaskStateListener`，监听每个任务的执行状态。

延迟调度器也同样提供了`Kotlin-DSL`模式

``` kotlin
buildDelayStartUp(this){
    addTask {
        logd("延迟任务1 run")
    }
    addTask{
        logd("延迟任务2 run")
    }
    addTask {
        logd("延迟任务3 run")
    }

    setOnTaskStateListener{
        onStart = {tag, _ ->
            logd("$tag 任务开启")
        }
        onFinished = {info->
            logd("延迟任务结束 : $info")
        }
    }
}.start()
```



# 内部实现

其中最主要的就是多线程并发与[拓扑排序](https://juejin.cn/post/6873233326186954765)。

对各功能进行解耦分离，明确各类的职责。

- 调度器构建类`TaskDispatcherBuilder`

  利用`Builder`模式，将任务调度器`TaskDispatcher`的构建与设置属性功能都解耦抽取到`TaskDispatcherBuilder`类

  提供诸如**添加任务**、**添加依赖任务**、**添加监听**、**任务拓扑排序**等功能

  - 其中对任务进行排序，使用`BFS`(**广度优先搜索**)算法进行拓扑排序

    > 前提条件是任务可以构成[有向无环图](https://juejin.cn/post/6926794003794903048)
    >
    > **入度** ： 任务的前置依赖任务数目
    >
    > **出度** ： 依赖该任务的其他任务数目
    >
    > 在建立任务间的依赖关系后（哈希表），
    >
    > 1. 先检索所有入度为0的任务缓存到队列
    >
    > 2. 循环依次取出队列中的任务，添加到排序后的集合，
    >
    > 3. 如果该任务的出度不为0，则将依赖该任务的所有子任务的入度减1，
    > 4. 如果子任务中在经历上一步骤后，入度为0，则添加到任务队列，继续进行循环取出任务
    > 5. 最后所有任务入度都被减为0，从而完成拓扑排序。

  - 调度器执行状态监听`OnDispatcherStateListener`

    可以在调度器运行前后，执行额外任务

  - 任务执行状态监听`OnTaskStateListener`

    允许在每个任务的执行状态时执行额外操作

  - 执行任务执行性能监听`OnMonitorRecordListener`

    在任务执行完成后，回调所有任务的执行时间等信息

- 任务调度器类`TaskDispatcher`

  只提供**开启调度**与**取消异步任务**的功能

  在进行任务调度时，通过装饰器类`TaskWrapper`包装原始任务。

  - 配合`CoutDownLatch`在**多线程并发**时，**等待**前置依赖任务完成后再执行当前任务。
  - 利用执行性能监控类`TaskExecuteMonitor`，记录任务运行时间，在所有任务完成后通过`OnMonitorRecordListener`回调传递到外部
  - 在任务运行结束后，通知所有依赖该任务的子项任务，将等待的`CountDownLatch`计数减1。


- 延迟任务调度类`DelayTaskDispatcher`

  对于`IdleHandler`的封装，将每个任务分散在每个主线程空闲时间片之中，延迟执行。

- 线程池提供类`ExecutorProvider`

  提供`IO`密集型线程池与`CPU`密集型线程池，默认线程调度在`CPU`密集型线程池内，限制最大并发数，避免线程抢占，影响主线程执行





# Thanks

[alpha](https://github.com/alibaba/alpha)

[AnchorTask](https://github.com/gdutxiaoxu/AnchorTask)

[AndroidStartup](https://github.com/Leifzhang/AndroidStartup)