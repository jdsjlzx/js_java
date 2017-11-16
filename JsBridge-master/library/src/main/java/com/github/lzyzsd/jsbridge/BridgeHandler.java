package com.github.lzyzsd.jsbridge;

import android.content.Context;

public interface BridgeHandler {
	
	void handler(Context mContext,String data, CallBackFunction function);

}
