package com.yupfeg.dispatcher.tools

import com.yupfeg.dispatcher.task.Task
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * 启动任务的有向无环图拓扑排序工具类
 * @author yuPFeG
 * @date 2022/01/05
 */
internal object TaskSortTools {

    /**
     * 是否存在提高任务优先级的任务
     * - 一个Task耗时非常多但是优先级却一般，很有可能开始的时间较晚，
     * 导致最后只是在等它，这种可以早开始。
     * - 或者需要主线程等待的任务，也需要优先执行
     * */
    @Volatile
    private var isUpPriorityTask : Boolean = false

    /**
     * 获取拓扑排序后的任务集合
     * @param origins 原始任务集合
     * @param nodeTaskDependMap 节点依赖关系的哈希表
     * @return 拓扑排序后的任务集合
     */
    @Throws(IllegalStateException::class)
    @Synchronized
    @JvmStatic
    fun getSortedList(
        origins: List<Task>,
        nodeTaskDependMap : HashMap<String, MutableList<Task>>
    ) : MutableList<Task>{
        isUpPriorityTask = false
        //缓存需要被依赖的任务类型，作为优先执行任务
        val dependsSet = HashSet<String>()
        //1. 建立依赖关系哈希表
        collectTaskDepends(origins,dependsSet,nodeTaskDependMap)
        //2. 拓扑排序
        val sortedTasks = topologicalSortList(origins,nodeTaskDependMap)
        if (isUpPriorityTask && dependsSet.isNotEmpty()) {
            //3. 提高特殊任务优先级
            return getUpPriorityTasks(sortedTasks, dependsSet)
        }
        return sortedTasks
    }

    /**
     * 收集所有任务的依赖关系
     * @param origins 原始启动任务的集合
     * @param nodeTaskDependMap 任务节点的依赖关系
     */
    private fun collectTaskDepends(
        origins: List<Task>,
        dependsSet : HashSet<String>,
        nodeTaskDependMap : HashMap<String, MutableList<Task>>
    ){
        for (task in origins) {
            val depends = task.taskDependsOn
            if (depends.isNullOrEmpty()) continue

            for (taskTag in depends) {
                // 添加到对应任务类型的集合
                // 方便在指定任务类型完成后，通知所有依赖该任务的后续任务
                nodeTaskDependMap[taskTag]?.also {
                    it.add(task)
                }?:run {
                    val tasks = mutableListOf<Task>()
                    tasks.add(task)
                    nodeTaskDependMap[taskTag] = tasks
                }
                //添加依赖项任务类型
                dependsSet.add(taskTag)
            }
        }
    }

    /**
     * 对任务集合进行拓扑排序
     *
     * [有向无环图的拓扑排序](https://juejin.cn/post/6873233326186954765)
     * - 入度：顶点的入度是指「指向该顶点的边」的数量；
     * - 出度：顶点的出度是指该顶点指向其他点的边的数量。
     * - 使用BFS（广度优先搜索）算法进行拓扑排序
     * @param origins 原始任务集合
     * @param nodeTaskDependMap 节点依赖关系的哈希表
     */
    @Throws(IllegalStateException::class)
    private fun topologicalSortList(
        origins: List<Task>,
        nodeTaskDependMap : HashMap<String, MutableList<Task>>
    ) : MutableList<Task>{
        val newTasks : MutableList<Task> = ArrayList(origins.size)
        val queue : Queue<Task> = ArrayDeque()
        val nodeInDegreeMap = HashMap<String,Int>()
        //1. 遍历找到所有入度为0的任务,并放入到任务队列
        findTopNodeTask(origins, queue, nodeInDegreeMap)
        //2. 开始BFS算法遍历入度为0的任务队列进行排序
        while (queue.isNotEmpty()){
            val nodeTask = queue.poll() ?: break
            newTasks.add(nodeTask)
            //3. 在该任务执行完后，遍历该任务的所有子节点任务（出度不为0）
            val childTasks = nodeTaskDependMap[nodeTask.tag]
            if (childTasks.isNullOrEmpty()) continue
            for (childTask in childTasks){
                var nodeInDegree = nodeInDegreeMap[childTask.tag]?:0
                //4. 依赖的任务已结束，则该子任务的入度-1，如果该节点入度为0，则放入到任务队列等待执行
                nodeInDegree--
                nodeInDegreeMap[childTask.tag] = nodeInDegree
                if (nodeInDegree == 0){
                    queue.offer(childTask)
                }
            }
        }

        //5. 所有入度为0的节点执行完后，校验是否节点有环，如果节点数量不相等表示任务列表不是有向无环图，抛出异常
        check(newTasks.size == origins.size){
            "task list exist circle,check task depends ;\n input size ${origins.size} : $origins" +
                    "\n result size ${newTasks.size} : $newTasks "
        }
        return newTasks
    }

    /**
     * 检索入度为0的任务
     * @param origins 所有原始任务集合
     * @param queue 入度为0，即没有依赖前置任务的任务队列
     * @param nodeInDegreeMap 缓存任务的依赖任务（入度）数量的入度哈希表
     * */
    private fun findTopNodeTask(
        origins: List<Task>,
        queue: Queue<Task>,
        nodeInDegreeMap : HashMap<String,Int>,
    ){
        for (task in origins) {
            if (nodeInDegreeMap.containsKey(task.tag)){
                throw RuntimeException("task tag : ${task.tag} is repeat，check your task tag")
            }

            if (task.isNeedRunAsSoon || task.isAsyncTaskNeedMainWaitOver()){
                //存在需要尽快执行的任务
                if (!isUpPriorityTask) isUpPriorityTask = true
            }

            val dependNum = task.taskDependsOn.size
            //缓存当前任务标识存在的子任务数量（统计任务节点的出度）
            nodeInDegreeMap[task.tag] = dependNum
            if (dependNum == 0){
                queue.offer(task)
            }
        }
    }

    /**
     * 获取需要提高优先执行的任务优先级的任务集合
     * @param sortedList 拓扑排序后的任务集合
     * @param dependsTaskSet 被依赖任务的哈希表
     */
    private fun getUpPriorityTasks(
        sortedList : List<Task>,
        dependsTaskSet : Set<String>,
    ) : MutableList<Task>{
        val newTasks : MutableList<Task> = ArrayList(sortedList.size)
        //被依赖的任务
        val dependsOnTasks : MutableList<Task> = mutableListOf()
        //需要尽快执行的任务
        val runAsSoonTasks : MutableList<Task> = mutableListOf()
        //需要主线程等待的任务
        val needWaitTasks : MutableList<Task> = mutableListOf()
        //其他任务
        val otherTasks : MutableList<Task> = mutableListOf()
        for (task in sortedList) {
            when {
                dependsTaskSet.contains(task.tag) -> {
                    //被依赖的任务需要先执行
                    dependsOnTasks.add(task)
                }
                task.isNeedRunAsSoon -> {
                    //需要尽快执行，特殊情况，优先级不高但耗时
                    runAsSoonTasks.add(task)
                }
                task.isAsyncTaskNeedMainWaitOver() -> {
                    //需要主线程等待的任务
                    needWaitTasks.add(task)
                }
                else -> {
                    otherTasks.add(task)
                }
            }
        }
        // 顺序：被别人依赖的 > 需要提升自己优先级的 > 需要被等待的 > 没有依赖的
        newTasks.addAll(dependsOnTasks)
        newTasks.addAll(runAsSoonTasks)
        newTasks.addAll(needWaitTasks)
        newTasks.addAll(otherTasks)
        return newTasks
    }
}