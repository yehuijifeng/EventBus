# getDefault（初始化）

## getDefault()

```java
//volatile关键字在java并发编程中常用，比synchronized的开销成本要小，轻便
//作用是线程能访问共享变量
//共享变量包括所有的实例变量，静态变量和数组元素，他们都存放在堆内存中
static volatile EventBus defaultInstance;
//一个双重锁定的单例
public static EventBus getDefault() {
    if (defaultInstance == null) {
        synchronized (EventBus.class) {
            if (defaultInstance == null) {
                defaultInstance = new EventBus();
            }
        }
    }
    return defaultInstance;
}

```

```java
//构造方法
public EventBus() {
    this(DEFAULT_BUILDER);//默认构造器
}
```

```java
//默认构造器的初始化
private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
//初始化是一个空构造方法
EventBusBuilder() {
}
```

## EventBus(EventBusBuilder)

```java
//构造方法，里面会初始化一些基础变量。
//使用Builder设计模式，可以生成定制的EventBus单例,或者默认的EventBus单例。
EventBus(EventBusBuilder builder) {
	logger = builder.getLogger();
	//特定的订阅者与之对应的所有需要接收订阅它的方法map
	subscriptionsByEventType = new HashMap<>();
	//object对应的它之下的所有订阅者map
	typesBySubscriber = new HashMap<>();
	//粘性事件存储map
	stickyEvents = new ConcurrentHashMap<>();
	mainThreadSupport = builder.getMainThreadSupport();
    //用于post非ui线程的订阅事件，切换到主线程，对应ThreadMode.Main
	mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
    //用于在后台线程发送订阅事件，对应ThreadMode.BACKGROUND
	backgroundPoster = new BackgroundPoster(this);
    //用于后台异步线程发送订阅事件，对应ThreadMode.ASYNC
	asyncPoster = new AsyncPoster(this);
	indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;
    //*订阅者方法查找类
	subscriberMethodFinder = new SubscriberMethodFinder(builder.subscriberInfoIndexes,
                builder.strictMethodVerification, builder.ignoreGeneratedIndex);//1
	logSubscriberExceptions = builder.logSubscriberExceptions;
	logNoSubscriberMessages = builder.logNoSubscriberMessages;
	sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;
	sendNoSubscriberEvent = builder.sendNoSubscriberEvent;
	throwSubscriberException = builder.throwSubscriberException;
	eventInheritance = builder.eventInheritance;
	executorService = builder.executorService;
}
```

# @Subscribe

**当方法添加了该注解的时候，就可以接收发送者发出的消息了**

```java
//订阅者注解添加后，就可以监听和接收发送来的消息
package org.greenrobot.eventbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//生成javadoc文档
@Documented
//注解的生命周期:注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在
@Retention(RetentionPolicy.RUNTIME)
//注解的作用域:只允许方法使用该注解
@Target({ElementType.METHOD})
//在方法上标记该注解就能够接收到到订阅的特定信息
public @interface Subscribe {
    //订阅者接收信息的线程模式
    ThreadMode t hreadMode() default ThreadMode.POSTING;

    /**
     * 如果为true，则传递最近的粘性事件。
     * 使用EventBus.postStickey(Object)传递给该订阅者(如果事件可用)。
     */
    boolean sticky() default false;

    /** 订阅者优先级，以影响事件交付的顺序。
     * 在同一个交付线程({@link ThreadMode})中，优先级较高的订阅者将比优先级较低的订阅者更早接收事件。	 * 默认优先级为0。
     * 注意:优先级不影响具有不同{@link ThreadMode}的订阅者之间的交付顺序!
     */
    int priority() default 0;
}
```

