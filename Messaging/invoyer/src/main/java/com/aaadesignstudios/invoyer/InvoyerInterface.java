package com.aaadesignstudios.invoyer;

import android.widget.EditText;

/**
 * Created by antonioallen on 4/3/16.
 */
public interface InvoyerInterface {

    String TAG = InvoyerInterface.class.getSimpleName();

    void sendMessage(String TYPE, String message);

    void initConvo(String channelId);

    void getLastMessage(InvoyerHistoryListener listener, String channel);

    void history();

    void setonTypingListener(EditText editText);

    void signOut();

    void gcmRegister();

    void sendNotification(String userId, String channel);

    boolean checkPlayServices();

    void gcmUnregister();

    void storeRegistrationId(String id);

    String getRegistrationId();



}
