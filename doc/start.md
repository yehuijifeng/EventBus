```java
//注册事件
EventBus.getDefault().register(Object subscriber);
```
```java
//注销事件
EventBus.getDefault().unregister(Object subscriber);
```
```java
//发送
EventBus.getDefault().post(Object event);
```
```java
//接收
//这里的方法名不是关键所在，关键是发送的Object和接收的Object需要保持一致
//@Subscribe注解中有三个参数
@Subscribe(threadMode = ThreadMode.MAIN)
public void onEvent(Object event) {
    Log.i("eventbus", event.toString());
}
```
```java
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
```
```java
ThreadMode threadMode()

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
```