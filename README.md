# Hook方式实现Activity插件化
随着应用程序的功能模块越来越多，复杂度越来越高，导致了应用程序模块之间的耦合度越来越高，App的体积也随之越来越大。与此同时，随着应用程序代码量的不断增大，引入的库越来越多，那么方法数很容易就超过了65535个，占用内存也会随之增大。于是，为了解决上述困境，出现了插件化的思想，其核心理念就是由宿主App去加载和运行插件App。

宿主App是指预先被安装在我们手机上的App，可以独立运行，同时也可以加载插件。插件App是指那些子功能模块的App，它们可以被宿主App加载和运行，同时也可以作为独立App进行单独运行。这样，各个功能模块就可以单独开发，宿主与插件之间，以及插件与插件之间的耦合度就会大大降低，而且灵活性大大提高。与此同时，dex的体积也会随之减小，从而避免65535问题。在内存占用方面，由于我们是只有在使用到某个插件时才会去进行相应的加载，这样就可以减少内存的使用。

插件化的知识体系还是比较多的，包括Java反射原理，ClassLoader加载Dex原理，Android资源的加载，四大组件的加载，Android系统服务的运行原理等等。其中，四大组件的加载是插件化技术的核心，而Activity的插件化则更是重中之重，因此本文主要介绍Activity的插件化。Activity的插件化主要有3种实现方式，分别是反射实现、接口实现和Hook技术实现。目前Hook技术实现是主流，对于前2种技术实现自身了解也很有限，因此本文主要介绍如何利用Hook技术实现Activity的插件化。

