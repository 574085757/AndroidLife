package com.camnter.gradle.plugin.reduce.dependency.packaging.hooker

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.internal.reflect.Instantiator

/**
 * Copy from VirtualAPK
 *
 * Manager of hookers, responsible for registration and scheduling execution
 *
 * @author CaMnter
 */

class TaskHookerManager {

    private Map<String, GradleTaskHooker> taskHookerMap = new HashMap<>()

    private Project project
    private AppExtension android
    private Instantiator instantiator

    TaskHookerManager(Project project, Instantiator instantiator) {
        this.project = project
        this.instantiator = instantiator
        this.android = project.extensions.findByType(AppExtension)
        project.gradle.addListener(new VirtualApkTaskListener())
    }

    void registerTaskHookers() {
        project.afterEvaluate {
            android.applicationVariants.all { ApplicationVariant appVariant ->
                registerTaskHooker(
                        instantiator.newInstance(PrepareDependenciesHooker, project, appVariant))
            }
        }
    }

    private void registerTaskHooker(GradleTaskHooker taskHooker) {
        taskHooker.setTaskHookerManager(this)
        taskHookerMap.put(taskHooker.taskName, taskHooker)
    }

    public <T> T findHookerByName(String taskName) {
        return taskHookerMap[taskName] as T
    }

    private class VirtualApkTaskListener implements TaskExecutionListener {

        @Override
        void beforeExecute(Task task) {
            if (task.project == project) {
                if (task in TransformTask) {
                    taskHookerMap[task.transform.name]?.beforeTaskExecute(task)
                } else {
                    taskHookerMap[task.name]?.beforeTaskExecute(task)
                }
            }
        }

        @Override
        void afterExecute(Task task, TaskState taskState) {
            if (task in TransformTask) {
                taskHookerMap[task.transform.name]?.afterTaskExecute(task)
            } else {
                taskHookerMap[task.name]?.afterTaskExecute(task)
            }
        }
    }
}