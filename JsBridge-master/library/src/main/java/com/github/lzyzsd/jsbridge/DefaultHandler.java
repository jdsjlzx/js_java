package com.github.lzyzsd.jsbridge;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 *
 */
public class DefaultHandler implements BridgeHandler{

	String TAG = "DefaultHandler";
	@Override
	public void handler(Context mContext,String content, CallBackFunction function) {
		Toast.makeText(mContext, "js端发起通信第十步时java端收到JS端数据:  ===>"+content, Toast.LENGTH_SHORT).show();
		Log.e("", "js端发起通信第十步");
		if(function != null){
			/*通过function回调给JS端*/
			function.onCallBack(content);
			Log.e("", "(js端和java端注册的handler通信第十二步)");
		}
	}

}
