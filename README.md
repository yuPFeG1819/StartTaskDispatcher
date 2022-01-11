# StartTaskDispatcher

启动任务调度器，充分发挥多线程并发优势，优化启动任务加载。

自动梳理任务执行顺序，内部通过有向无环图进行[拓扑排序](https://juejin.cn/post/6873233326186954765)。

- 显式在外部声明任务的前置依赖关系，便于任务解耦
- 利用`CountDownLatch`支持异步**任务等待**，支持主线程**任务等待**，在前置依赖任务完成后才执行
- 以字符串作为任务唯一标识，区分与建立依赖关系
- 提供独立的任务执行性能、各任务执行状态的监听
- 支持在调度器执行前后执行 **Head Task** 与 **Tail Task**
- 支持特殊任务提升执行优先级
- 明确调度器各模块职责，解耦各模块功能
- 支持Kotlin-DSL方式便捷构建任务调度器
- 提供`IdleHanler`延迟任务调度器，便于统一管理延迟任务



# 简单使用

1. 实现抽象类`Task`，设置任务唯一标识与具体执行任务。

   > 可以覆写父类的一些方法函数，来修改运行线程、是否仅在主进程初始化、是否需要提高优先级等功能，具体可以看`Task`类源码，注释已经清晰表明其功能。
   >
   > - 启动任务的颗粒度推荐尽可能的细，这样才能

   ``` kotlin
   class AsyncInitTask : Task(){
       companion object{
           const val TAG = "AsyncInitTask"
       }
   
       override val tag: String
           get() = TAG
       
       override fun run() {
          	... 
       }
   	
   }
   ```

2. 通过`Builder`模式的`TaskDispatcherBuilder`构建任务调度器，添加需要需要执行的任务。

   > 限制只能在`TaskDispatcherBuilder`添加任务、设置任务的前置依赖等操作

   ``` kotlin
   fun initBuilder(){
       TaskDispatcherBuilder(context).apply{
           addTask(...) //添加声明的任务
           .dependsOn(...) //添加上面任务的前置依赖任务标识
       }
   }
   
   ```

3. 通过`TaskDispatcherBuilder.dependsOn`函数，对最近调用`addTask`添加任务进行添加前置依赖任务

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

7. 通过`TaskDispatcherBuilder.build`方法构建`TaskDispatcher`对所有任务进行**拓扑排序**。

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
       //this为MutableList，添加
       add(...) //添加前置任务的标识
   }
   ...
   
   addTask(
       //简易快捷构建主线程任务，不太推荐使用这种方式，尽可能声明启动任务类
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



# Thanks

[alpha](https://github.com/alibaba/alpha)

[AnchorTask](https://github.com/gdutxiaoxu/AnchorTask)

[AndroidStartup](https://github.com/Leifzhang/AndroidStartup)