1. ThreadMode threadMode()
   1. ThreadMode 是enum（枚举）类型
   2. threadMode默认值是POSTING
   3. ThreadMode有四种模式：
      1. POSTING
         1. 订阅者将在发布事件的同一线程中直接调用。
         2. 这是默认值。
         3. 事件交付意味着开销最小，因为它完全避免了线程切换。
         4. 因此，这是推荐的模式，不需要主线程就可以在很短的时间内完成的简单任务。
         5. 事件处理程序使用此模式必须快速返回，以避免阻塞可能是主线程的发布线程。
      2. MAIN
         1. 在Android上，订阅者将在Android的主线程(UI线程)中被调用。
         2. 如果发布线程是主线程，订阅者方法将被直接调用，从而阻塞发布线程。
         3. 否则事件排队等待交付(非阻塞)。
         4. 使用此模式的订阅者必须快速返回，以避免阻塞主线程。
         5. 如果不在Android上，其行为与POSTING相同。
      3. MAIN_ORDERED
         1. 在Android上，订阅者将在Android的主线程(UI线程)中被调用。
         2. 与MAIN不同，事件将始终排队等待交付。
         3. 这确保post调用是非阻塞的。
      4. BACKGROUND
         1. 在Android上，订阅者将在后台线程中调用。
         2. 如果发布线程不是主线程，则将直接在发布线程中调用订阅者方法。
         3. 如果发布线程是主线程，EventBus使用一个后台线程，它将按顺序交付所有事件。
         4. 使用此模式的订阅者应尝试快速返回，以避免阻塞后台线程。
         5. 如果不是在Android上，总是使用后台线程。
      5. ASYNC
         1. 订阅者将在单独的线程中调用。
         2. 这总是独立于发布线程和主线程。
         3. 发布事件从不使用此模式等待订阅者方法。
         4. 如果订阅者方法的执行可能需要一些时间，例如网络访问，则应使用此模式。
         5. 避免同时触发大量长时间运行的异步订阅方方法来限制并发线程的数量。
         6. EventBus使用线程池来有效地重用来自已完成异步订阅方通知的线程。
2. boolean sticky()
   1. sticky（粘性）默认值是false
   2. 如果是true，那么可以通过EventBus的postSticky方法分发最近的粘性事件给该订阅者（前提是该事件可获得）。
3. int priority()
   1. Method的优先级
   2. 优先级高的可以先获得分发的事件。
   3. 这个不会影响不同的ThreadMode的分发事件顺序。也就是同一线程中的先后顺序

# register（注册）

```java
//注册事件接收
EventBus.getDefault().register(this);
```

```java
//注册事件总线，在获取到eventbus单例后就可以注册了
//注册方法中有两个至关重要的方法：
//1. 寻找到当前object对象中所有带@Subscribe注解标记的方法并保存起来
//2. 对当前object对象中所有带@Subscribe注解的方法进行订阅
public void register(Object subscriber) {
    //获得当前对象的class对象
	Class<?> subscriberClass = subscriber.getClass();
    //通过subscriberMethodFinder对象查找到当前class对象下的所有方法，并存入集合
    List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);//1
    //程序锁
    synchronized (this) {
        //将当前对象下的所有方法遍历
        for (SubscriberMethod subscriberMethod : subscriberMethods) {
            //订阅（对象，方法）
            subscribe(subscriber, subscriberMethod);//2
        }
    }
}
```

## findSubscriberMethods（Class）

```java
//缓存当前class对象和对应的所有方法的并发map
private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();
```

```java
//查找对象下的所有方法的源码
List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
    //这里是一个缓存，将当前class对象缓存起来
	List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
	//如果缓存中有，则直接使用缓存的数据
    if (subscriberMethods != null) {
	    return subscriberMethods;
	//是否忽略生成的索引
	if (ignoreGeneratedIndex) {
        //忽略，使用反射的方式查找subscriberMethods
	    subscriberMethods = findUsingReflection(subscriberClass);//1
	} else {
        //不忽略，使用信息的方式查找subscriberMethods
	    subscriberMethods = findUsingInfo(subscriberClass);//2
	}
    //如果当前缓存依然为努力了，则报错：当前传递进来的clas是对象没有附带@Subscribe注解对象
	if (subscriberMethods.isEmpty()) {
	    throw new EventBusException("Subscriber " + subscriberClass
	            + " and its super classes have no public methods with the @Subscribe annotation");
	} else {
        //存储缓存并返回查找的结果
	    METHOD_CACHE.put(subscriberClass, subscriberMethods);
	    return subscriberMethods;
	}
}

```

