package com.android.toast;

import android.app.Application;
import android.content.Context;
import android.os.Looper;

import androidx.annotation.IntDef;
import androidx.annotation.StringRes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by wenjing.liu on 2020-07-27.
 * <p>
 * 调用Toast控件对外提供的API,使用方法同原生Toast。
 * 第一步：调用{@link #registerToast(Application)}。原因在于：{@link PowerfulToast}依赖于Activity的WindowManager，需要在{@link PowerfulToast}主动释放WindowManager
 * 第二步：调用{@link #makeText(Context, int, int)}
 * <p>
 * !!!!!!! 重复内容Toast显示
 *
 * @author wenjing.liu
 */
public class Toast {

    //定义Duration注解，只能有LENGTH_SHORT,LENGTH_LONG两个值
    @IntDef({LENGTH_SHORT, LENGTH_LONG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static final int LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT;
    public static final int LENGTH_LONG = android.widget.Toast.LENGTH_LONG;
    private static android.widget.Toast mToast;
    /**
     * 之所以使用static是因为这样可以全局存在,用来标记是否在Application中注册Toast
     */
    private static boolean isRegisterToast = false;

    /**
     * 必须在Application注册
     *
     * @param application
     */
    public static void registerToast(Application application) {
        PowerfulToast.getService().registerToast(application);
        isRegisterToast = true;
    }

    /**
     * 获取初始化的Toast对象
     *
     * @return
     */
    public static android.widget.Toast getToast() {
        return mToast;
    }

    /**
     * Make a standard toast that just contains a text view.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param resId    The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     */
    public static android.widget.Toast makeText(Context context, @StringRes int resId, @Duration int duration) {
        return makeText(context, null, context.getResources().getText(resId), duration);
    }

    /**
     * Make a standard toast that just contains a text view.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param text     The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     */
    public static android.widget.Toast makeText(Context context, CharSequence text, @Duration int duration) {
        return makeText(context, null, text, duration);
    }

    /**
     * Make a standard toast that just contains a text view.
     *
     * @param context  The context to use.  Usually your {@link android.app.Application}
     *                 or {@link android.app.Activity} object.
     * @param text     The text to show.  Can be formatted text.
     * @param duration How long to display the message.  Either {@link #LENGTH_SHORT} or
     *                 {@link #LENGTH_LONG}
     */
    public static android.widget.Toast makeText(Context context, Looper looper, CharSequence text, @Duration int duration) {
        Context contxt = context;
        android.widget.Toast toast;
        //统一替换成自定义的Toast
//        if (NotificationUtils.isNotificationEnabled(contxt)) {
//            toast = android.widget.Toast.makeText(contxt, text, duration);
//        } else {
        checkRegisterToast();
        toast = new PowerfulToast(contxt, looper);
        toast.setText(text);
        toast.setDuration(duration);
//       }
        mToast = toast;
        return toast;
    }

    /**
     * 监测是否在Application中注册ActivityLifecycleCallbacks
     * 在该控件中需要根据Activity的生命周期进行管理绘制Toast的WindowManager
     */
    private static void checkRegisterToast() {
        /**
         * 是否在Application中注册Toast控件
         * 之所以使用static是因为这样可以全局存在,用来标记是否在Application中注册Toast
         * TODO 有一个问题：如果isRegisterToast被垃圾回收了，怎么办呢？
         */
        if (isRegisterToast) {
            return;
        }
        throw new RuntimeException("You must call registerToast(Application application) in Application");
    }

}
