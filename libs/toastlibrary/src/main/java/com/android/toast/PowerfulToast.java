package com.android.toast;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;


/**
 * Created by wenjing.liu on 2020-07-27 .
 * 关闭通知也可以显示的Toast
 *
 * @author wenjing.liu
 */
public class PowerfulToast extends Toast {
    private static final String TAG = "powerful toast";
    private Context mContext;
    private View mNextView;
    private TextView mMessageView;
    private TN mTN;
    private String content;
    private static PowerfulToastManagerService service;
    /**
     * 在 NotificationManagerService中的最终的Toast显示时间
     * scheduleDurationReachedLocked()
     */
    static final int LONG_DELAY = 3500;
    // 2 seconds
    static final int SHORT_DELAY = 2000;

    /**
     * Construct an empty Toast object. You must call {@link #setView} before you
     * can call {@link #show}
     *
     * @param context
     */
    protected PowerfulToast(Context context) {
        this(context, null);
    }

    /**
     * Construct an empty Toast object. If looper is null ,Looper.myLooper() is used.
     * the default is
     *
     * @param context
     * @param looper  {@link android.os.Looper}
     */
    protected PowerfulToast(Context context, Looper looper) {
        super(context);
        mContext = context;
        mTN = new TN(context.getPackageName(), looper);
    }

    /**
     * 单实例
     *
     * @return
     */
    protected static PowerfulToastManagerService getService() {
        if (service != null) {
            return service;
        }
        service = new PowerfulToastManagerService();
        return service;
    }

    @Override
    public void show() {
        if (mNextView == null) {
            throw new RuntimeException("setView must have been called");
        }
        TN tn = mTN;
        tn.mNextView = mNextView;
        tn.toast = this;
        syncToastDefaultPosition(tn);
        //service.，
        getService().enqueueToast(this, content, mTN, mTN.mDuration);
    }

    @Override
    public void cancel() {
        mTN.cancel();
    }

    /**
     * 同{@link android.widget.Toast}仅仅支持设置的两个delay time
     *
     * @param duration
     */
    @Override
    public void setDuration(@com.android.toast.Toast.Duration int duration) {
        mTN.mDuration = duration == Toast.LENGTH_LONG ? LONG_DELAY : SHORT_DELAY;
    }

    /**
     * @param view must contain the {@link TextView} of android.R.id.message
     */
    @Override
    public void setView(View view) {
        mNextView = view;
        formatMessageView(view);
    }

    @Override
    public void setText(CharSequence s) {
        if (mMessageView == null) {
            syncToastDefaultView();
        }
        mMessageView.setText(s);
        content = s.toString();
        mTN.content = content;
    }

    @Override
    public void setText(int resId) {
        setText(mContext.getResources().getString(resId));
    }

    @Override
    public void setGravity(int gravity, int xOffset, int yOffset) {
        super.setGravity(gravity, xOffset, yOffset);
        mTN.mGravity = gravity;
        mTN.mX = xOffset;
        mTN.mY = yOffset;
    }

    @Override
    public void setMargin(float horizontalMargin, float verticalMargin) {
        super.setMargin(horizontalMargin, verticalMargin);
        mTN.mHorizontalMargin = horizontalMargin;
        mTN.mVerticalMargin = verticalMargin;
    }

    /**
     * Synchronize the {@link #Toast} position , in case no set gravity or margin ,
     */
    private void syncToastDefaultPosition(TN tn) {
        tn.mHorizontalMargin = getHorizontalMargin();
        tn.mVerticalMargin = getVerticalMargin();
        tn.mGravity = getGravity();
        tn.mX = getXOffset();
        tn.mY = getYOffset();
    }