### findUsingReflection（class）

**通过反射的方式查找subscriberMethods**

#### FindState

```java
static class FindState {
    //当前eventbus中记录的所有订阅者方法
    final List<SubscriberMethod>subscriberMethods = new ArrayList<>();
    //将同一个class的方法存放在同一个class中
    final Map<Class, Object> anyMethodByEventType = new HashMap<>();
    //订阅者的方法key集合
    final Map<String, Class> subscriberClassByMethodKey = new HashMap<>();
    final StringBuilder methodKeyBuilder = new StringBuilder(128);
	//订阅者的class
    Class<?> subscriberClass;
    //当前传递进来的class
    Class<?> clazz;
    //是否跳过父类
    boolean skipSuperClasses;
    //订阅者信息
    SubscriberInfo subscriberInfo;

    //初始化
    void initForSubscriber(Class<?> subscriberClass) {
        //订阅class和当前class为同一个
        this.subscriberClass = clazz = subscriberClass;
        //默认不跳过父类
        skipSuperClasses = false;
        subscriberInfo = null;
    }
	//清除
    void recycle() {
        subscriberMethods.clear();
        anyMethodByEventType.clear();
        subscriberClassByMethodKey.clear();
        methodKeyBuilder.setLength(0);
        subscriberClass = null;
        clazz = null;
        skipSuperClasses = false;
        subscriberInfo = null;
    }
	//检查当前方法的事件类型
    boolean checkAdd(Method method, Class<?> eventType) {
        // 2级检查:只有事件类型的第一级(快速)，如果需要，第二级有完整的签名。
        //这里判断当前订阅服务器是否已经监听了相同的方法参数了
        Object existing = anyMethodByEventType.put(eventType, method);
        //没监听，则返回true
        if (existing == null) {
            return true;
        }
        //已经监听过了该方法，则判断上一个保存的existing是否是Method
        else {
            //如果existing对象是method类型，则订阅服务器则进行二次检查
            if (existing instanceof Method) {
                //如果eventtype保存的上一个value依然不是当前方法和参数，则抛出异常
                if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                    // 抛出异常：非法状态
                    throw new IllegalStateException();
                }
                //官方解释：将任何非方法对象“消费”现有方法
                //这一步的目的暂不清楚，走到这一步，则会返回true
                anyMethodByEventType.put(eventType, this);
            }
            //
            return checkAddWithMethodSignature(method, eventType);
        }
    }
	//检查添加使用的方法签名：1，方法；2，参数的class对象
    //该方法目的是判断当前方法和参数是否已经被存储起来了
    //如果当前的key已经被某个class占用，则返回false
    //如果当前的key没有被占用，或者key存储的class与传递进来的方法的依附class相同，则返回true
    private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
        methodKeyBuilder.setLength(0);
        methodKeyBuilder.append(method.getName());//方法名
        methodKeyBuilder.append('>').append(eventType.getName());//参数名
		//生成一个方法key，key = 方法名>参数名
        String methodKey = methodKeyBuilder.toString();
        //当前方法的类class对象
        Class<?> methodClass = method.getDeclaringClass();
        //将当前class对象存储到对应的key之下
        Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey, methodClass);
        //如果当前key没有被存储或者当前方法的类class对象是否是其父类
        //有可能是为了防止在找父类时覆盖了子类的方法，因为此方法是子类是重写，方法名参数名完全一样（方法签名）；
        //另一个原因是可能是当一个类有多个方法监听同一个event(尽管一般不会这样做)，也能将这些方法加进去。
        if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
            return true;
        } else {
            //如果当前的methodKey已经有值，则这里返回的methodClassOld是上一个存储的值
            //这里还原put，并且返回当前false，当前eventType参数已经保存过了
            subscriberClassByMethodKey.put(methodKey, methodClassOld);
            return false;
        }
    }
	//清除父类class
    void moveToSuperclass() {
        //如果清除父类class状态为true，则清空clazz
        if (skipSuperClasses) {
            clazz = null;
        } else {
            //获得当前clazz的父类class
            clazz = clazz.getSuperclass();
            //获得当前clazz的父类class的名字
            String clazzName = clazz.getName();
            //直接清除当前类的系统级父类，这会提高性能，所有java和javax或者android.开头的都直接清空
            if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                clazz = null;
            }
        }
    }
}

```



