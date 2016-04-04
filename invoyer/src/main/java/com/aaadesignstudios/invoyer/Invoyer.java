package com.aaadesignstudios.invoyer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.aaadesignstudios.invoyer.utils.RandomString;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.pubnub.api.Callback;
import com.pubnub.api.PnGcmMessage;
import com.pubnub.api.PnMessage;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by antonioallen on 4/3/16.
 */
public class Invoyer implements InvoyerInterface{

    private String TAG = Invoyer.class.getSimpleName();

    private InvoyerListener listener;
    private Context context;
    private Activity activity;
    private Pubnub mPubNub;

    //Users
    private String userA; // Current User Id
    private String channelId;

    //Google Cloud Messaging
    private GoogleCloudMessaging gcm;
    private String gcmRegId;
    private String gcmSenderId;

    //For typing Listner
    private EditText edx;

    public Invoyer(@NonNull InvoyerListener invoyerListener, @NonNull String publishKey, @NonNull String subscribeKey, String gcmSenderId, @NonNull String userA, @NonNull Context context, @NonNull Activity activity){
        //Init
        this.listener = invoyerListener;
        this.context = context;
        this.userA = userA;
        this.activity = activity;

        this.mPubNub = new Pubnub(publishKey, subscribeKey);
        this.mPubNub.setUUID(userA);
        this.gcmSenderId = gcmSenderId;
        gcmRegister();

        if (this.listener != null && this.context != null && this.userA != null && this.activity !=null
                && this.mPubNub !=null){
            listener.onInitialized();
        }else {
            listener.onError("Something is null");
            if (this.listener == null){
                listener.onError("Listener Null");

            }else if (this.context == null){
                listener.onError("Contex Null");

            }else if (this.userA == null){
                listener.onError("UserA Null");

            }else if (this.activity == null){
                listener.onError("Activity Null");

            }else if (this.mPubNub == null){
                listener.onError("PubNub Null");

            }
        }
    }


    @Override
    public void sendMessage(String TYPE, String message) {
        InvoyerMessage invoyMsg = new InvoyerMessage(userA, message, nextMessageId(), TYPE, System.currentTimeMillis());
        try {
            JSONObject json = new JSONObject();
            json.put(InvoyerConstants.JSON_USER_ID, invoyMsg.getSenderId());
            json.put(InvoyerConstants.JSON_MSG_ID, invoyMsg.getMessageId());
            json.put(InvoyerConstants.JSON_TYPE, invoyMsg.getType());
            json.put(InvoyerConstants.JSON_MSG, invoyMsg.getMessage());
            json.put(InvoyerConstants.JSON_TIME, invoyMsg.getTimeStamp());
            publish(InvoyerConstants.JSON_TYPE_DM, json);
        } catch (JSONException e){ e.printStackTrace(); }
        listener.onMessageSent(invoyMsg);

    }

    private void sendTypingIndicator(boolean typing){
        try {
            JSONObject json = new JSONObject();
            json.put(InvoyerConstants.JSON_TYPING_ID, userA);
            json.put(InvoyerConstants.JSON_TYPING_BOOL, typing);
            publishTyping(InvoyerConstants.JSON_TYPE_TYPING, json);
        } catch (JSONException e){ e.printStackTrace(); }
    }

    @Override
    public void initConvo(String channelId) {
        this.channelId = channelId;
        history();
        subscribeToListen();
        subscribeToListenTyping();
    }

