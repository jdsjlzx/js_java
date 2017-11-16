package com.github.lzyzsd.jsbridge;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by bruce on 10/28/15.
 */
public class BridgeWebViewClient extends WebViewClient {
    private BridgeWebView webView;
    private boolean isPageLoadingFinish;

    public BridgeWebViewClient(BridgeWebView webView) {
        this.webView = webView;
    }

//    @Override
//    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//        String url = null;
//        try {
//            url = request.getUrl().toString();//app level>=21;
//            url = URLDecoder.decode(url, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
//            webView.handlerReturnData(url);
//            return true;
//        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
//            webView.flushMessageQueue();
//            return true;
//        } else {
//            return super.shouldOverrideUrlLoading(view, request);
//        }
//    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // yy://return/   第四步收到Js端的返回数据会回调到这里
            Log.e("", "js端发起通信第四步[java 调用Js的第十三步]");
            Log.e("", "####(js端和java端注册的handler通信第六步)");
            webView.handlerReturnData(url);
            return true;
        } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { // yy://自定义传输协议  拦截url 回调到这里表示收到js端发过来消息
            Log.e("", "js端发起通信第二步[java 调用Js的第十一步]");
            Log.e("", "(####js端和java端注册的handler通信第四步)");
            webView.flushMessageQueue();//收到JS端的消息后第二步要做的事情是java端要回调jsbridge的_fetchQueue方法
            //最终仍然是通过webview.loadUrl(String url)方法
            return true;
        } else {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        /*排除onPageFinished调用多次,加个Boolean变量标记一下*/
        if (BridgeWebView.toLoadJs != null && !isPageLoadingFinish) {
            /*注入初始化JS脚本文件*/
            BridgeUtil.webViewLoadLocalJs(view, BridgeWebView.toLoadJs);
            Log.e("", "注入初始化JS脚本");
        }
        isPageLoadingFinish = true;
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
    }
}