```java
//通过反射的方式查找subscriberMethods
private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
    //FindState状态初始化
	FindState findState = prepareFindState();//1
    //寻找状态初始化订阅者
	findState.initForSubscriber(subscriberClass);
    //如果findState中的class对象不为null，则一直继续循环
	while (findState.clazz != null) {
        //使用反射的方式查找subscriberMethods
	    findUsingReflectionInSingleClass(findState);//3
        //移除父类class
	    findState.moveToSuperclass();
	}
    //这个方法非常重要，一个是获取methods，另一个是释放findState里面的map信息
	return getMethodsAndRelease(findState);//2
}

```

#### prepareFindState()

```java
//真个EventBus中只存放4个FinsState对象
private static final int POOL_SIZE = 4;
private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];

```

```java
//初始化FindState
private FindState prepareFindState() {
    //同步锁，缓存当前class对象的并发map
    synchronized (FIND_STATE_POOL) {
        //循环出所有的FindState对象
        for (int i = 0; i < POOL_SIZE; i++) {
            FindState state = FIND_STATE_POOL[i];
            if (state != null) {
                //先将map中的对象置空，这里置空为的是防止并发造成的互相干扰
                FIND_STATE_POOL[i] = null;
                //返回已保存的FindState对象
                return state;
            }
        }
    }
    //map中没有对象则直接new一个FindState对象
    return new FindState();
}

```

#### getMethodsAndRelease（FindState）

```java
//这个方法非常重要，一是获取methods，二是释放findState里面的map信息
private List<SubscriberMethod> getMethodsAndRelease(FindState findState) {
    //将所有方法放入集合中
    List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
    //清空findstate中存储的信息，上一步已经将findstate中subscriberMethods放入了集合中，所以这里清空没有问题
    findState.recycle();//1
    //同步锁
    synchronized (FIND_STATE_POOL) {
        //这里的循环和之前的prepareFindState（）方法相呼应
        //简单来说即使一个复用池，如果当前FindState的数组中有空位置则存入FinsState对象
        for (int i = 0; i < POOL_SIZE; i++) {
            //这里将findstate对象赋值，并发的时候这个复用池用的时候隔离，不用了回收
            if (FIND_STATE_POOL[i] == null) {
                FIND_STATE_POOL[i] = findState;
                break;
            }
        }
    }
    return subscriberMethods;
}

```

#### findUsingReflectionInSingleClass（FindState）

```java
//使用反射的方式查找subscriberMethods
private void findUsingReflectionInSingleClass(FindState findState) {
    Method[] methods;//所有方法集合
    try {
        //这比getMethods更快，特别是当订阅者是像activity这样的臃肿类时
        //查找到所有不包含父类的public方法，有助于增加效率
        methods = findState.clazz.getDeclaredMethods();
    } catch (Throwable th) {
        // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
        //如果报错这里再使用getMethods（）
        methods = findState.clazz.getMethods();
        //跳过了父类方法
        findState.skipSuperClasses = true;
    }
    //循环所有方法
    for (Method method : methods) {
        //获得方法的修饰符
        int modifiers = method.getModifiers();
        //方法是public的且没有包含特定的修饰符
        //int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;
        if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {
            //获得方法的所有参数
            Class<?>[] parameterTypes = method.getParameterTypes();
            //如果参数只有一个，订阅者只能写一个形参的方法
            if (parameterTypes.length == 1) {
                //获得@Subscribe注解，详见@Subscribe标签介绍
                Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                //如果有该注解则继续往下走
                if (subscribeAnnotation != null) {
                    //获得当前方法的参数
                    Class<?> eventType = parameterTypes[0];
                    //检查当前方法和参数，如果没有被添加到eventbus中，则往下走
                    if (findState.checkAdd(method, eventType)) {
                        //获得当前订阅者要求的线程类型
                        ThreadMode threadMode = subscribeAnnotation.threadMode();
                        //subscriberMethods中新添加一个SubscriberMethod（方法，参数，线程类型，优先级，是否分发粘性事件）
                        findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                    }
                }
            }
            //如果strictMethodVerification = true且当前方法有@Subscribe注解且参数不为1个，则抛出异常
            else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                //抛出异常：订阅者方法有且只有一个参数
                throw new EventBusException("@Subscribe method " + methodName +
                        "must have exactly 1 parameter but has " + parameterTypes.length);
            }
        }
        //如果strictMethodVerification = true;且当前方法带有注解@Subscribe则这里抛出异常
        else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
            String methodName = method.getDeclaringClass().getName() + "." + method.getName();
            //抛出异常：非法的@Subscribe方法:必须是公共的、非静态的和非抽象的方法
            throw new EventBusException(methodName +
                    " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
        }
    }
}

```

