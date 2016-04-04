package com.aaadesignstudios.messaging;

import android.app.ListActivity;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aaadesignstudios.invoyer.Invoyer;
import com.aaadesignstudios.invoyer.InvoyerConstants;
import com.aaadesignstudios.invoyer.InvoyerHistoryListener;
import com.aaadesignstudios.invoyer.InvoyerListener;
import com.aaadesignstudios.invoyer.InvoyerMessage;

import java.util.ArrayList;

public class MainActivity extends ListActivity implements InvoyerListener {
    private Button mChannelView;
    private EditText mMessageET;
    private MenuItem mHereNow;
    private TextView tv_Typing;
    private ListView mListView;
    private ChatAdapter mChatAdapter;
    private String channel  = "MainChat";
    private String userId;

    private Invoyer invoyer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.userId = getIntent().getStringExtra("userId");
        if (this.userId == null){
            startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        this.tv_Typing = (TextView) findViewById(R.id.tv_typing);
        tv_Typing.setVisibility(View.GONE);
        this.mListView = getListView();
        this.mChatAdapter = new ChatAdapter(this, new ArrayList<InvoyerMessage>());
        this.mChatAdapter.userPresence(this.userId, "join"); // Set user to online. Status changes handled in presence
        setupAutoScroll();
        this.mListView.setAdapter(mChatAdapter);
        setupListView();

        this.mMessageET = (EditText) findViewById(R.id.message_et);
        this.mChannelView = (Button) findViewById(R.id.channel_bar);
        this.mChannelView.setText(this.channel);

        initInvoyer();

        invoyer.setonTypingListener(mMessageET);
        mChannelView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, "Signing out", Toast.LENGTH_SHORT).show();
                invoyer.signOut();
                return false;
            }
        });
        mChannelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("MChannel", "Channel Clicked");

                invoyer.getLastMessage(new InvoyerHistoryListener() {
                    @Override
                    public void onLastMessage(String channelId, InvoyerMessage invoyerMessage) {
                        Log.e(TAG, "Received Last Message: " + invoyerMessage.getMessage());

                    }

                    @Override
                    public void onEmpty(String channelId) {
                        Toast.makeText(MainActivity.this, "Empty: "+ channelId, Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onError(String channelId, String e) {
                        Log.e("Error", channelId+" "+e);

                    }
                }, channel);
            }
        });
    }

    private void initInvoyer(){
        invoyer = new Invoyer(this, "pub-c-9e51a08a-2bbf-4715-9dd7-77bf1c2a4460",
                "sub-c-19a3e694-eee7-11e5-8126-0619f8945a4f", "924323551429", userId,
                this, this);
    }

    public void sendMessage(View view){
        String message = mMessageET.getText().toString();
        if (message.equals("")) return;
        mMessageET.setText("");
        invoyer.sendMessage(InvoyerConstants.CHAT_MSG_TYPE, message);
    }

    /**
     * Setup the listview to scroll to bottom anytime it receives a message.
     */
    private void setupAutoScroll(){
        this.mChatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mListView.setSelection(mChatAdapter.getCount() - 1);
                // mListView.smoothScrollToPosition(mChatAdapter.getCount()-1);
            }
        });
    }

    /**
     * On message click, display the last time the user logged in.
     */
    private void setupListView(){
        this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InvoyerMessage chatMsg = mChatAdapter.getItem(position);
                //sendNotification(chatMsg.getUsername());
            }
        });
    }


    @Override
    public void onMessageSent(final InvoyerMessage invoyerMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChatAdapter.addMessage(invoyerMessage);
            }
        });

    }

    @Override
    public void onOldMessages(final InvoyerMessage invoyerMessage, final int total) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mChatAdapter.getCount() < total){
                    mChatAdapter.addMessage(invoyerMessage);
                }
                Log.d("Size: ", String.valueOf(mChatAdapter.getCount()) + " Total: " + String.valueOf(total));
            }
        });

    }

    @Override
    public void onMessageSentSuccessfully() {
        Log.d("Main","Message Success Sent: ");
    }

    @Override
    public void onMessageFailed() {
        Log.d("Main","Message Failed");
    }

    @Override
    public void onHistoryLoaded(final ArrayList<InvoyerMessage> invoyerMessages) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <invoyerMessages.size();i++){
                    mChatAdapter.addMessage(invoyerMessages.get(i));
                }
            }
        });
    }

    @Override
    public void onError(String e) {
        Log.e("Error", e);
    }

    @Override
    public void onRegistered() {
        Log.e("Main", "Registered");
    }

    @Override
    public void onSigout() {
        Log.e("Main", "Signed Out");
        Toast.makeText(MainActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    @Override
    public void onTyping(final String userId, final boolean b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (userId.contains(MainActivity.this.userId) != true){
                    if (b == true){
                        //User Typing
                        tv_Typing.setText(userId+" is typing...");
                        tv_Typing.setVisibility(View.VISIBLE);
                        mChannelView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.holo_green_light));
                    }else {
                        //User Not Typing
                        mChannelView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, android.R.color.darker_gray));
                        tv_Typing.setVisibility(View.GONE);
                    }
                }
            }
        });

    }

    @Override
    public void onInitialized() {
        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                String channel  = "MainChat";
                invoyer.initConvo(channel);
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(r, 1000);
    }
}