    /**
     * Synchronize the {@link #Toast} layout
     */
    private void syncToastDefaultView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @LayoutRes int layout = mContext.getResources().getIdentifier("transient_notification", "layout", "android");
        View v = inflater.inflate(layout, null);
        mMessageView = v.findViewById(android.R.id.message);
       // mMessageView.setBackgroundColor(Color.RED);
        mNextView = v;
    }

    /**
     * Format the toast message view from {@link #setView(View)}
     *
     * @param view
     */
    private void formatMessageView(View view) {
        if (view instanceof TextView) {
            mMessageView = (TextView) view;
            return;
        }
        //instanceof the left can null,and return false
        if (view.findViewById(android.R.id.message) instanceof TextView) {
            mMessageView = view.findViewById(android.R.id.message);
            return;
        }
        syncToastDefaultView();
    }

    /**
     * 使用静态内部类可以避免Activity的内存泄漏
     */
    protected static class TN implements ITransientPowerfulToast {
        private WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();

        private static final int SHOW = 0;
        private static final int HIDE = 1;
        private static final int CANCEL = 2;

        Handler mHandler;
        /**
         * 为了取消toast的时候能够传入到ToastManagerService
         */
        PowerfulToast toast;

        int mGravity;
        int mX, mY;
        float mHorizontalMargin;
        float mVerticalMargin;

        View mView;
        View mNextView;
        String content;
        int mDuration;

        WindowManager mWM;

        String mPackageName;

        TN(String packageName, Looper looper) {

            WindowManager.LayoutParams params = mParams;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            //设置图片的格式
            params.format = PixelFormat.TRANSLUCENT;
            params.windowAnimations = android.R.style.Animation_Toast;
            //去掉TYPE_TOAST：因为通知权限在关闭后设置显示的类型为 Toast 会抛出android.view.WindowManager$BadTokenException
            //去掉TYPE_APPLICATION_OVERLAY：需要权限才能显示
            //params.type = TYPE_BASE_APPLICATION;//getWindowManagerType();
            params.setTitle("Toast");
            params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            mPackageName = packageName;

            if (looper == null) {
                looper = Looper.myLooper();
                if (looper == null) {
                    throw new RuntimeException("Can't toast on a thread that has not called Looper.prepare()");
                }
            }
            mHandler = new Handler(looper, null) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case SHOW: {
                            handleShow();
                            break;
                        }
                        case CANCEL: {
                            handleCancel();
                            break;
                        }
                        case HIDE: {
                            handleHide();
                            //Don't do this in handleHide(),because it is also invoked by handleShow()
                            mNextView = null;
                            break;
                        }
                        default:
                    }
                }
            };

        }

        @Override
        public void show() {
            Log.logV(TAG, content + " is showing ");
            mHandler.obtainMessage(SHOW).sendToTarget();
        }

        @Override
        public void hide() {
            Log.logV(TAG, content + " is hiding");
            mHandler.obtainMessage(HIDE).sendToTarget();
        }

        protected void cancel() {
            Log.logV(TAG, content + " is canceling ");
            mHandler.obtainMessage(CANCEL).sendToTarget();
        }

        private void handleShow() {
            // If a cancel/hide is pending - no need to show - at this point
            // the window token is already invalid and no need to do any work.
            if (mHandler.hasMessages(CANCEL) || mHandler.hasMessages(HIDE)) {
                return;
            }
            //mView为上一个View，默认为null；mNextView为用户调用了setView方法或者初始化的View
            if (mView != mNextView) {
                //remove the old view if necessary
                handleHide();

                mView = mNextView;
                Context context = mView.getContext();
                String packageName = mView.getContext().getPackageName();
                mWM = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                //
                Configuration config = mView.getContext().getResources().getConfiguration();
                int gravity = Gravity.getAbsoluteGravity(mGravity, config.getLayoutDirection());
                mParams.gravity = gravity;
                if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
                    mParams.horizontalWeight = 1.0f;
                }
                if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
                    mParams.verticalWeight = 1.0f;
                }
                mParams.x = mX;
                mParams.y = mY;
                mParams.verticalMargin = mVerticalMargin;
                mParams.horizontalMargin = mHorizontalMargin;

                mParams.packageName = packageName;
                //mParams.token
                Log.logV(TAG, "mView parent =  " + mView.getParent());

                if (mView.getParent() != null) {
                    Log.logV(TAG, "REMOVE! " + mView + " in " + this);
                    mWM.removeView(mView);
                }

                if (!isActivityRunning(context)) {
                    Log.logV(TAG, "The Activity is not running !");
                    return;
                }

                try {
                    mWM.addView(mView, mParams);
                    trySendAccessibilityEvent();
                } catch (Exception e) {
                    // 如果 WindowManager 绑定的 Activity 已经销毁，则会抛出异常
                    // android.view.WindowManager$BadTokenException: Unable to add window -- token android.os.BinderProxy@ef1ccb6 is not valid; is your activity running?
                    isActivityRunning(context);
                    e.printStackTrace();
                }
                //在ToastManagerService中维护着一个Handler来隐藏toast

            }
        }

        private void trySendAccessibilityEvent() {
            //当用户在设置->无障碍里面选择了开启或关闭一个辅助功能，会导致一些系统状态会变化；
            // Accessibility APP的安装状态会以BroadcastReceivers的方式会通知状态改变；
            // 还有其他的一些状态改变。这些变化最终会调用到AMS的onUserStateChangedLocked()方法。
            //第三方的监听/模拟点击服务
            Context context = mView.getContext();
            AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (!accessibilityManager.isEnabled()) {
                return;
            }
            AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
            event.setClassName(getClass().getName());
            event.setPackageName(context.getPackageName());
            mView.dispatchPopulateAccessibilityEvent(event);
            accessibilityManager.sendAccessibilityEvent(event);
        }

        /**
         * The activity is or not running
         *
         * @param context
         * @return
         */
        private boolean isActivityRunning(Context context) {
            if (!(context instanceof Activity)) {
                return false;
            }
            Activity activity = (Activity) context;
            boolean isFinish = activity.isDestroyed() || activity.isFinishing();
            // Log.logV(TAG, activity.getClass().getSimpleName() + " isRunning  = " + !isFinish);
            return !isFinish;

        }

        /**
         * hide the finish toast
         */
        private void handleHide() {
            // Log.logV(TAG, "handleHide");
            if (mView == null) {
                return;
            }
            if (mView.getParent() == null) {
                return;
            }
            mWM.removeViewImmediate(mView);
            //Now that we've removed the view it's safe fow the server to release the sources
            getService().finishToken(toast);
        }

        /**
         * cancel the running toast,
         */
        private void handleCancel() {

            handleHide();
            //Don't do this in handleHide(),because it is also invoked by handleShow()
            mNextView = null;
            //getService().cancelToast(mPackageName, TN.this);
            getService().cancelToast(toast);
        }

        /**
         * @return params.type的版本适配
         */
        private int getWindowManagerType() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                return WindowManager.LayoutParams.TYPE_TOAST;
            }
        }


    }
}