### findUsingInfo(class)

**使用方法信息查找subscriberMethods**

```java
//用于由注释处理创建的生成索引类的基类
public interface SubscriberInfo {
    Class<?> getSubscriberClass();

    SubscriberMethod[] getSubscriberMethods();

    SubscriberInfo getSuperSubscriberInfo();

    boolean shouldCheckSuperclass();
}

```

```java
//订阅者信息索引
//subscriberInfoIndexes是该对象的一个有序集合arraylist
public interface SubscriberInfoIndex {
    SubscriberInfo getSubscriberInfo(Class<?> subscriberClass);
}

```

```java
//使用方法信息查找subscriberMethods
//EventBus 3.0刚加的功能，特点是比反射快
//在编译期完成subscriber的注册register，而不是在注册期间
private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
    //初始化
	FindState findState = prepareFindState();
	findState.initForSubscriber(subscriberClass);
	while (findState.clazz != null) {
        //获得订阅者的信息
	    findState.subscriberInfo = getSubscriberInfo(findState);//1
	    if (findState.subscriberInfo != null) {
            //获得订阅者订阅的所有方法，这一步很关键，如果没有存储某些方法，需要存储到subscriberMethods中
            //获得当前订阅者信息里存储的所有订阅者方法
	        SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
	        for (SubscriberMethod subscriberMethod : array) {
                //依然检查当前eventbus是否存储了该方法和参数
	            if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                    //没有存储则存储到subscriberMethods中
	                findState.subscriberMethods.add(subscriberMethod);
	            }
	        }
	    } else {
            //如果没有获取到订阅者信息，则依然使用反射的方法去获得findState
	        findUsingReflectionInSingleClass(findState);
	    }
        //清除当前类的父类
	    findState.moveToSuperclass();
	}
    //返回所有当前类记录的需要订阅的方法并且清除Findstatus对象
	return getMethodsAndRelease(findState);
}

```

#### getSubscriberInfo（FindState）

```java
//获得订阅者的信息
private SubscriberInfo getSubscriberInfo(FindState findState) {
    //如果订阅者信息不为null并且订阅者的父类的信息也不为null
    if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
        //将订阅者父类信息赋值给订阅者
        SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
        //如果clazz和订阅者class一致，则返回订阅者信息
        if (findState.clazz == superclassInfo.getSubscriberClass()) {
            return superclassInfo;
        }
    }
    //如果有订阅者信息索引则直接从索引中拿出来
    if (subscriberInfoIndexes != null) {
        for (SubscriberInfoIndex index : subscriberInfoIndexes) {
            //从索引中获取当前findstate的class对象所存储的订阅者信息
            SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
            if (info != null) {
                return info;
            }
        }
    }
    return null;
}

```

**这里有一个延申，订阅者信息索引的保存方法，EventBusBuilder中的addIndex(SubscriberInfoIndex)**

