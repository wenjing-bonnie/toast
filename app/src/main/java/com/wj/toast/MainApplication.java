package com.wj.toast;

import android.app.Application;

import com.android.toast.Toast;

/**
 * Created by wenjing.liu on 2020-09-01 .
 *
 * @author wenjing.liu
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.registerToast(this);
    }
}
