package com.zhixiao.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import org.gradle.internal.impldep.org.apache.ivy.util.FileUtil


class HLYATransform extends Transform{
    private Project project
    private HLYAInjector injector

    HLYATransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "MyTestTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        // 我们在这里定义了一个注入类专门去完成操作字节码的工作
        injector = new HLYAInjector(project)
        transformInvocation.getInputs().each { TransformInput input ->
            // 遍历文件夹
            input.directoryInputs.each { DirectoryInput directoryInput ->
                // 将耗时检测的代码注入到字节码里
                injector.inject(directoryInput.file.absolutePath)

                def desk = transformInvocation.getOutputProvider().getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)

                FileUtils.copyDirectory(directoryInput.file, desk)
            }

            // 遍历jar文件
            input.jarInputs.each { JarInput jarInput ->
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
                if(jarName.endsWith(".jar")){
                    jarName = jarName.substring(0, jarName.length()-4)
                }

                def desk = transformInvocation.getOutputProvider().getContentLocation(
                        jarName+md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, desk)
            }
        }
    }
}