> 注：源码中addIndex这个方法并没有用到，暂不清楚订阅者信息索引如何起作用的

## subscribe(Object,SubscriberMethod)

**当执行register的时候，会将当前注册的对象中的所有带有@Subscribe注解标记的方法都收集起来，上面这部分详细的分析整个收集过程**

```java
//将当前class对象中已经标记了的订阅存起来
private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;

```

```java
//订阅
//参数：
//1. 订阅的object
//2. 当前object中被@Subscribe标记的某个方法
private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
	//获得当前方法的参数的class对象，订阅的方法有且只有一个参数
	Class<?> eventType = subscriberMethod.eventType;
	//new一个Subscription，传入object和subscriberMethod
	Subscription newSubscription = new Subscription(subscriber, subscriberMethod);//1
    //查询当前是否已经保存了订阅对象的Subscription集合
    //CopyOnWriteArrayList并发安全且性能比Vector好
    CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
    //如果当前订阅对象的Subscription不存在则new一个并发安全的集合
	if (subscriptions == null) {
	    subscriptions = new CopyOnWriteArrayList<>();
        //保存起来
	    subscriptionsByEventType.put(eventType, subscriptions);
	} else {
        //如果已经保存了，则判断当前集合中是否已经包含了newSubscription对象
	    if (subscriptions.contains(newSubscription)) {
            //如果包含了，则抛出异常：订阅者已经被标记过了
	        throw new EventBusException("Subscriber " + subscriber.getClass()
                                        + " already registered to event " + eventType);
	    }
	}
	//当前循环是将订阅的优先级实现，优先级高的排序在集合的最上面
	int size = subscriptions.size();
	for (int i = 0; i <= size; i++) {
	    if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
	        subscriptions.add(i, newSubscription);
	        break;
	    }
	}
	//查询当前object中的订阅者有多少个
	List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
    //如果当前object没有记录订阅者则new一个并且保存起来
	if (subscribedEvents == null) {
	    subscribedEvents = new ArrayList<>();
	    typesBySubscriber.put(subscriber, subscribedEvents);
	}
    //添加订阅的class对象
	subscribedEvents.add(eventType);
	//粘性事件
	if (subscriberMethod.sticky) {
        //事件继承？？？
	    if (eventInheritance) {
            //官方解释：
            //现有的事件的事件类型的子类必须被考虑。
            //注意:遍历所有事件可能与很多棘手的事件,效率低下，
            //因此数据结构应该改为允许更有效的查找，
            //额外的map存储子类的超类:class > List<class>;
	        Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
	        for (Map.Entry<Class<?>, Object> entry : entries) {
                //候选人事件类型;
	            Class<?> candidateEventType = entry.getKey();
                //当前方法的类class对象是否是其父类
	            if (eventType.isAssignableFrom(candidateEventType)) {
                    //如果当前类是粘性事件的父类，则检查粘贴事件是否已订阅
	                Object stickyEvent = entry.getValue();
	                checkPostStickyEventToSubscription(newSubscription, stickyEvent);//2
	            }
	        }
	    } else {
            //直接检查粘性事件是否已经post到订阅中去了
	        Object stickyEvent = stickyEvents.get(eventType);
	        checkPostStickyEventToSubscription(newSubscription, stickyEvent);
	    }
	}
}

```

### Subscription

```java
//订阅
final class Subscription {
    //订阅者
    final Object subscriber;
    //所订阅的方法
    final SubscriberMethod subscriberMethod;
    //调用{@link EventBus#invokeSubscriber(PendingPost)}来防止竞争条件
    //队列事件交付{@link EventBus#invokeSubscriber(PendingPost)}检查该对象是否为false。
    volatile boolean active;//是否是有效的

    Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
        active = true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Subscription) {
        	//如果对比对象属于Subscription
            Subscription otherSubscription = (Subscription) other;
            //订阅的object对象相同且订阅的方法相同，则返回true
            return subscriber == otherSubscription.subscriber
                    && subscriberMethod.equals(otherSubscription.subscriberMethod);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        //object的hashcode+方法的hashcode
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode();
    }
}

```

