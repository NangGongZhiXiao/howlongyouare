package com.zhixiao.hlya;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * @ClassName: SecondActivity
 * @Description:
 * @Author: zhixiao
 * @CreateDate: 2019/12/7
 */
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
