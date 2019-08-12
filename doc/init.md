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