### SubscriberMethod

```java
//内部使用的EventBus和生成的订阅者索引
public class SubscriberMethod {
    final Method method;//订阅的方法
    final ThreadMode threadMode;//订阅发送到线程的模式
    final Class<?> eventType;//订阅的方法参数的class
    final int priority;//发送优先级
    final boolean sticky;//粘性事件
    //用于高效比较
    String methodString;

    public SubscriberMethod(Method method, Class<?> eventType, ThreadMode threadMode, int priority, boolean sticky) {
        this.method = method;
        this.threadMode = threadMode;
        this.eventType = eventType;
        this.priority = priority;
        this.sticky = sticky;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof SubscriberMethod) {
            //检查方法名
            checkMethodString();
            SubscriberMethod otherSubscriberMethod = (SubscriberMethod)other;
            otherSubscriberMethod.checkMethodString();
            //两个方法名作对比
            return methodString.equals(otherSubscriberMethod.methodString);
        } else {
            return false;
        }
    }
	//检查方法名
    private synchronized void checkMethodString() {
        if (methodString == null) {
            //tostring有更多的开销，只取方法的相关部分
            StringBuilder builder = new StringBuilder(64);
            //该方法属于哪个class
            builder.append(method.getDeclaringClass().getName());
            //方法名
            builder.append('#').append(method.getName());
            //参数名
            builder.append('(').append(eventType.getName());
            methodString = builder.toString();
        }
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }
}

```

### checkPostStickyEventToSubscription(Subscription,Object)

```java
//检查粘性事件是否post到了订阅者中
private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
    if (stickyEvent != null) {
        //官方解释
        //如果订阅者试图中止事件，它将失败(事件在发布状态下没有被跟踪)
        //很奇怪的箱子，我们这里不处理。
        //这里详见post源码解析
        postToSubscription(newSubscription, stickyEvent, isMainThread());
    }
}

```

# post（发送）

```java
//post到订阅者中
private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
    //判断订阅者的线程要求
    switch (subscription.subscriberMethod.threadMode) {
        case POSTING://当前线程中
            //调用invokeSubscriber
            invokeSubscriber(subscription, event);
            break;
        case MAIN://主线程中
            if (isMainThread) {
                //如果在主线程中直接调用invokeSubscriber
                invokeSubscriber(subscription, event);
            } else {
                //如果不再主线程中，需要将该粘性事件发送到主线程后再调用invokeSubscriber
                mainThreadPoster.enqueue(subscription, event);
            }
            break;
        case MAIN_ORDERED://在主线程中，但是不会造成阻塞
            if (mainThreadPoster != null) {
                //将该订阅事件发送到主线程去处理
                mainThreadPoster.enqueue(subscription, event);
            } else {
                //官方解释
                //临时的处理：由于发送者和订阅者之间无法耦合，所以技术上是不正确的
                invokeSubscriber(subscription, event);
            }
            break;
        case BACKGROUND://后台线程中
            if (isMainThread) {
                //如果是主线程，则将订阅事件发送到后台线程中
                backgroundPoster.enqueue(subscription, event);
            } else {
                //否则在当前线程中订阅
                invokeSubscriber(subscription, event);
            }
            break;
        case ASYNC://异步耗时线程
            //将订阅事件发送到异步线程中
            asyncPoster.enqueue(subscription, event);
            break;
        default:
            throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
    }
}

```

#### invokeSubscriber(subscription, event)

```java
//调用订阅。订阅者信息，订阅者所属的object
void invokeSubscriber(Subscription subscription, Object event) {
    try {
        //这里使用了java原生的反射invoke方法
        //第一个参数是订阅此event的对象object
        //第二个参数是event
        subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
    } catch (InvocationTargetException e) {
        //处理订阅异常
        handleSubscriberException(subscription, event, e.getCause());
    } catch (IllegalAccessException e) {
        throw new IllegalStateException("Unexpected exception", e);
    }
}

```

#### Poster

