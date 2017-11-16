package com.github.lzyzsd.jsbridge.example;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeUtil;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.github.lzyzsd.jsbridge.DefaultHandler;
import com.github.lzyzsd.jsbridge.Message;
import com.google.gson.Gson;

public class MainActivity extends Activity implements OnClickListener {

    private final String TAG = "MainActivity";

    private BridgeWebView webView;

    private Button button;

    private int RESULT_CODE = 0;
    private boolean isPageLoadingFinish;
    private User user;
    private ValueCallback<Uri> mUploadMessage;

    static class Location {
        String address;
    }

    static class User {
        String name;
        Location location;
        String testStr;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (BridgeWebView) findViewById(R.id.webView);

        button = (Button) findViewById(R.id.button);

        button.setOnClickListener(this);

        webView.setDefaultHandler(new DefaultHandler());

        webView.setWebChromeClient(new WebChromeClient() {

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType, String capture) {
                this.openFileChooser(uploadMsg);
            }

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType) {
                this.openFileChooser(uploadMsg);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                pickFile();
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.e("xufeng", "onProgressChanged: ===>" + newProgress);
                if (newProgress == 100 && !isPageLoadingFinish) {
                    /*测试发现newProgress == 100多数情况会执行两次 0==90==100==100
                    并且newProgress == 100时下一步会回调WebViewClient的onPageFinished
                    有些资料说WebViewClient的onPageFinished我们无法确定它什么时候执行
                    为了封装的封闭原则我们把注入JS代码的初始化操作放在WebViewClient实现
                    这里对应的实现类是BridgeWebViewClient
                    onProgressChanged: ===>0
                    onProgressChanged: ===>90
                    onProgressChanged: ===>100
                    onPageFinished
                    onProgressChanged: ===>100
                    onPageFinished*/
                }
            }
        });

        webView.loadUrl("file:///android_asset/demo.html");
/*java端注册一个名为submitFromWeb的handler,当js需要调用本地方法时可以通过调用window.WebViewJavascriptBridge.callHandler
* 这个方法,该方法通过传入在Java端定义handlerName“submitFromWeb”从而和java端建立联系,最终会回调到这里的handler方法达到JS调
* java的目的,JS端的数据通过data传递过来,java端的回调数据通过*/
        webView.registerHandler("submitFromWeb", new BridgeHandler() {

            @Override
            public void handler(Context mContext, String data, CallBackFunction function) {
                Toast.makeText(mContext, "handler = submitFromWeb, js端传递的数据 " + data, Toast.LENGTH_LONG).show();
                Log.e("", "####(js端和java端注册的handler通信第十二步)");
                function.onCallBack("java端回复的消息   ");
            }
        });

        user = new User();
        Location location = new Location();
        location.address = "SDU";
        user.location = location;
        user.name = "大头鬼";
        //    webView.send("hello");

    }

    public void pickFile() {
        Intent chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooserIntent.setType("image/*");
        startActivityForResult(chooserIntent, RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RESULT_CODE) {
            if (null == mUploadMessage) {
                return;
            }
            try {
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (button.equals(v)) {
            /*java调用Js,原理是通过webview.loadurl方法调用到注入的WebViewJavascriptBridge.js的方法,WebViewJavascriptBridge.js
            * 又可以和html页面交互,这样通过WebViewJavascriptBridge.js这样一个桥梁完成java和JS的通信*/
            webView.callHandler("functionInJs", new Gson().toJson(user), new CallBackFunction() {

                @Override
                public void onCallBack(String data) {
                    Log.e("", "[java 调用Js的第十七步],回调到java端调用处的接口方法,至此java调js过程结束");
                    Toast.makeText(MainActivity.this, "js回复的数据:   " + data, Toast.LENGTH_SHORT).show();
                }

            });
        }

    }

}
