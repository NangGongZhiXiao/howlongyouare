# Android学习笔记 Javassist

### 背景

学习Javassist这个内容起因是wanAndroid知识星球的第一期作业（尝试用ASM或Javassist修改Java字节码），既不懂ASM也不懂Javassist，研究了一下发现ASM需要熟悉Java字节码，而自己对这方面不熟，所以选择了Javassist。

### Gradle

为什么上来先说Gradle？我们都知道Android最终的APK是由Gradle构建出来的，Gradle之前是Java源文件，Gradle之后是APK。那么对Java字节码的操作就得在Gradle中进行了，除非你不用Gradle来构建你的Android工程。

那么怎么在Gradle中修改我们的字节码？首先看看Gradle的是如何工作的，我们在编译工程的时候会在build窗口看见一堆这样的输出：
``` java
> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:checkDebugManifest UP-TO-DATE
> Task :app:generateDebugBuildConfig UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:mainApkListPersistenceDebug UP-TO-DATE
> Task :app:generateDebugResValues UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:mergeDebugShaders UP-TO-DATE
> Task :app:compileDebugShaders UP-TO-DATE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:compileDebugAidl NO-SOURCE
> Task :app:compileDebugRenderscript NO-SOURCE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:compileDebugJavaWithJavac
> Task :app:compileDebugSources
> Task :app:mergeDebugAssets UP-TO-DATE
> Task :app:processDebugJavaRes NO-SOURCE
> Task :app:mergeDebugJavaResource UP-TO-DATE
> Task :app:validateSigningDebug UP-TO-DATE
> Task :app:checkDebugDuplicateClasses UP-TO-DATE
```

这些输出是Gradle的构建过程的任务，任务的产物从上一个任务的输出来到当前任务的输入再输出到下一个任务，一条任务链下来最终构建出我们的APK。这样我们就可以定义一个任务让它在源代码编译成Java字节码之后，打包成dex之前对字节码进行修改，就可以实现编译时插桩的操作了。

### Transform

事实上在Gradle 3.5.0版本之前就是这么做的，但是在3.5.0版本之后Gradle引入了Transform，我们的Gradle会帮我们把Transform包装成Task加入到任务链中，位置就是在编译成Java字节码之后，正和我意！既如此，我们就可以定义一个Gradle插件去注册我们的Transform，这样我们就可以在Transform中修改Java字节码了。

定义Transform主要实现以下几个方法：
``` groovy
class CustomTransform extends Transform{
	// 自定义Transform的名字
    @Override
    String getName() {}

	// 输入类型，即你需要处理的类型，我们现在需要处理的是字节码，那就返回字节码类型
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

	// 处理内容的范围 我们选择整个工程，当然你可以根据实际情况缩小范围提升效率
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

	// 是否支持增量编译，这是为了提升编译性能使用的
    @Override
    boolean isIncremental() {
        return false
    }

	// 真正操作字节码的地方
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        injector = new MyTestInjector(project)
        transformInvocation.getInputs().each { TransformInput input ->
            // 遍历文件夹
            input.directoryInputs.each { DirectoryInput directoryInput ->
                doSomething() // 处理函数
                
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
```

接下来我们只需要在代码中的doSomething()处理我们的逻辑就好了。

### Javassist

Javassist的使用主要是Classpool，CtClass和CtMethod这三个类，第一个是类池，后两个从名字就可以看出他们表示Java中的类和方法，操作步骤就是：
1. 从类池取出类或者自己定义一个类
2. 操作类和方法
3. 将类写到Class文件

### 通过Javassist实现方法耗时检测

具体的用法还是通过demo来看看吧，我们就来实现一下方法的耗时检测。
新建项目，然后再加入一个module，在这个module里写我们的Gradle插件，具体做法直接网上就能找到，我们直接来看看插件的实现。
```groovy
class HLYAPlugin implements Plugin<Project> {
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        // 在build.gradle创建类HLYAMethods，配置需要添加耗时检测的方法
        project.extensions.create('hlyaMethods', HLYAMethods)
        def classTransform = new HLYATransform(project)
        android.registerTransform(classTransform)
    }
}
    
class HLYAMethods {
    String[] methods // 配置的方法列表
    String[] classes // 配置的类列表，类里定义的方法都会添加耗时检测
}
```
上面我们在定义的插件里面去注册了Transform，然后还做了一件事情就是添加配置信息，用来配置需要添加耗时检测的方法，更好的实现解耦和控制灵活性。
```groovy
class HLYATransform extends Transform{
	// 只留下关键部分
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
		// 我们在这里定义了一个注入类专门去完成操作字节码的工作
        HLYAInjector injector = new HLYAInjector(project)
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
```
事实上真正用到Javassist的地方HLYAInjector里面，通过在方法的前后插入代码计算时间差得到方法耗时。
```groovy
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
```
在这里我也写了一个工具来对耗时进行格式化输出，具体可以看看我的项目。在app的build.gradle加入如下内容:
```java
hlyaMethods{
    methods = ["com.zhixiao.hlya.MainActivity.onCreate", "com.zhixiao.hlya.MainActivity.hlya"]
    classes = ["com.zhixiao.hlya.SecondActivity"]
}
```
执行这样的代码
```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        noHlya();
        hlya();
        startActivity(new Intent(this, SecondActivity.class));
    }

    private void hlya() {
        try {
            Thread.sleep(100);
            hlya(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void hlya(long a){
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void noHlya(){
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


/***************************************第二个页面**********************************************/
public class SecondActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        oneMethod();
    }

    private void oneMethod() {
        try {
            Thread.sleep(102);
            secondMethod(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void secondMethod(int count) {
        try {
            Thread.sleep(50);
            if(count > 0){
                secondMethod(--count);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

```
测试的输出如下：
``` java
I: ⌈start com.zhixiao.hlya.MainActivity.onCreate(android.os.Bundle)
I: |   ⌈start com.zhixiao.hlya.MainActivity.hlya()
I: |   |   ⌈start com.zhixiao.hlya.MainActivity.hlya(long)
I: |   |   ⌊end time: 101 ms
I: |   ⌊end time: 205 ms
I: ⌊end time: 933 ms
I: ⌈start com.zhixiao.hlya.SecondActivity.onCreate(android.os.Bundle)
I: |   ⌈start com.zhixiao.hlya.SecondActivity.oneMethod()
I: |   |   ⌈start com.zhixiao.hlya.SecondActivity.secondMethod(int)
I: |   |   |   ⌈start com.zhixiao.hlya.SecondActivity.secondMethod(int)
I: |   |   |   |   ⌈start com.zhixiao.hlya.SecondActivity.secondMethod(int)
I: |   |   |   |   |   ⌈start com.zhixiao.hlya.SecondActivity.secondMethod(int)
I: |   |   |   |   |   ⌊end time: 53 ms
I: |   |   |   |   ⌊end time: 107 ms
I: |   |   |   ⌊end time: 161 ms
I: |   |   ⌊end time: 213 ms
I: |   ⌊end time: 318 ms
I: ⌊end time: 322 ms
```
格式化输出了方法调用的层级和时间。


### 最后
虽然完成的是小小的一个作业，但是觉得学到了很多之前没有接触过的东西，尝试把学习的过程记录下来相信对以后会有很大的帮助。

附上项目的链接：[How Long You Are](https://github.com/NangGongZhiXiao/howlongyouare)
