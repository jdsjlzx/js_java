package com.github.lzyzsd.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge {

	private final String TAG = "BridgeWebView";

	public static final String toLoadJs = "WebViewJavascriptBridge.js";
	public static final String testJs = "test.js";
	Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();
	Map<String, BridgeHandler> messageHandlers = new HashMap<String, BridgeHandler>();
	BridgeHandler defaultHandler = new DefaultHandler();

//	private List<Message> startupMessage = new ArrayList<Message>();
//
//	public List<Message> getStartupMessage() {
//		return startupMessage;
//	}
//
//	public void setStartupMessage(List<Message> startupMessage) {
//		this.startupMessage = startupMessage;
//	}

	private long uniqueId = 0;

	public BridgeWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public BridgeWebView(Context context) {
		super(context);
		init();
	}

	/**
	 * 
	 * @param handler
	 *            default handler,handle messages send by js without assigned handler name,
     *            if js message has handler name, it will be handled by named handlers registered by native
	 */
	public void setDefaultHandler(BridgeHandler handler) {
       this.defaultHandler = handler;
	}

    private void init() {
		this.setVerticalScrollBarEnabled(false);
		this.setHorizontalScrollBarEnabled(false);
		this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
		this.setWebViewClient(generateBridgeWebViewClient());
	}

    protected BridgeWebViewClient generateBridgeWebViewClient() {
        return new BridgeWebViewClient(this);
    }

	void handlerReturnData(String url) {
		Log.e("", "js端发起通信第五步[java 调用Js的第十四步]");
		Log.e("", "####(js端和java端注册的handler通信第七步)");
		String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
		//这里的CallBackFunction指向调用flushMessageQueue()中的loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction()
		//方法时的new CallBackFunction()
		CallBackFunction f = responseCallbacks.get(functionName);
		String data = BridgeUtil.getDataFromReturnUrl(url);
		if (f != null) {

			f.onCallBack(data);//所以这里回调到上面的new CallBackFunction()的onCallBack方法
			responseCallbacks.remove(functionName);//回调后从map中移除该接口实例
			return;
		}
	}

	@Override
	public void send(String data) {
		send(data, null);
	}

	@Override
	public void send(String data, CallBackFunction responseCallback) {
		doSend(null, data, responseCallback);
	}

	private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
		Message m = new Message();
		if (!TextUtils.isEmpty(data)) {
			m.setData(data);
		}
		if (responseCallback != null) {
			String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
			responseCallbacks.put(callbackStr, responseCallback);
			m.setCallbackId(callbackStr);
		}
		if (!TextUtils.isEmpty(handlerName)) {
			m.setHandlerName(handlerName);
		}
		queueMessage(m);
	}

	private void queueMessage(Message m) {
//		if (startupMessage != null) {
//			startupMessage.add(m);
//		} else {
//		}
			dispatchMessage(m);
	}

	/**
	 * java端和JS通信的最终出口方法
	 * @param m
	 */
	  void dispatchMessage(Message m) {
        String messageJson = m.toJson();
		  System.out.println("js端发起通信的第十二步 [java 调用Js的第二步]");
		  System.out.println("####(js端和java端注册的handler通信第十四步)");

        //escape special characters for json string
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            this.loadUrl(javascriptCommand);
        }
    }

	void flushMessageQueue() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			/*回调js的方法同时创建新的Java端回调接口并保存在内存中*/
			loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {

				@Override
				public void onCallBack(String data) {
					Log.e("", "js端发起通信第六步[java调用Js的第十五步]");
					Log.e("", "####(js端和java端注册的handler通信第八步)");
					// deserializeMessage
					List<Message> list = null;
					try {
						list = Message.toArrayList(data);
					} catch (Exception e) {
                        e.printStackTrace();
						return;
					}
					if (list == null || list.size() == 0) {
						return;
					}
					for (int i = 0; i < list.size(); i++) {
						Message m = list.get(i);
						String responseId = m.getResponseId();
						// 是否是response
						if (!TextUtils.isEmpty(responseId)) {
							Log.e("", "[java 调用Js的第十六步]");
							CallBackFunction function = responseCallbacks.get(responseId);
							String responseData = m.getResponseData();
							function.onCallBack(responseData);
							responseCallbacks.remove(responseId);
						} else {
							CallBackFunction responseFunction = null;
							// if had callbackId js端发消息时给消息设置了callbackId属性值
							final String callbackId = m.getCallbackId();
							if (!TextUtils.isEmpty(callbackId)) {
								Log.e("", "js端发起通信第七步");
								Log.e("", "####(js端和java端注册的handler通信第九步)");
								responseFunction = new CallBackFunction() {
									@Override
									public void onCallBack(String data) {
										Log.e("", "js端发起通信第十一步");
										Log.e("", "####(js端和java端注册的handler通信第十三步)");
										Message responseMsg = new Message();
										responseMsg.setResponseId(callbackId);
										responseMsg.setResponseData(data);
										responseMsg.setExtraContent("java端回复: 我很好! ");
										queueMessage(responseMsg);
									}
								};
							} else {
								responseFunction = new CallBackFunction() {
									@Override
									public void onCallBack(String data) {
										// do nothing
									}
								};
							}
							BridgeHandler handler;
							if (!TextUtils.isEmpty(m.getHandlerName())) {
								handler = messageHandlers.get(m.getHandlerName());
								Log.e("", "####(js端和java端注册的handler通信第十步)");
							} else {
								handler = defaultHandler;
								Log.e("", "js端发起通信第八步");
							}
							if (handler != null){/*把JS端发送的消息内容通过默认的消息处理者defaultHandler回调给java端,同时
							通过responseFunction的onCallBack回调到queueMessage
							而queueMessage方法里最终会通过loadUrl(javascriptCommand)调用到JS端代码
							这样以来java端和js端就能实现通信*/
								Log.e("", "js端发起通信第九步");
								Log.e("", "####(js端和java端注册的handler通信第十一步)");
								handler.handler(BridgeWebView.this.getContext(),m.getExtraContent(), responseFunction);
							}
						}
					}
				}
			});
		}
	}

	public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
		this.loadUrl(jsUrl);
		//  responseCallbacks.put("_fetchQueue", returnCallback);
		responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
	}

	/**
	 * register handler,so that javascript can call it
	 * 
	 * @param handlerName
	 * @param handler
	 */
	public void registerHandler(String handlerName, BridgeHandler handler) {
		if (handler != null) {
			messageHandlers.put(handlerName, handler);
		}
	}

	/**
	 * call javascript registered handler
	 *
     * @param handlerName
	 * @param data
	 * @param callBack
	 */
	public void callHandler(String handlerName, String data, CallBackFunction callBack) {
		System.out.println("java 调用Js第一步");
		doSend(handlerName, data, callBack);
	}
}
