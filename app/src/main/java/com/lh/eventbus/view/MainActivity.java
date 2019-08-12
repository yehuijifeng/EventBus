package com.lh.eventbus.view;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lh.eventbus.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private Map<String, Integer> map = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //发射事件
        EventBus.getDefault().post(new MessageEvent(1, "admin"));
        Toast.makeText(this, "结果：" + map.put("test", 1), Toast.LENGTH_LONG).show();
    }


    @Override
    public void onStart() {
        super.onStart();
        //注册事件
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        //注销事件
        EventBus.getDefault().unregister(this);
    }

    public void onEventBus(View view) {
        Toast.makeText(this, "onEventBus：" + map.put("test", 2), Toast.LENGTH_LONG).show();
    }

    //需要发送的对象
    private class MessageEvent {
        private int userId;
        private String userName;

        private MessageEvent(int userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        @Override
        public String toString() {
            return "MessageEvent{" +
                    "userId=" + userId +
                    ", userName='" + userName + '\'' +
                    '}';
        }
    }

    /**
     * 在主线程里面接收到事件
     *
     * @param event 接受特定的对象
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        Log.i("eventbus", event.toString());
        Toast.makeText(this, event.toString(), Toast.LENGTH_LONG).show();
    }
}
