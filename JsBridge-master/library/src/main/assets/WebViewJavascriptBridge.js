//notation: js file can only use this kind of comments
//since comments will cause error when use in webview.loadurl,
//comments will be remove by java use regexp
(function() {
    if (window.WebViewJavascriptBridge) {
        return;
    }
 console.log('html页面加载到100%时会执行注入的WebViewJavascriptBridge.js脚本');
    var messagingIframe;
    var sendMessageQueue = [];
    var receiveMessageQueue = [];
    //js端收到java端消息时用来处理java端传过来的数据的handler
    var messageHandlers = {};

    var CUSTOM_PROTOCOL_SCHEME = 'yy';
    var QUEUE_HAS_MESSAGE = '__QUEUE_MESSAGE__/';

    var responseCallbacks = {};
    var uniqueId = 1;

    function _createQueueReadyIframe(doc) {
     console.log('html页面执行注入的WebViewJavascriptBridge.js脚本的_createQueueReadyIframe(doc)方法');
        messagingIframe = doc.createElement('iframe');
        messagingIframe.style.display = 'none';
        doc.documentElement.appendChild(messagingIframe);
    }

    //set default messageHandler
    function init(messageHandler) {
        if (WebViewJavascriptBridge._messageHandler) {
            throw new Error('WebViewJavascriptBridge.init called twice');
        }
        console.log('html页面加载初始化执行第二步===>WebViewJavascriptBridge.js的init方法');
         console.log(messageHandler);
        WebViewJavascriptBridge._messageHandler = messageHandler;
        var receivedMessages = receiveMessageQueue;
         console.log("收到消息的队列的长度 :"+receiveMessageQueue.length);
        receiveMessageQueue = null;
        for (var i = 0; i < receivedMessages.length; i++) {
            _dispatchMessageFromNative(receivedMessages[i]);
        }
    }
     //js和java端通信过程: [js端主动搭讪java端,通信的发起方为js端]
    function send(data, responseCallback) {
    //这俩参数是HTML调用处传递过来的
     console.log("js端发起通信第零步");
      console.log(data);
        _doSend({
            data: data
        }, responseCallback);
    }

    function registerHandler(handlerName, handler) {
     console.log('html页面加载初始化执行第三步===>js端注册了一个Handler供java端使用');
        messageHandlers[handlerName] = handler;
         console.log('注册的handler为: '+ handlerName +"  "+handler);
    }

    function callHandler(handlerName, data, responseCallback) {
      console.log("####js端和java端注册的handler通信第二步");
        _doSend({
            handlerName: handlerName,
            data: data
        }, responseCallback);
    }

    //sendMessage add message, 触发native处理 sendMessage
    function _doSend(message, responseCallback) {
     try {
        console.log("js端发起通信第一步"+"[java 调用Js的第九步]"+"(js端和java端注册的handler通信第三步)");
        console.log("(####js端和java端注册的handler通信第三步)");
        console.log(responseCallback);
        console.log(message);
        console.log(message.data);
        if (responseCallback) {
            var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
            message.callbackId = callbackId;
            message.extraContent = 'js端问:你好吗?   ';
        }

        sendMessageQueue.push(message);
        console.log(message);
         console.log(sendMessageQueue);
      //  messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://' + QUEUE_HAS_MESSAGE;
      //  messagingIframe.src会回调到Java端Webview的shouldOverrideUrlLoading方法
        messagingIframe.src = 'yy://__QUEUE_MESSAGE__/';
        }catch(exception){
          console.log(exception);
        }

    }

    // 提供给native调用,该函数作用:获取sendMessageQueue返回给native,由于android不能直接获取返回的内容,所以使用url shouldOverrideUrlLoading 的方式返回内容
    //JS端发送消息Java端拦截到URL后紧着着回调调js端 _fetchQueue()方法,该方法又会重新刷新java端webview,又会回调到java端
    //webview的shouldOverrideUrlLoading方法
    function _fetchQueue() {
       console.log("js端发起通信第三步[java 调用Js的第十二步]");
        console.log("####(js端和java端注册的handler通信第五步)");
        var messageQueueString = JSON.stringify(sendMessageQueue);
        console.log(messageQueueString);
        sendMessageQueue = [];
        //android can't read directly the return data, so we can reload iframe src to communicate with java
        //把js端的回复信息加上message.responseId[其实就是java端传过来的callbackId]转成一定格式的
        //messageQueueString返回给java端,java端通过拦截url解析出回复数据并做相应的业务逻辑
        messagingIframe.src = CUSTOM_PROTOCOL_SCHEME + '://return/_fetchQueue/' + encodeURIComponent(messageQueueString);

    }

    //提供给native使用,
    function _dispatchMessageFromNative(messageJSON) {
     console.log("js端发起通信第十四步"+" [java 调用Js的第四步]" );
     console.log("####(js端和java端注册的handler通信第十六步)");
        setTimeout(function() {
           try {
            var message = JSON.parse(messageJSON);

            console.log(message);
            var responseCallback;
            //java call finished, now need to call js callback function
            //message.responseId是在第十一步是由java端赋值的,message.responseId = message.callbackId
            if (message.responseId) {
                responseCallback = responseCallbacks[message.responseId];
                if (!responseCallback) {
                    return;
                }
                  //注意不能把注释放在js代码的后面,比如: js代码  //这里写注释 会报错 刚开始这样写坑了好久
                  // responseCallback(message.responseData);//把java端的回复消息回调给js端

                  //把java端的回复消息回调给js端
                  responseCallback(message.extraContent);
                delete responseCallbacks[message.responseId];
            } else {
                // 直接发送
                 console.log("java 调用Js的第五步");
                if (message.callbackId) {
                 console.log(message.callbackId);
                    var callbackResponseId = message.callbackId;
                    responseCallback = function(responseData) {
                        _doSend({
                            responseId: callbackResponseId,
                            responseData: responseData
                        });
                    };
                     console.log(responseCallback);
                }
                  //handler为HTML页面初始化时创建的Handler保存在WebViewJavascriptBridge._messageHandler中
                var handler = WebViewJavascriptBridge._messageHandler;
                 console.log(handler);
                console.log("java 调用Js的第六步");
                if (message.handlerName) {
                    handler = messageHandlers[message.handlerName];
                    console.log("java 调用Js的第七步");
                }
                //查找指定handler,回调到HTML初始化时注册的那个handler的的对应方法
                    handler(message.data, responseCallback);
                }
            }catch (exception) {
            console.log(exception);
               if (typeof console != 'undefined') {
                   console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
               }
                    }
        });
    }

    //java调用JS会调用到这里,receiveMessageQueue 在会在页面加载完后赋值为null,所以会执行到else里面的
    // _dispatchMessageFromNative(messageJSON);
    function _handleMessageFromNative(messageJSON) {
        if (receiveMessageQueue && receiveMessageQueue.length > 0) {
            receiveMessageQueue.push(messageJSON);
        } else {
        console.log("js端发起通信第十三步" +"[java 调用Js的第三步]"  );
       console.log("####(js端和java端注册的handler通信第十五步)");
        console.log(messageJSON);
            _dispatchMessageFromNative(messageJSON);
        }
    }

    var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {
        init: init,
        send: send,
        registerHandler: registerHandler,
        callHandler: callHandler,
        _fetchQueue: _fetchQueue,
        _handleMessageFromNative: _handleMessageFromNative
    };
    console.log("'html页面执行完了注入的WebViewJavascriptBridge.js脚本的WebViewJavascriptBridge的构造方法");
    var doc = document;
    _createQueueReadyIframe(doc);
    var readyEvent = doc.createEvent('Events');
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
})();

