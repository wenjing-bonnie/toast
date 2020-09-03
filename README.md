# 前言
   最近项目中出现一个问题，就是有的手机在关闭系统通知，结果项目中使用的原生Toast在有的手机上竟然不显示了，然后就去查系统源码，发现原来原生的Toast是基于NotificaitionManagerService实现的，难怪有些手机不显示。另外在我的cdsn博客中写了一篇原生Toast的源码分析，希望和大家一起交流。https://blog.csdn.net/nihaomabmt/article/details/108104146
# 系统源码存在的问题
## 1.重复创建Toast
   通常我们在用Toast的时候，都会直接调用下面一行代码来显示Toast，从我们在第二部分分析的显示流程中我们可以看到：`Toast.makeText(mContext, "原生Toast",Toast.LENGTH_SHORT).show();`
通过源码我们可以看到每调用一次这行代码，都会实例化一个Toast，然后加入到NotificationServiceManager的mToastQueue队列中。如果恰好在点击按钮时调用这行代码，很容易会多次调用这行代码，引起重复创建Toast。有时候项目中为了避免重复创建Toast，所以通常会创建一个Toast实例，全局调用这一个Toast实例，例如：
```
private static Toast toast;
public static void showToast(Context context, String content) {
    if (toast == null) {
        toast = Toast.makeText(context, content, Toast.LENGTH_SHORT);
    } else {
        toast.setText(content);
    }
    toast.show();
}
```
上面的这行代码其实是会有内存泄漏的问题。例如当这个Toast在显示的时候，会持有Activity对象，当还未消失的时候，关闭了该Activity，就会导致Activity对象无法回收，引起Activity的内存泄漏。所以针对这个问题，已经在自定义的Toast中进行了改进。从源码中可以看到，每次显示的时候，其实都是取了mToastQueue中的第0个元素来显示，直到显示完才将该元素从集合中删除，那么我们完全可以在加入Toast之前，先去判断下该Toast的显示的文字内容与当前的Toast的文字内容是否一致，如果一致的话，可以先不加入到mToastQueue队列中。在该代码中已针对这点做出了优化。具体在PowerfulToastManagerService中：
```
    protected void enqueueToast(PowerfulToast toast, String content, ITransientPowerfulToast callBack, int duration) {
            //。。。。。。省略其他代码
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
         //。。。。。。省略其他代码
}
```
## 2.原生Toast的显示的死循环
   在原生的Toast显示的时候，这里取出mToastQueue的第0个元素，然后显示出来到最后消失的时候，这个循环一直在执行，一直到Toast显示完的时候，这个循环一直存在，其实为什么这里不直接使用一个`if(record!=null)`来进行判断就可以了呢？这个源码之所以采用这种方式有什么好处，暂时没有想到原因。所以在自定义Toast的时候，已经将该逻辑改了，直接使用的就是if(record!=null)来判断。
```
    void showNextToastLocked() {
        ToastRecord record = mToastQueue.get(0);
        //这块为什么会要用一个死循环的方式呢？
        while (record != null) {
            
        }
     }
```
## 3.原生Toast为系统级别的Toast
  原生Toast在显示的时候，设置的WindowManager.LayoutParams的时候，采用的是下面的这种类型，但是对于自定义的Toast的时候，`params.type = WindowManager.LayoutParams.TYPE_TOAST;`WindowManager的类型分为应用Window、子Window、系统Window。应用Window对应的一个Activity，子Window不能单独存在，必须附属到父Window中，而系统Window在使用的时候，必须声明权限。
  所以我们在自定义的Toast的不能采用这种类型，因为通知权限在关闭后设置显示的类型为TYPE_TOAST会抛`android.view.WindowManager$BadTokenException`这个异常。而系统Window的类型，在使用的时候，会提示用户给到相应的权限，这样在用户体验很差，所以只能采用应用Window，那么使用应用Window类型的时候，就会有另外一个问题，如果在Toast没有消失的时候，关闭Activity的时候，会抛出` android.view.WindowLeaked: Activity`。所以为了避免这种情况，所以监听Activity的生命周期，在Activity关闭的时候，取消所有mToastQueue中的Toast。所以需要在使用自定义Toast的时候，需要先注册该Toast。
```
public class PowerfulToastManagerService implements Application.ActivityLifecycleCallbacks {   
 /**
     * 将application传入用来管理Activity的生命周期
     *
     * @param application
     */
    protected void registerToast(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }
 /**
     * {@link android.app.Application.ActivityLifecycleCallbacks}
     */
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Log.logV(TAG, activity.getClass().getSimpleName() + " , is paused ！ " + " ， size is " + mToastQueue.size());
        cancelAllPowerfulToast();
    }
}
```
# 该自定义控件使用方法
## 1.项目增加依赖Gradle
  ``` 
dependencies {
   implementation project(':libs:toastlibrary')
}
```
## 2.Application中注册Toast
   必须在Application中直接调用`Toast.registerToast(this);`就可以。如果不调用该方法，会在调用的时候抛出 `You must call registerToast(Application application) in Application`,
## 3.使用方式同原生Toast的使用
   `Toast.makeText(ToastActivity.this, "short duration Toast", Toast.LENGTH_SHORT).show();`