![启动插件Activity.gif](https://upload-images.jianshu.io/upload_images/5519943-bdcb7a48c10d0a61.gif?imageMogr2/auto-orient/strip)

### 一、Activity启动流程
首先，需要说明的是，如果一个Activity没有在AndroidManifest中注册，此时如果去启动它的话将会得到ActivityNotFoundException，因为在启动的过程中，存在一个校验的过程，而这个校验则是由AMS来完成的，这个我们无法干预。而我们也很明确的知道，插件App中的Activity预先是不可能在宿主App的AndroidManifest中进行注册的。所以，我们要想要实现Activity的插件化，就要重点去解决这个问题。

那么如何实现Activity的插件化呢，我们的首要任务就是要彻底搞清楚Activity的启动流程，只有这样才能从中找出解决问题的实现方案。在[Activity启动流程源码解析](https://www.jianshu.com/p/a626387cd523)一文中，我们主要从源码的角度分析了普通Activity的启动流程。这个过程主要涉及了两个进程，分别是AMS所在SystemServer进程和应用程序进程，通过Binder机制进行跨进程通信，相互配合，最终完成Activity的启动。

对于我们而言，AMS在SystemServer进程中，我们无法直接进行修改，只能在应用程序进程中做文章。因此，Activity的插件化方案大多是采用占坑的思想，即预先在AndroidManifest.xml中显示注册一个Activity来进行占坑，用来通过AMS的校验，在通过校验之后再用插件Activity替换占坑的Activity。 

通过分析Activity的启动流程，目前Hook技术实现Activity的插件化主要有2种解决方案 ，一种是通过Hook IActivityManager来实现，而另一种则是Hook Instrumentation实现。接下来，我们来看一下具体的实现过程。声明一下，我这边的源码版本是基于Android 8.0的。
### 二、Hook IActivityManager实现Activity插件化
#### 2.1 AndroidManifest.xml中注册占坑Activity
很简单，我们创建一个SubActivity，并且在AndroidManifest.xml中进行注册，目的就是用来占坑。
```java
    <application
        android:name=".application.MyApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".activity.MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".activity.StubActivity" />

    </application>
```
#### 2.2 使用占坑Activity通过AMS校验
仔细想一下，我们的目标其实很简单，就是在AMS执行startActivity()方法之前，将要启动的插件Activity替换成占坑Activity。而调用AMS的startActivity()方法是由AMS在本地的代理对象来完成的，所以我们就把目光聚焦到了这个AMS本地代理对象。

在[Activity启动流程源码解析](https://www.jianshu.com/p/a626387cd523)中我们提到，关于获取AMS代理对象的方式，Android 8.0和之前的版本是有一些差别的。Android 8.0采用的是AIDL的实现方式获取AMS的代理对象，而Android 8.0之前是通过ActivityManagerNative.getDefault()来获取AMS的代理对象的。不过这个对我们影响不是很大，做好兼容处理就行。
```java
    // Android 8.0源码
    public static IActivityManager getService() {
        return IActivityManagerSingleton.get();
    }

    private static final Singleton<IActivityManager> IActivityManagerSingleton =
            new Singleton<IActivityManager>() {
                @Override
                protected IActivityManager create() {
                    final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
                    final IActivityManager am = IActivityManager.Stub.asInterface(b);
                    return am;
                }
            };


    // Android 8.0之前源码
    static public IActivityManager getDefault() {
        return gDefault.get();
    }
     private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
        protected IActivityManager create() {
            IBinder b = ServiceManager.getService("activity");
            if (false) {
                Log.v("ActivityManager", "default service binder = " + b);
            }
            IActivityManager am = asInterface(b);
            if (false) {
                Log.v("ActivityManager", "default service = " + am);
            }
            return am;
        }
    };
```
由上述源码可知，不管是Android 8.0，还是Android之前，最终返回的AMS本地代理对象都是IActivityManager类型的对象。因此，IActivityManager就是一个很好的Hook点，我们只需要去拦截它的startActivity()方法，并且将要启动的插件Activity替换成占坑Activity。为了简单起见，省略了加载插件Activity的过程，直接创建了一个PluginActivity来代表已经加载进来的插件Activity，并且没有在AndroidManifest.xml中进行注册。同时，由于IActivityManager又是一个接口，所以我们完全可以采用动态代理来拦截它的startActivity()方法，具体实现如下：
```java
public class IActivityManagerProxy implements InvocationHandler {

    private static final String TAG = "IActivityManagerProxy";
    private Object mActivityManager;

    public IActivityManagerProxy(Object activityManager) {
        this.mActivityManager = activityManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            Log.e(TAG, "invoke startActivity");
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            // 获取启动PluginActivity的Intent
            Intent pluginIntent = (Intent) args[index];

            // 新建用来启动StubActivity的Intent
            Intent stubIntent = new Intent();
            stubIntent.setClassName("com.lxbnjupt.pluginactivitydemo",
                    "com.lxbnjupt.pluginactivitydemo.activity.StubActivity");
            // 将启动PluginActivity的Intent保存在subIntent中，便于之后还原
            stubIntent.putExtra(HookHelper.PLUGIN_INTENT, pluginIntent);

            // 通过stubIntent赋值给args，从而将启动目标变为StubActivity，以此达到通过AMS校验的目的
            args[index] = stubIntent;
        }
        return method.invoke(mActivityManager, args);
    }
}
```
之后，创建代理类IActivityManagerProxy，并且使用IActivityManagerProxy去替换原来的IActivityManager即可：
```java
    /**
     * Hook IActivityManager
     *
     * @throws Exception
     */
    public static void hookAMS() throws Exception {
        Log.e(TAG, "hookAMS");
        Object singleton = null;
        if (Build.VERSION.SDK_INT >= 26) {
            Class<?> activityManageClazz = Class.forName("android.app.ActivityManager");
            // 获取ActivityManager中的IActivityManagerSingleton字段
            Field iActivityManagerSingletonField = ReflectUtils.getField(activityManageClazz, "IActivityManagerSingleton");
            singleton = iActivityManagerSingletonField.get(activityManageClazz);
        } else {
            Class<?> activityManagerNativeClazz = Class.forName("android.app.ActivityManagerNative");
            // 获取ActivityManagerNative中的gDefault字段
            Field gDefaultField = ReflectUtils.getField(activityManagerNativeClazz, "gDefault");
            singleton = gDefaultField.get(activityManagerNativeClazz);
        }

        Class<?> singletonClazz = Class.forName("android.util.Singleton");
        // 获取Singleton中mInstance字段
        Field mInstanceField = ReflectUtils.getField(singletonClazz, "mInstance");
        // 获取IActivityManager
        Object iActivityManager = mInstanceField.get(singleton);

        Class<?> iActivityManagerClazz = Class.forName("android.app.IActivityManager");
        // 获取IActivityManager代理对象
        Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{iActivityManagerClazz}, new IActivityManagerProxy(iActivityManager));

        // 将IActivityManager代理对象赋值给Singleton中mInstance字段
        mInstanceField.set(singleton, proxy);
    }
```
#### 2.3 还原插件Activity
在AMS执行startActivity()方法之前，我们使用占坑Activity替换插件Activity，从而通过了AMS的校验。但是，我们真正要启动的是插件Activity，那么势必还是要替换回来的。那么，回想一下Activity的启动流程，我们的目标就换成了要在ActivityThread执行handleLaunchActivity()方法之前，将占坑Activity替换回插件Activity。ActivityThread会通过Handler内部类H将代码的逻辑切换到主线程中，H中重写的handleMessage方法会对LAUNCH_ACTIVITY类型的消息进行处理，我们可以将H的mCallback作为Hook点。
```java
public class HCallback implements Handler.Callback {

    private static final int LAUNCH_ACTIVITY = 100;
    Handler mHandler;

    public HCallback(Handler handler) {
        mHandler = handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == LAUNCH_ACTIVITY) {
            Object obj = msg.obj;
            try {
                // 获取启动SubActivity的Intent
                Intent stubIntent = (Intent) ReflectUtils.getField(obj.getClass(), "intent", obj);

                // 获取启动PluginActivity的Intent(之前保存在启动SubActivity的Intent之中)
                Intent pluginIntent = stubIntent.getParcelableExtra(HookHelper.PLUGIN_INTENT);

                // 将启动SubActivity的Intent替换为启动PluginActivity的Intent
                stubIntent.setComponent(pluginIntent.getComponent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mHandler.handleMessage(msg);
        return true;
    }
}
```
由上述代码可知，HCallback实现了Handler.Callback，并重写了handleMessage方法，当收到消息的类型为LAUNCH_ACTIVITY时，将启动占坑Activity的Intent替换为启动插件Activity的Intent。
之后，我们创建HCallback的实例，并且用它来替换H的mCallback：
```java
    /**
     * Hook ActivityThread中Handler成员变量mH
     *
     * @throws Exception
     */
    public static void hookHandler() throws Exception {
        Log.e(TAG, "hookHandler");
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        // 获取ActivityThread中成员变量sCurrentActivityThread字段
        Field sCurrentActivityThreadField = ReflectUtils.getField(activityThreadClazz, "sCurrentActivityThread");
        // 获取ActivityThread主线程对象
        Object currentActivityThread = sCurrentActivityThreadField.get(activityThreadClazz);

        // 获取ActivityThread中成员变量mH字段
        Field mHField = ReflectUtils.getField(activityThreadClazz, "mH");
        // 获取ActivityThread主线程中Handler对象
        Handler mH = (Handler) mHField.get(currentActivityThread);

        // 将我们自己的HCallback对象赋值给mH的mCallback
        ReflectUtils.setField(Handler.class, "mCallback", mH, new HCallback(mH));
    }
```
#### 2.4 测试运行
自定义Application，调用hookAMS()方法、hookHandler() 方法：
```java
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            // 通过Hook IActivityManager实现Activity插件化
            HookHelper.hookAMS();
            HookHelper.hookHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
MainActivity代码：
```java
public class MainActivity extends AppCompatActivity {

    private Button btnStartPluginActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartPluginActivity = (Button) findViewById(R.id.tv_start_plugin_activity);
        btnStartPluginActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PluginActivity.class);
                startActivity(intent);
            }
        });
    }
}
```
运行程序，当我们点击启动插件Activity按钮，发现启动的是插件PluginActivity。
### 三、Hook Instrumentation实现Activity插件化
#### 3.1 替换和还原插件Activity
Hook Instrumentation实现Activity插件化的思想同样也是使用占坑Activity，与Hook IActivityManager不同的地方是替换和还原的地方不同而已，而且相对来说会稍微简洁一些。

由Activity启动流程可知，启动一个Activity的过程中会调用到Instrumentation的execStartActivity()方法，在此方法中我们可以用占坑Activity来替换插件Activity，以此来通过AMS的验证。然后，在回到ActivityThread主线程的performLaunchActivity方法中时，会调用Instrumentation的newActivity方法创建Activity，在此方法中我们可以用插件Activity来替换占坑Activity。
```java
public class InstrumentationProxy extends Instrumentation {