```java
// 发送事件
interface Poster {

   /**
    * 将要发布的事件排队用于特定订阅。
    *
    * @param subscription 将接收事件的订阅。
    * @param event 该事件将发布到订阅服务器。
    */
   void enqueue(Subscription subscription, Object event);
}

```

```java
//EventBusBuilder中获得主线程的实现类
MainThreadSupport mainThreadSupport;
MainThreadSupport getMainThreadSupport() {
    //一个eventbus中只有一个实例对象
    if (mainThreadSupport != null) {
        return mainThreadSupport;
    }
    //检查android日志是否可用
    else if (AndroidLogger.isAndroidLogAvailable()) {
        //获得主线程的looper
        Object looperOrNull = getAndroidMainLooperOrNull();//1
        //返回一个实现了MainThreadSupport接口的类
        return looperOrNull == null ? null :
        new MainThreadSupport.AndroidHandlerMainThreadSupport((Looper) looperOrNull);//2
    } else {
        return null;
    }
}

```

```java
//获得Android的主线程的Looper
Object getAndroidMainLooperOrNull() {
    try {
        return Looper.getMainLooper();
    } catch (RuntimeException e) {
        // Not really a functional Android (e.g. "Stub!" maven dependencies)
        return null;
    }
}

```

```java
import android.os.Looper;

//主线程实现类接口
public interface MainThreadSupport {

    boolean isMainThread();

    Poster createPoster(EventBus eventBus);

    class AndroidHandlerMainThreadSupport implements MainThreadSupport {

        private final Looper looper;

        public AndroidHandlerMainThreadSupport(Looper looper) {
            this.looper = looper;
        }

        @Override
        public boolean isMainThread() {
            //检查是否是主线程
            //这里的Looper.myLooper()在android源码中的意义就是主线程的Looper
            return looper == Looper.myLooper();
        }

        @Override
        public Poster createPoster(EventBus eventBus) {
            //new一个poster的handler
            return new HandlerPoster(eventBus, looper, 10);
        }
    }

}

```

```java
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
//poster的Handler类，作用是切换主线程
public class HandlerPoster extends Handler implements Poster {

    private final PendingPostQueue queue;
    private final int maxMillisInsideHandleMessage;
    private final EventBus eventBus;
    private boolean handlerActive;

    protected HandlerPoster(EventBus eventBus, Looper looper, int maxMillisInsideHandleMessage) {
        super(looper);
        this.eventBus = eventBus;
        this.maxMillisInsideHandleMessage = maxMillisInsideHandleMessage;
        queue = new PendingPostQueue();
    }

    public void enqueue(Subscription subscription, Object event) {
        PendingPost pendingPost = PendingPost.obtainPendingPost(subscription, event);
        synchronized (this) {
            queue.enqueue(pendingPost);
            if (!handlerActive) {
                handlerActive = true;
                if (!sendMessage(obtainMessage())) {
                    throw new EventBusException("Could not send handler message");
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        boolean rescheduled = false;
        try {
            long started = SystemClock.uptimeMillis();
            while (true) {
                PendingPost pendingPost = queue.poll();
                if (pendingPost == null) {
                    synchronized (this) {
                        // Check again, this time in synchronized
                        pendingPost = queue.poll();
                        if (pendingPost == null) {
                            handlerActive = false;
                            return;
                        }
                    }
                }
                eventBus.invokeSubscriber(pendingPost);
                long timeInMethod = SystemClock.uptimeMillis() - started;
                if (timeInMethod >= maxMillisInsideHandleMessage) {
                    if (!sendMessage(obtainMessage())) {
                        throw new EventBusException("Could not send handler message");
                    }
                    rescheduled = true;
                    return;
                }
            }
        } finally {
            handlerActive = rescheduled;
        }
    }
}

```

```java
//这里已经进入了post的Handler逻辑
//返回的是HandlerPoster对象
mainThreadPoster = mainThreadSupport.createPoster(EventBus)

```

#### backgroundPoster.enqueue(subscription, event)

```java
//待定

```

#### asyncPoster.enqueue(subscription, event)

```java
//待定

```

#

