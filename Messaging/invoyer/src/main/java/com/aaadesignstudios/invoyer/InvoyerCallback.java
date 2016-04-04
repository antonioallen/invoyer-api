package com.aaadesignstudios.invoyer;

import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

/**
 * Created by antonioallen on 4/3/16.
 */
public class InvoyerCallback extends Callback{

    public InvoyerCallback(){

    }

    @Override
    public void successCallback(String channel, Object response) {
        Log.d("PUBNUB", "Success: " + response.toString());
    }

    @Override
    public void connectCallback(String channel, Object message) {
        Log.d("PUBNUB", "Connect: " + message.toString());
    }

    @Override
    public void errorCallback(String channel, PubnubError error) {
        Log.d("PUBNUB", "Error: " + error.toString());
    }

}
