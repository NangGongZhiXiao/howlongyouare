package com.zhixiao.plugin

import com.android.build.gradle.AppExtension
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.gradle.api.Project

/**
 * @ClassName: HLYAInjector* @Description:
 * @Author:         zhixiao
 * @CreateDate: 2019/12/7
 */
class HLYAInjector {
    // 类池
    private final static ClassPool pool = ClassPool.getDefault()
    private String[] methods
    private String[] classes
    private List<String> mClasses

    HLYAInjector(Project project){
        // android相关类的路径
        pool.appendClassPath(project.extensions.getByType(AppExtension).bootClasspath[0].toString())
        // 引入android.os.Bundle包，因为onCreate方法参数有Bundle
        pool.importPackage("android.os.Bundle")
        pool.importPackage("android.widget.Toast")
        // 这些是使用到的包，提前引入
        pool.importPackage("com.zhixiao.hlya.utils.hlya.HLYAServer")
        pool.importPackage("com.zhixiao.hlya.utils.hlya.HLYAMethod")
        // 从build.gradle中读取配置好的需要添加耗时检测的方法并存储下来
        methods = project.hlyaMethods.methods
        classes = project.hlyaMethods.classes
        mClasses = new LinkedList<>()
        methods.each {String method ->
            int lastDot = method.lastIndexOf(".")
            mClasses.add(method.substring(0, lastDot))
            println("get mclass " + mClasses.get(mClasses.size()-1))
        }
    }

    void inject(String path){
        // 被注入的类路径
        pool.appendClassPath(path)

        File dir = new File(path)
        if(dir.isDirectory()){
            // 递归遍历所有文件
            dir.eachFileRecurse { File file ->
                if(file.isFile()) {
                    String filePath = file.path
                    String className = filePath.substring(filePath.lastIndexOf("classes\\") + 8, filePath.length() - 6).replace("\\", ".")
                    println(className)
                    // 如果该类在classes配置了则将它的全部方法都添加耗时检测
                    if (classes.contains(className)) {
                        // 成功找到需要注入的类
                        CtClass ctClass = pool.getCtClass(className)
                        if(ctClass.isFrozen()){
                            ctClass.defrost()
                        }

                        CtMethod[] ctMethods = ctClass.getDeclaredMethods()
                        ctMethods.each {CtMethod ctMethod ->
                            inject2method(ctMethod)
                        }
                        ctClass.writeFile(path)
                        ctClass.detach()
                    }// 如果类在methods的方法中定义了则进入找到它的方法添加耗时检测
                    else if(mClasses.contains(className)){
                        // 成功找到需要注入的类
                        CtClass ctClass = pool.getCtClass(className)
                        if(ctClass.isFrozen()){
                            ctClass.defrost()
                        }

                        int size = mClasses.size()
                        for (int i = 0; i < size; i++) {
                            if(mClasses.get(i) == className) {
                                String name = methods[i]
                                String[] ss = name.split('\\.')
                                CtMethod[] ctMethods = ctClass.getDeclaredMethods(ss[ss.length - 1])
                                ctMethods.each { CtMethod ctMethod ->
                                    inject2method(ctMethod)
                                }
                            }
                        }
                        ctClass.writeFile(path)
                        ctClass.detach()
                    }
                }
            }
        }
    }

    // 将耗时检测注入到方法中
    private static void inject2method(CtMethod ctMethod) {
        println("inject to ctMethod = " + ctMethod.longName)

        String beforeStr = String.format("HLYAServer.getInstance().startMethod(new HLYAMethod(\"%s\"));", ctMethod.longName)
        String afterStr = """HLYAServer.getInstance().endMethod();"""

        ctMethod.insertBefore(beforeStr)
        ctMethod.insertAfter(afterStr)
    }
}
