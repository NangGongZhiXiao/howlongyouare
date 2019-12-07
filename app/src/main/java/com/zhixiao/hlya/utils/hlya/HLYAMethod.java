package com.zhixiao.hlya.utils.hlya;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @ClassName: HLYAMethod
 * @Description:
 * @Author: zhixiao
 * @CreateDate: 2019/12/5
 */
public class HLYAMethod {
    private String methodName;
    private String className;
    private String fullyMethodName;
    private long startTime;

    public HLYAMethod(String className, String methodName) {
        this.methodName = methodName;
        this.className = className;
        fullyMethodName = className + "." + methodName;
        startTime = System.currentTimeMillis();
    }

    public HLYAMethod(Class clazz, Method method) {
        this(clazz.getCanonicalName(), getMethodLongName(method));
    }

    public HLYAMethod(String name) {
        fullyMethodName = name;
        startTime = System.currentTimeMillis();
    }

    private static String getMethodLongName(Method method) {
        final StringBuilder methodName = new StringBuilder(method.getName());
        methodName.append("(");
        Arrays.asList(method.getParameterTypes()).stream().forEach(new Consumer<Class<?>>() {
            @Override
            public void accept(Class<?> aClass) {
                methodName.append(aClass.getName());
                methodName.append(",");
            }
        });
        int length = methodName.length();
        if(methodName.charAt(length) == ','){
            methodName.deleteCharAt(length-1);
        }
        methodName.append(")");
        return methodName.toString();
    }


    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getFullyMethodName() {
        return fullyMethodName;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long end(){
        return System.currentTimeMillis() - startTime;
    }
}