    private Instrumentation mInstrumentation;
    private PackageManager mPackageManager;

    public InstrumentationProxy(Instrumentation instrumentation, PackageManager packageManager) {
        this.mInstrumentation = instrumentation;
        this.mPackageManager = packageManager;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        // 查找要启动的Activity是否已经在AndroidManifest.xml中注册
        List<ResolveInfo> infos = mPackageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        if (infos == null || infos.size() == 0) {
            // 要启动的Activity没有注册，则将启动它的Intent保存在Intent中，便于之后还原
            intent.putExtra(HookHelper.PLUGIN_INTENT, intent.getComponent().getClassName());
            // 替换要启动的Activity为StubActivity
            intent.setClassName(who, "com.lxbnjupt.pluginactivitydemo.activity.StubActivity");
        }
        try {
            Method execMethod = Instrumentation.class.getDeclaredMethod("execStartActivity",
                    Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
            // 通过反射调用execStartActivity方法，将启动目标变为StubActivity,以此达到通过AMS校验的目的
            return (ActivityResult) execMethod.invoke(mInstrumentation, who, contextThread, token,
                    target, intent, requestCode, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        String intentName = intent.getStringExtra(HookHelper.PLUGIN_INTENT);
        if (!TextUtils.isEmpty(intentName)) {
            // 还原启动目标Activity
            return super.newActivity(cl, intentName, intent);
        }
        return super.newActivity(cl, className, intent);
    }
}
```
接着，我们需要创建InstrumentationProxy对象，并且让其替换主线程中的Instrumentation对象即可：
```java
    /**
    /**
     * Hook Instrumentation
     *
     * @param context 上下文环境
     * @throws Exception
     */
    public static void hookInstrumentation(Context context) throws Exception {
        Log.e(TAG, "hookInstrumentation");
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");
        // 获取ActivityThread中成员变量sCurrentActivityThread字段
        Field sCurrentActivityThreadField = ReflectUtils.getField(activityThreadClazz, "sCurrentActivityThread");
        // 获取ActivityThread中成员变量mInstrumentation字段
        Field mInstrumentationField = ReflectUtils.getField(activityThreadClazz, "mInstrumentation");
        // 获取ActivityThread主线程对象(应用程序启动后就会在attach方法中赋值)
        Object currentActivityThread = sCurrentActivityThreadField.get(activityThreadClazz);
        // 获取Instrumentation对象
        Instrumentation instrumentation = (Instrumentation) mInstrumentationField.get(currentActivityThread);
        PackageManager packageManager = context.getPackageManager();
        // 创建Instrumentation代理对象
        InstrumentationProxy instrumentationProxy = new InstrumentationProxy(instrumentation, packageManager);

        // 用InstrumentationProxy代理对象替换原来的Instrumentation对象
        ReflectUtils.setField(activityThreadClazz, "mInstrumentation", currentActivityThread, instrumentationProxy);
    }
```
#### 3.2 测试运行
自定义Application，调用hookInstrumentation()方法：
```java
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            // 通过Hook Instrumentation实现Activity插件化
            HookHelper.hookInstrumentation(base);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
MainActivity的代码我就不贴了，跟Hook IActivityManager实现Activity插件化里面是一毛一样的。同样，运行程序，当我们点击启动插件Activity按钮，发现启动的是插件PluginActivity。
### 总结
Activity的插件化实现过程，实质就是两个字，那就是模仿。通过对Activity启动流程的源码分析，了解系统启动Activity的整个过程，并且模仿系统的行为，找到其中的Hook点，从而最终实现Activity的插件化。





