package com.zhixiao.hlya.utils.hlya;

import android.util.Log;

import java.util.Stack;

/**
 * @ClassName: HLYAServer
 * @Description:
 * @Author: zhixiao
 * @CreateDate: 2019/12/5
 */
public class HLYAServer {
    private static volatile HLYAServer INSTANCE;
    private static final String TAG = "HLYAServer";
    private Stack<HLYAMethod> methodStack;
    private StringBuilder formatSb;

    public static HLYAServer getInstance(){
        if(INSTANCE == null){
            synchronized (HLYAServer.class){
                if (INSTANCE == null) {
                    INSTANCE = new HLYAServer();
                }
            }
        }
        return INSTANCE;
    }

    private HLYAServer(){
        methodStack = new Stack<>();
        formatSb = new StringBuilder();
    }

    public void startMethod(HLYAMethod method){
        Log.i(TAG, String.format("%s⌈start %s", formatSb.toString(), method.getFullyMethodName()));
        formatSb.append("|   ");
        methodStack.push(method);
    }

    public void endMethod(){
        int length = formatSb.length();
        formatSb.delete(length-4, length);
        Log.i(TAG, String.format("%s⌊end time: %d ms", formatSb.toString(), methodStack.pop().end()));
    }
}
