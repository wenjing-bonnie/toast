package com.android.toast;


import androidx.annotation.NonNull;

/**
 * Created by wenjing.liu on 2020-08-11.
 * {@link PowerfulToastManagerService 中进行对Toast的管理}
 * 对要实现的Toast队列中的Toast对象进行封装
 *
 * @author wenjing.liu
 */
public class PowerfulToastRecord {

    PowerfulToast mToast;
    String content;
    ITransientPowerfulToast callback;
    int duration;

    /**
     * @param toast    PowerfulToast对象
     * @param content  PowerfulToast显示的内容
     * @param callback PowerfulToast的中的hide/cancel回调
     * @param duration 延时隐藏时间
     */
    protected PowerfulToastRecord(PowerfulToast toast, String content, ITransientPowerfulToast callback, int duration) {
        this.mToast = toast;
        this.content = content;
        this.callback = callback;
        this.duration = duration;
    }

    protected void update(String content, int duration) {
        this.duration = duration;
        this.content = content;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("content = %s , duration = %s", this.content, this.duration);
    }
}