    @Override
    public void getLastMessage(final InvoyerHistoryListener historyListener, String channel) {
        Log.e(TAG, "Getting Last Message");
        this.mPubNub.history(channel, 1, false, new Callback() {
            @Override
            public void successCallback(String channel, final Object message) {
                try {
                    JSONArray json = (JSONArray) message;
                    if (json != null){
                        Log.d("Last Message Array", json.toString());
                        JSONArray messages = json.getJSONArray(0);
                        if (messages != null){
                            if (messages.length() > 0){
                                JSONObject jsonObj = (JSONObject) messages.get(0);
                                if (jsonObj != null){
                                    if (jsonObj.has("type")) { // Checking if object has type
                                        if (jsonObj.getString("type").contains(InvoyerConstants.JSON_TYPE_DM)) {

                                            JSONObject messageObj = jsonObj.getJSONObject("data");
                                            String userId = messageObj.getString(InvoyerConstants.JSON_USER_ID);
                                            String msg = messageObj.getString(InvoyerConstants.JSON_MSG);
                                            String msgId = messageObj.getString(InvoyerConstants.JSON_MSG_ID);
                                            String type = messageObj.getString(InvoyerConstants.JSON_TYPE);
                                            long time = messageObj.getLong(InvoyerConstants.JSON_TIME);
                                            InvoyerMessage invoyerMsg = new InvoyerMessage(userId, msg, msgId, type, time);

                                            Log.e(TAG, "Got Last Message");

                                            historyListener.onLastMessage(channel, invoyerMsg);

                                        }
                                    }else {
                                        historyListener.onEmpty(channel);
                                    }
                                }else {
                                    historyListener.onEmpty(channel);
                                }
                            }else {
                                historyListener.onEmpty(channel);
                            }


                        }else {
                            historyListener.onEmpty(channel);
                        }
                    }else {
                        historyListener.onEmpty(channel);
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                    historyListener.onError(channel, e.getMessage());
                }
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.d("History", error.toString());
            }
        });
    }


    @Override
    public void history() {
        this.mPubNub.history(channelId, 1000, false, new Callback() {
            @Override
            public void successCallback(String channel, final Object message) {
                try {
                    JSONArray json = (JSONArray) message;
                    Log.d("History", json.toString());
                    final JSONArray messages = json.getJSONArray(0);
                    for (int i = 0; i < messages.length(); i++) {
                        JSONObject jsonObj = (JSONObject) messages.get(i);
                        if (jsonObj.has("type")) { // Checking if object has type
                            if (jsonObj.getString("type").contains(InvoyerConstants.JSON_TYPE_DM)) {
                                Log.d(TAG, "Received Old Message + " + String.valueOf(i) + "out of " + String.valueOf(messages.length()));

                                JSONObject messageObj = jsonObj.getJSONObject("data");
                                String userId = messageObj.getString(InvoyerConstants.JSON_USER_ID);
                                String msg = messageObj.getString(InvoyerConstants.JSON_MSG);
                                String msgId = messageObj.getString(InvoyerConstants.JSON_MSG_ID);
                                String type = messageObj.getString(InvoyerConstants.JSON_TYPE);
                                long time = messageObj.getLong(InvoyerConstants.JSON_TIME);
                                InvoyerMessage invoyerMsg = new InvoyerMessage(userId, msg, msgId, type, time);

                                listener.onOldMessages(invoyerMsg, messages.length());

                            }
                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.d("History", error.toString());
            }
        });
    }

    @Override
    public void setonTypingListener(EditText editText) {
        edx = editText;
        edx.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Text changed: " + s.toString());
                //Send Typing Indicator
                if (s.length() > 0) {
                    sendTypingIndicator(true);
                } else {
                    sendTypingIndicator(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }



    @Override
    public void signOut() {
        if (channelId != null){
            this.mPubNub.unsubscribe(channelId);
            this.mPubNub.unsubscribe(channelId+InvoyerConstants.INVOYER_TYPING_CHANNEL);
        }
        listener.onSigout();
    }

    @Override
    public void gcmRegister() {
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(context);
            try {
                gcmRegId = getRegistrationId();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (gcmRegId.isEmpty()) {
                registerInBackground();
            } else {
                Toast.makeText(context, "Registration ID already exists: " + gcmRegId, Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("GCM-register", "No valid Google Play Services APK found.");
        }
    }

    @Override
    public void sendNotification(String userId, String channel) {
        PnGcmMessage gcmMessage = new PnGcmMessage();
        JSONObject json = new JSONObject();
        try {
            json.put(InvoyerConstants.GCM_FROM, userId);
            json.put(InvoyerConstants.GCM_CHAT_CHANNEL, channel);
            json.put(InvoyerConstants.GCM_MESSAGE, "You received a new message!");
            gcmMessage.setData(json);

            PnMessage message = new PnMessage(
                    this.mPubNub,
                    userId,
                    new InvoyerCallback(),
                    gcmMessage);

            message.put("pn_debug",true); // Subscribe to yourchannel-pndebug on console for reports
            message.publish();
        }
        catch (JSONException e) { e.printStackTrace(); }
        catch (PubnubException e) { e.printStackTrace(); }
    }

    @Override
    public boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity, InvoyerConstants.PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.e("GCM-check", "This device is not supported.");
                listener.onError("This device is not supported.");
            }
            return false;
        }
        return true;
    }

    @Override
    public void gcmUnregister() {
        new UnregisterTask().execute();
    }

    @Override
    public void storeRegistrationId(String regId) {
        SharedPreferences prefs = activity.getSharedPreferences(InvoyerConstants.CHAT_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(InvoyerConstants.GCM_REG_ID, regId);
        editor.apply();
    }

    @Override
    public String getRegistrationId() {
        SharedPreferences prefs = activity.getSharedPreferences(InvoyerConstants.CHAT_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(InvoyerConstants.GCM_REG_ID, "");
    }


    //Incoming messages
    public void subscribeToListen(){
        Callback subscribeCallback = new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                if (message instanceof JSONObject){
                    try {
                        JSONObject jsonObj = (JSONObject) message;
                        if (jsonObj.has("type")){ // Checking if object has type
                            if (jsonObj.getString("type").contains(InvoyerConstants.JSON_TYPE_DM)){
                                Log.d(TAG, "Recieved New Message");

                                JSONObject json = jsonObj.getJSONObject("data");
                                String userId = json.getString(InvoyerConstants.JSON_USER_ID);
                                String msg  = json.getString(InvoyerConstants.JSON_MSG);
                                String msgId  = json.getString(InvoyerConstants.JSON_MSG_ID);
                                String type = json.getString(InvoyerConstants.JSON_TYPE);
                                long time   = json.getLong(InvoyerConstants.JSON_TIME);
                                if (userId.equals(mPubNub.getUUID())) return; // Ignore own messages
                                final InvoyerMessage invoyerMsg = new InvoyerMessage(userId, msg, msgId,type, time);
                                listener.onMessageSent(invoyerMsg);

                            }
                        }

                    } catch (JSONException e){ e.printStackTrace(); }
                }
                Log.d("PUBNUB", "Channel: " + channel + " Msg: " + message.toString());
            }

            @Override
            public void connectCallback(String channel, Object message) {
                Log.d("Subscribe", "Connected! " + message.toString());
            }
        };
        try {
            mPubNub.subscribe(channelId, subscribeCallback);
        } catch (PubnubException e){ e.printStackTrace(); }
    }

    //Incoming messages
    public void subscribeToListenTyping(){
        Callback subscribeCallback = new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                if (message instanceof JSONObject){
                    try {
                        JSONObject jsonObj = (JSONObject) message;
                        if (jsonObj.has("type")){ // Checking if object has type
                            if (jsonObj.getString("type").contains(InvoyerConstants.JSON_TYPE_TYPING)){

                                Log.d(TAG, "Recieved Typing Indicator");
                                JSONObject json = jsonObj.getJSONObject("data");
                                String userId = json.getString(InvoyerConstants.JSON_TYPING_ID);
                                Boolean typing  = json.getBoolean(InvoyerConstants.JSON_TYPING_BOOL);
                                Log.d("User Id", userId);
                                Log.d("User typing?", String.valueOf(typing));
                                if (userId.equals(mPubNub.getUUID())) return; // Ignore own messages
                                listener.onTyping(userId, typing);
                            }
                        }

                    } catch (JSONException e){ e.printStackTrace(); }
                }
                Log.d("PUBNUB", "Channel: " + channel + " Msg: " + message.toString());
            }

            @Override
            public void connectCallback(String channel, Object message) {
                Log.d("Subscribe", "Connected! " + message.toString());
            }
        };
        try {
            mPubNub.subscribe(channelId+InvoyerConstants.INVOYER_TYPING_CHANNEL, subscribeCallback);
        } catch (PubnubException e){ e.printStackTrace(); }
    }

    /**
     * Extra Async Tasks
     */

    private void registerInBackground() {
        new RegisterTask().execute();
    }

    private void removeRegistrationId() {
        SharedPreferences prefs = activity.getSharedPreferences(InvoyerConstants.CHAT_PREFS, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(InvoyerConstants.GCM_REG_ID);
        editor.apply();
    }

    private void sendRegistrationId(String regId) {
        this.mPubNub.enablePushNotificationsOnChannel(userA, regId, new InvoyerCallback());
    }
    private class RegisterTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String msg="";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                gcmRegId = gcm.register(gcmSenderId);
                msg = "Device registered, registration ID: " + gcmRegId;

                sendRegistrationId(gcmRegId);

                storeRegistrationId(gcmRegId);
                Log.i("GCM-register", msg);
            } catch (IOException e){
                e.printStackTrace();
            }
            return msg;
        }
    }

    private class UnregisterTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }

