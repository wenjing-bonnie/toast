package com.android.test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.toast.Log;
import com.android.toast.R;
import com.android.toast.Toast;

public class ToastBActivity extends Activity {
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            String text = (String) msg.obj;
            Log.logV("ToastActivity", text);
            Toast.makeText(ToastBActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toast_b);
        setTitle("ToastBActivity");
    }

    public void testShortTime(View view) {
        Toast.makeText(ToastBActivity.this, "short duration Toast", Toast.LENGTH_SHORT).show();
    }

    public void testLongTime(View view) {
        Toast.makeText(ToastBActivity.this, "long duration Toast",Toast.LENGTH_LONG).show();
    }

    public void testLongText(View view) {
        String longText = "ndroid是一个开源的，基于Linux的移动设备操作系统，主要使用于移动设备，如智能手机和平板电脑。Android是由谷歌及其他公司带领的开放手机联盟开发的。ndroid是一个开源的，基于Linux的移动设备操作系统，主要使用于移动设备，如智能手机和平板电脑。Android是由谷歌及其他公司带领的开放手机联盟开发的。";
        Toast.makeText(ToastBActivity.this, longText, Toast.LENGTH_LONG).show();
    }

    static int index = 1;

    public void testMaxCapacity(View view) {
        index++;
        String text = String.format("第%d个Toast", index);
        int duration = index / 2 == 0 ? android.widget.Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Message msg = Message.obtain(mHandler, duration, text);
        mHandler.sendMessageDelayed(msg, 1000);
        if (index > 10) {
            index = 1;
        }
    }

    /**
     * A 显示Toast中进入到B，B是否可以显示Toast
     *
     * @param view
     */
    public void testStartOtherActivity(View view) {
        finish();
    }
}
