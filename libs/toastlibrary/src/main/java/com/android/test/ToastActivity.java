package com.android.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;


import androidx.annotation.NonNull;

import com.android.toast.Log;
import com.android.toast.Toast;
import com.android.toast.R;

public class ToastActivity extends Activity {
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            String text = (String) msg.obj;
            Log.logV("ToastActivity", text);
            Toast.makeText(ToastActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toast);
        setTitle("ToastAActivity");
    }

    public void testShortTime(View view) {
        Toast.makeText(ToastActivity.this, "short duration Toast", Toast.LENGTH_SHORT).show();
    }

    public void testLongTime(View view) {
        Toast.makeText(ToastActivity.this, "long duration Toast", Toast.LENGTH_LONG).show();
    }

    public void testLongText(View view) {
        String longText = "ndroid是一个开源的，基于Linux的移动设备操作系统，主要使用于移动设备，如智能手机和平板电脑。Android是由谷歌及其他公司带领的开放手机联盟开发的。ndroid是一个开源的，基于Linux的移动设备操作系统，主要使用于移动设备，如智能手机和平板电脑。Android是由谷歌及其他公司带领的开放手机联盟开发的。";
        Toast.makeText(ToastActivity.this, longText, Toast.LENGTH_LONG).show();
    }

    static int index = 1;

    public void testMaxCapacity(View view) {
        index++;
        String text = String.format("第%d个Toast", index);
        Message msg = Message.obtain(mHandler);
        msg.obj = text;
        mHandler.sendMessageDelayed(msg, 1000);
        if (index > 25) {
            index = 1;
        }
    }

    /**
     * A 显示Toast中进入到B，B是否可以显示Toast
     *
     * @param view
     */
    public void testStartOtherActivity(View view) {
        // 不能放在 onStop 或者 onDestroyed 方法中，因为此时新的 Activity 已经创建完成，必须在这个新的 Activity 未创建之前关闭这个 WindowManager
        // 调用取消显示会直接导致新的 Activity 的 onCreate 调用显示吐司可能显示不出来的问题，又或者有时候会立马显示然后立马消失的那种效果
        Intent intent = new Intent(ToastActivity.this, ToastBActivity.class);
        startActivity(intent);
    }


    public void testDialog(View view) {
        AlertDialog dialog = new AlertDialog.Builder(ToastActivity.this)
                .setTitle("Dialog")
                .setNeutralButton("ok", null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ToastActivity.this, "Dialog存在的时候显示Toast", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void testToast(View view) {
        new Thread() {
            @Override
            public void run() {
                super.run();

               // HandlerThread
                Looper.prepare();
               // Looper.myLooper();

                //android.widget.Toast.makeText(ToastActivity.this,"11", android.widget.Toast.LENGTH_SHORT).show();
                //android.widget.Toast.makeText(ToastActivity.this,looper, android.widget.Toast.LENGTH_SHORT,"111").show();
                Toast.makeText(ToastActivity.this,"22",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();

        new HandlerThread("11"){
            @Override
            public void run() {
                super.run();

            }

            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                Toast.makeText(ToastActivity.this,"22",Toast.LENGTH_SHORT).show();
            }
        }.start();
    }
}