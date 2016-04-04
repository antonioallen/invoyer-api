package com.aaadesignstudios.invoyer;

/**
 * Created by antonioallen on 4/3/16.
 */
public interface InvoyerHistoryListener {

    String TAG = InvoyerHistoryListener.class.getSimpleName();

    void onLastMessage(String channelId, InvoyerMessage invoyerMessage);

    void onEmpty(String channelId);

    void onError(String channelId, String e);

}
