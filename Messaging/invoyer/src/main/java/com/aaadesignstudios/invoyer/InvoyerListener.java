package com.aaadesignstudios.invoyer;

import java.util.ArrayList;

/**
 * Created by antonioallen on 4/3/16.
 */
public interface InvoyerListener {

    String TAG = InvoyerListener.class.getSimpleName();

    void onMessageSent(InvoyerMessage invoyerMessage);

    void onOldMessages(InvoyerMessage invoyerMessage, int total);

    void onMessageSentSuccessfully();

    void onMessageFailed();

    void onHistoryLoaded(ArrayList<InvoyerMessage> invoyerMessages);

    void onError(String e);

    void onRegistered();

    void onSigout();

    void onTyping(String userId, boolean b);

    void onInitialized();



}
