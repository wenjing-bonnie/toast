package com.android.toast;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by wenjing.liu on 2020-08-11 .
 * <p>
 * 参照{@link android.app.NotificationManager }来实现Toast的show/hide/cancel管理
 * 全局管理{@link PowerfulToast}，PowerfulToastManagerService为单实例
 * <p>
 * <p>
 * 2）如果Toast在显示，还没有显示完，又来了一个Toast显示， 序列Toast显示
 *
 * @author wenjing.liu
 */
public class PowerfulToastManagerService implements Application.ActivityLifecycleCallbacks {
    private static final int MAX_PACKAGE_NOTIFICATIONS = 25;
    private static final String TAG = "ToastManagerService";
    private Handler workHandler = new WorkHandler();
    static final int MESSAGE_REACHED = 1;
    static final int MESSAGE_DURATION_REACHED = 2;
    /**
     * 必须保持全局唯一（PowerfulToastManagerService为单实例）
     */
    private ArrayList<PowerfulToastRecord> mToastQueue = new ArrayList<>();

    /**
     * 将application传入用来管理Activity的生命周期
     *
     * @param application
     */
    protected void registerToast(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    /**
     * 将{@link PowerfulToast}加入到队列中
     * 依次显示所有加入到集合中的{@link PowerfulToast}，如果超过{@link #MAX_PACKAGE_NOTIFICATIONS},则将之前的Toast取消
     * <p>
     * 但finish{@link Activity}的时候，取消所有显示的{@link PowerfulToast},而系统的Toast采用的这样方式，并且当Activity关闭之后，仍能继续显示;
     * <p>
     * 但{@link PowerfulToast}不可以！！因为{@link PowerfulToast}依赖的是{@link Activity}的{@link android.view.WindowManager}，
     * 在finish{@link Activity}的时候必须取消所有显示中的{@link PowerfulToast}来释放{@link android.view.WindowManager}
     *
     * @param toast
     * @param content
     */
    protected void enqueueToast(PowerfulToast toast, String content, ITransientPowerfulToast callBack, int duration) {

        PowerfulToastRecord record;
        //依次显示所有的Toast，所有的Toast显示不叠加
        int index = indexOfToastLocked(toast);
        // If it's already in the queue, we update it in place, we don't
        // move it to the end of the queue.
        // 如果新加入的这个Toast已经在{@link mToastQueue}中，我们只更新下该toast显示的内容,并不对该Toast的位置进行改变
        // 如果只调用{Toast.makeText}来显示Toast,就会创建一个新的Toast对象;所以只有{通过实例化mToast，在主动调用mToast.show()}方式才需要更新Toast的内容
        //其实这也是我们在实现showNoRepeat的方式的时候，创建一个Toast实例，通过更改实例的赋值来显示，而不加入到mToastQueue队列中
        //如果在 Toast 消失之前，Toast 持有了当前 Activity，而此时，用户点击了返回键，导致 Activity 无法被 GC 销毁, 这个 Activity 就引起了内存泄露
        Log.logV(TAG, "Now enqueue toast , size is " + mToastQueue.size());
        if (index >= 0) {
            record = mToastQueue.get(index);
            record.update(content, duration);
        } else {
            // Limit the number of toasts that any given package except the android
            // package can enqueue.  Prevents DOS attacks and deals with leaks.
            //如果超出了设置的设置的最大值，直接将之前的Toast取消 {@link #removeAllPowerfulToast()}，区别于源码中直接不在加入
            if (isToastQueueFull()) {
                cancelAllPowerfulToast();
                Log.logV(TAG, "Now has already posted " + MAX_PACKAGE_NOTIFICATIONS + " toasts. Cancel before toasts");
            }
            //如果与正在显示的Toast的内容一致，则不将该Toast加入到Toast队列中；
            //(1)恰好该workHandler的延时MESSAGE_DURATION_REACHED到了在执行remove操作的时候，此时为null，会向下加入这个Toast
            //(2)只要这个Toast没有显示完，则取出来的值不为空，则不会加入到显示mToastQueue队列中
            if (mToastQueue != null && !mToastQueue.isEmpty()) {
                PowerfulToastRecord curRecord = mToastQueue.get(0);
                if (curRecord != null && content.equals(curRecord.content)) {
                    return;
                }
            }
            //将新增的toast加入到队列中
            record = new PowerfulToastRecord(toast, content, callBack, duration);
            mToastQueue.add(record);
            //将索引值指到队列的末尾,指向刚刚插入的Toast

            index = mToastQueue.size() - 1;
        }
        //显示当前的Toast,每次调用{@link PowerfulToast}的{@link #show}的时候都是只显示第一个Toast，
        //而其他Toast通过showNextToastLocked的循环依次将mToastQueue里面的Toast显示完
        // If it's at index 0, it's the current toast.  It doesn't matter if it's
        // new or just been updated.  Call back and tell it to show itself.
        // If the callback fails, this will remove it from the list, so don't
        // assume that it's valid after this.
        if (index == 0) {
            showNextToastLocked();
        }
    }

    /**
     * 取消正在显示的Toast
     *
     * @param toast
     */
    protected void cancelToast(PowerfulToast toast) {
        synchronized (mToastQueue) {
            int index = indexOfToastLocked(toast);
            if (index < 0) {
                return;
            }
            //必须即可隐藏toast
            cancelToastLocked(index);
        }
    }


    /**
     * 当Toast被hide之后，需要将对workHandler的所有消息移除掉
     *
     * @param toast
     */
    protected void finishToken(PowerfulToast toast) {
        synchronized (mToastQueue) {
            int index = indexOfToastLocked(toast);
            if (index < 0) {
                return;
            }
            removePowerfulToastRecordFromQueue(index);
        }
    }

    /**
     * 用来发送隐藏Toast的消息回调
     */
    private class WorkHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_DURATION_REACHED: {
                    PowerfulToastRecord record = (PowerfulToastRecord) msg.obj;
                    handleDurationReached(record);
                    break;
                }
                default:
            }
        }
    }

    /**
     * 在{@link #cancelToastLocked(int)}的时候循环依次将{@link #mToastQueue}中的{@link PowerfulToast}显示完
     */
    private void showNextToastLocked() {
        Log.logV(TAG, "Now show next toast queue  , size is " + mToastQueue.size());
        PowerfulToastRecord record = mToastQueue.get(0);
        if (record == null) {
            return;
        }
        record.callback.show();
        scheduleDurationReachedLocked(record);
    }

    /**
     * 获取当前正在显示的PowerfulToastRecord
     *
     * @return
     */
    private PowerfulToastRecord getCurrentPowerfulToastRecord() {
        return mToastQueue.get(0);
    }

    /**
     * 延时来发送消息来隐藏toast
     *
     * @param record
     */
    private void scheduleDurationReachedLocked(PowerfulToastRecord record) {
        workHandler.removeCallbacksAndMessages(record);
        Message msg = Message.obtain(workHandler, MESSAGE_DURATION_REACHED, record);
        workHandler.sendMessageDelayed(msg, record.duration);
    }

    /**
     * 将已经显示完的Toast从队列中移除
     *
     * @param index
     */
    private void removePowerfulToastRecordFromQueue(int index) {
        if (index < 0 || index > mToastQueue.size()) {
            return;
        }
        PowerfulToastRecord record = mToastQueue.get(index);
        mToastQueue.remove(record);
        workHandler.removeCallbacksAndMessages(record);
    }

    /**
     * 将队列中的所有Toast移除
     * 该方法调用发生在：还没有显示完Toast的时候，用户主动关闭Activity的情况下，需要将队列中的Toast移除，并且释放资源
     */
    private void cancelAllPowerfulToast() {
        if (mToastQueue.isEmpty()) {
            return;
        }
        Log.logV(TAG, "Now removing all toast , size is " + mToastQueue.size());
        synchronized (mToastQueue) {
            //不能直接调用{@link #cancelToastLocked(int)},因为在这里还有显示下一个Toast的操作
            for (PowerfulToastRecord record : mToastQueue) {
                record.callback.hide();
                workHandler.removeCallbacksAndMessages(record);
            }
            mToastQueue.clear();
        }
        workHandler.removeCallbacksAndMessages(null);
        Log.logV(TAG, "Now removed all toast  , size is " + mToastQueue.size());
    }


    /**
     * 隐藏toast
     *
     * @param record
     */
    private void handleDurationReached(PowerfulToastRecord record) {
        synchronized (mToastQueue) {
            int index = indexOfToastLocked(record.mToast);
            if (index < 0) {
                return;
            }
            cancelToastLocked(index);
        }
    }

    /**
     * 取消当前的Toast,并且显示mToastQueue中未显示的Toast
     *
     * @param index 在mToastQueue集合中的索引值
     */
    private void cancelToastLocked(int index) {
        PowerfulToastRecord record = mToastQueue.get(index);
        record.callback.hide();
        removePowerfulToastRecordFromQueue(index);
        //继续循环显示未显示完的Toast
        if (mToastQueue.isEmpty()) {
            return;
        }
        // Show the next one. If the callback fails, this will remove
        // it from the list, so don't assume that the list hasn't changed
        // after this point.
        showNextToastLocked();
    }

    /**
     * 获取当前的Toast在队列中的位置
     *
     * @param toast
     * @return
     */
    private int indexOfToastLocked(PowerfulToast toast) {
        ArrayList<PowerfulToastRecord> list = mToastQueue;
        int len = list.size();
        for (int i = 0; i < len; i++) {
            PowerfulToastRecord record = list.get(i);
            if (record.mToast.equals(toast)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * ToastQueue是不是已经最大值
     *
     * @return
     */
    private boolean isToastQueueFull() {
        int size = mToastQueue.size();
        return size >= MAX_PACKAGE_NOTIFICATIONS;
    }

    /**
     * {@link android.app.Application.ActivityLifecycleCallbacks}
     */

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Log.logV(TAG, activity.getClass().getSimpleName() + " , is paused ！ " + " ， size is " + mToastQueue.size());
        cancelAllPowerfulToast();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

}