                // Unregister from GCM
                gcm.unregister();

                // Remove Registration ID from memory
                removeRegistrationId();

                // Disable Push Notification
                mPubNub.disablePushNotificationsOnChannel(userA, gcmRegId);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }



    /**
     *Extras
     */

    private String nextMessageId(){
        RandomString randomString = new RandomString(16);
        return randomString.nextString();
    }

    /**
     * Use PubNub to send any sort of data
     * @param type The type of the data, used to differentiate groupMessage from directMessage
     * @param data The payload of the publish
     */
    public void publish(String type, final JSONObject data){
        if (channelId != null){
            JSONObject json = new JSONObject();
            try {
                json.put("type", type);
                json.put("data", data);
            } catch (JSONException e) { e.printStackTrace(); }

            this.mPubNub.publish(channelId, json, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    super.successCallback(channel, message);
                    listener.onMessageSentSuccessfully();
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    super.errorCallback(channel, error);
                    listener.onMessageFailed();

                }
            });
        }else {
            listener.onError("Please init convo first");
        }

    }

    public void publishTyping(String type, final JSONObject data){
        if (channelId != null){
            JSONObject json = new JSONObject();
            try {
                json.put("type", type);
                json.put("data", data);
            } catch (JSONException e) { e.printStackTrace(); }

            this.mPubNub.publish(channelId+InvoyerConstants.INVOYER_TYPING_CHANNEL, json, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    super.successCallback(channel, message);
                    listener.onMessageSentSuccessfully();
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    super.errorCallback(channel, error);
                    listener.onMessageFailed();

                }
            });
        }else {
            listener.onError("Please init convo first");
        }

    }
}