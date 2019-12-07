package com.zhixiao.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class HLYAPlugin implements Plugin<Project> {

    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        def classTransform = new HLYATransform(project)
        // 在build.gradle创建类HLYAMethods，配置需要添加时长检测的方法
        project.extensions.create('hlyaMethods', HLYAMethods)
        android.registerTransform(classTransform)
    }
}