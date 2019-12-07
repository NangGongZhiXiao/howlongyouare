package com.zhixiao.hlya;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.sangfor.hlya.R;

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
