package com.socketdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ZQY";
    private static final int CONNECT_SUCCESS = 0;
    private static final int CONNECT_FAILED = 1;
    private static final int UPDATE_RECEIVE = 2;
    private static final int UPDATE_SEND = 3;
    private Button connectBtn;
    private Button disconnectBtn;


//    private Button sendStartSessionBtn;
//    private Button sendStopSessionBtn;
//    private Button sendGetDeviceInfoBtn;
//    private Button lsBtn;
    private TextView sendArgumentText;

    private TextView receiveArgumentText;

    // Socket变量
    private Socket socket;

    // 输入流对象; 接收服务器消息
    private InputStream inputStream;

    // 输入流读取器对象
    private InputStreamReader inputStreamReader ;
    private BufferedReader bufferedReader;

    // 接收服务器发送过来的消息
    private String response;

    // 输出流对象: 发送消息到服务器
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setListener();
    }

    private void initViews() {
        connectBtn = (Button) findViewById(R.id.connect);
        disconnectBtn = (Button) findViewById(R.id.disconnect);

//        sendStartSessionBtn = (Button) findViewById(R.id.send_start_session);
//        sendStopSessionBtn = (Button) findViewById(R.id.send_stop_session);
//        sendGetDeviceInfoBtn = (Button) findViewById(R.id.send_get_device_info);
//        lsBtn = (Button) findViewById(R.id.send_ls);
        sendArgumentText = (TextView) findViewById(R.id.send_argument);
        receiveArgumentText = (TextView) findViewById(R.id.receive_argument);
    }

    private void setListener() {
        connectBtn.setOnClickListener(this);
        disconnectBtn.setOnClickListener(this);

        findViewById(R.id.send_start_session).setOnClickListener(this);
        findViewById(R.id.send_stop_session).setOnClickListener(this);
        findViewById(R.id.send_get_device_info).setOnClickListener(this);
        findViewById(R.id.send_ls).setOnClickListener(this);

        findViewById(R.id.receive_start_session).setOnClickListener(this);
        findViewById(R.id.receive_stop_session).setOnClickListener(this);
        findViewById(R.id.receive_get_device_info).setOnClickListener(this);
        findViewById(R.id.receive_ls).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect: {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        // 创建客户端 & 服务器的连接
                        try {
                            // 创建Socket对象 & 指定服务端的IP 及 端口号
                            // socket = new Socket("192.168.1.172", 8989);
                            socket = new Socket();
                            socket.connect(new InetSocketAddress("192.168.1.172", 8989), 3000);

                            handler.sendEmptyMessage(socket.isConnected() ? CONNECT_SUCCESS : CONNECT_FAILED);

                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "IOException; " + e.toString());
                            handler.sendEmptyMessage(CONNECT_FAILED);
                        }
                    }
                }).start();
                break;
            }
            case R.id.disconnect: {
                // 断开客户端 & 服务器的连接
                try {
                    // 断开 客户端发送到服务器 的连接，即关闭输出流对象OutputStream
                    if (outputStream != null) {
                        outputStream.close();
                    }

                    // 断开 服务器发送到客户端 的连接，即关闭输入流读取器对象BufferedReader
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }

                    // 最终关闭整个Socket连接
                    if (socket != null) {
                        socket.close();
                        // 判断客户端和服务器是否已经断开连接
                        Log.e(TAG, "connect; " + socket.isConnected());
                        Toast.makeText(MainActivity.this, "Socket已断开", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            case R.id.send_start_session: {
                sendMsgToService(4112);
                break;
            }
            case R.id.send_stop_session: {
                sendMsgToService(4128);
                break;
            }
            case R.id.send_get_device_info: {
                sendMsgToService(176);
                break;
            }
            case R.id.send_ls: {
                sendMsgToService(20512);
                break;
            }
            case R.id.receive_start_session: {
                receiveMsgFromService(4112);
                break;
            }
            case R.id.receive_stop_session: {
                receiveMsgFromService(4128);
                break;
            }
            case R.id.receive_get_device_info: {
                receiveMsgFromService(176);
                break;
            }
            case R.id.receive_ls: {
                receiveMsgFromService(20512);
                break;
            }
        }
    }

    private void sendMsgToService(final int msg_id) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // 发送消息给服务器
                try {
                    String text = getJsonMsg(msg_id);

                    // 步骤1：从Socket 获得输出流对象OutputStream
                    // 该对象作用：发送数据
                    outputStream = socket.getOutputStream();

                    // 步骤2：写入需要发送的数据到输出流对象中
                    outputStream.write((text + "\n").getBytes("utf-8"));
                    // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                    // 步骤3：发送数据到服务端
                    outputStream.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String getJsonMsg(int msg_id) {
        String json = "";
        Session session = new Session();
        switch (msg_id) {
            case 4112: {
                // 建立session
                session.token = 0;
                session.msg_id = 4112;
                break;
            }
            case 4128: {
                // 关闭当前session
                session.token = 99;
                session.msg_id = 4128;
                break;
            }
            case 176: {
                // 获取记录仪设备信息
                session.token = 99;
                session.msg_id = 176;
                break;
            }
            case 20512: {
                // LS: 获取"param"中路径的文件名
                session.token = 99;
                session.msg_id = 20512;
                session.param = "/tmp/fuse_d/ -D -S";
                break;
            }
        }
        Gson gson = new Gson();
        json = gson.toJson(session);
        Log.e(TAG, "msg; " + gson.toJson(session));

        Message msg = Message.obtain();
        msg.what = UPDATE_SEND;
        msg.obj = json;
        handler.sendMessage(msg);
        return json;
    }

    private void receiveMsgFromService(final int msg_id) {
        String string = "";
        Gson gson = new Gson();
        switch (msg_id) {
            case 4112: {
                // 建立session后 返回json数据
                // {"rval":0,"msg_id":4112,"param":TokenNumber}
                String json = "{\"rval\":0,\"msg_id\":4112,\"param\":99}";
                Result result = gson.fromJson(json, Result.class);
                if (result != null) {
                    string = "rval = " + result.rval + ", msg_id = " + result.msg_id + ", param = "  + result.param;
                }
                break;
            }
            case 4128: {
                // 关闭当前session
                // {"rval":0,"msg_id":4128}
                String json = "{\"rval\":0,\"msg_id\":4128}";
                Result result = gson.fromJson(json, Result.class);
                if (result != null) {
                    string = "rval = " + result.rval + ", msg_id = " + result.msg_id;
                }
                break;
            }
            case 176: {
                // 获取记录仪设备信息
                // {"rval":0,"msg_id":176，"chip":"a12","fw_ver":"xxx","hw_ver":"xxx","sw_ver":"xxx","video_folder":"xxx","event_folder":"xxx","photo_folder":"xxx"}
                break;
            }
            case 20512: {
                // LS: 获取"param"中路径的文件名
                // System.currentTimeMillis();

                break;
            }
        }

        Message msg = Message.obtain();
        msg.what = UPDATE_RECEIVE;
        msg.obj = string;
        handler.sendMessage(msg);


//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                // 接收服务器消息
//                try {
//                    // 步骤1：创建输入流对象InputStream
//                    inputStream = socket.getInputStream();
//
//                    // 步骤2：创建输入流读取器对象 并传入输入流对象
//                    // 该对象作用：获取服务器返回的数据
//                    inputStreamReader = new InputStreamReader(inputStream);
//                    bufferedReader = new BufferedReader(inputStreamReader);
//
//                    // 步骤3：通过输入流读取器对象 接收服务器发送过来的数据
//                    String response = bufferedReader.readLine();
//
//                    // 步骤4： 通知主线程,将接收的消息显示到界面
//                    Message msg = Message.obtain();
//                    msg.what = UPDATE_RECEIVE;
//                    msg.obj = response;
//                    handler.sendMessage(msg);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT_SUCCESS: {
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    break;
                }
                case CONNECT_FAILED: {
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    break;
                }
                case UPDATE_RECEIVE: {
                    receiveArgumentText.setText("接收数据：" + msg.obj.toString());
                    break;
                }
                case UPDATE_SEND: {
                    sendArgumentText.setText("上传数据：" + msg.obj.toString());
                    break;
                }
            }
        }
    };
}
