package com.bye.heyyou.heyyou.fragments.chat;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bye.heyyou.heyyou.ChatActivity;
import com.bye.heyyou.heyyou.R;
import com.bye.heyyou.heyyou.chat.ChatManager;
import com.bye.heyyou.heyyou.chat.SingleChat;
import com.bye.heyyou.heyyou.exceptions.ChatNotFoundException;
import com.bye.heyyou.heyyou.message.MessageTypes;
import com.bye.heyyou.heyyou.message.UserMessage;
import com.bye.heyyou.heyyou.notifications.MessageNotificationManager;

import java.util.Observable;
import java.util.Observer;

public class ChatOverviewFragment extends Fragment implements Observer {
    private static final String username_arg1 = "username";
    private static final String password_arg2 = "password";
    private static final String messageDb_arg3 = "messageDb";

    private String username;
    private String password;
    private String userMessageServerUrl;
    private Intent chatIntent;
    private ChatManager chatManager;

    private String selectedUserIdChatRow;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param username Parameter 1.
     * @param password Parameter 2.
     * @return A new instance of fragment ChatOverviewFragment.
     */
    public static ChatOverviewFragment newInstance(String username, String password, String userMessageServerUrl) {
        ChatOverviewFragment fragment = new ChatOverviewFragment();
        Bundle args = new Bundle();
        args.putString(username_arg1, username);
        args.putString(password_arg2, password);
        args.putString(messageDb_arg3, userMessageServerUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public ChatOverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onPause() {
        super.onPause();
        chatManager.pause();

    }
    @Override
    public void onStop(){
        super.onStop();
        chatManager.pause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        chatManager.unregister();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username = getArguments().getString(username_arg1);
            password = getArguments().getString(password_arg2);
            userMessageServerUrl = getArguments().getString(messageDb_arg3);
        }
        chatIntent = new Intent(super.getActivity(), ChatActivity.class);
        chatManager = new ChatManager(super.getActivity(), username,password,userMessageServerUrl);
        chatManager.addObserver(this);
        chatManager.resume();
    }

    @Override
    public void onResume() {
        super.onResume();
        new MessageNotificationManager(super.getActivity()).resetNumberNewMessages();
        new MessageNotificationManager(super.getActivity()).deleteNotification();
        chatManager.resume();
        new DisplayActiveChats().execute();
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void update(Observable observable, Object data) {
        new DisplayActiveChats().execute();
    }


    /*
displays all current Chats from the User that will be returned from the ChatManager. The Chats have to be stored in the local Database
 */
    private void displayActiveChats() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final LinearLayout availableChatsViewContainer = (LinearLayout) getView().findViewById(R.id.availableChatsContainer);
        for (
                final SingleChat chat : chatManager.getActiveChats()) {
            try {
                final View chatRowView = findChatRow(chat.getOpponentUserID(), availableChatsViewContainer);
                if (chat.getMessages().size() > 1) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView messagePreview = (TextView) chatRowView.findViewById(R.id.messagePreview);
                            messagePreview.setText(chat.getMessages().get(chat.getMessages().size() - 1).getContent());
                        }
                    });

                }
            } catch (ChatNotFoundException e) {
                final View chatRowView = inflater.inflate(R.layout.chatrow, availableChatsViewContainer, false);
                final TextView headline = (TextView) chatRowView.findViewById(R.id.opponentUserID);
                final TextView messagePreview = (TextView) chatRowView.findViewById(R.id.messagePreview);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headline.setText(chat.getOpponentUserID());
                        UserMessage lastMessage = chat.getMessages().get(chat.getMessages().size() - 1);
                        if (lastMessage.getMessageType() == MessageTypes.TEXT) {
                            messagePreview.setText(lastMessage.getContent());
                        }
                        chatRowView.setTag(chat.getOpponentUserID());
                        registerForContextMenu(chatRowView);
                        availableChatsViewContainer.addView(chatRowView);
                    }
                });

            }
        }
    }

    private class DisplayActiveChats extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            displayActiveChats();
            return null;
        }
    }


    private View findChatRow(String userId, View availableChatsViewContainer) throws ChatNotFoundException {
        View chatRowView = availableChatsViewContainer.findViewWithTag(userId);
        if (chatRowView != null) {
            return chatRowView;
        }
        throw new ChatNotFoundException();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_overview, container, false);
    }

    public void onChatClick(View view) {
        Bundle bundle = new Bundle();
        bundle.putString("OpponentUserId", (String) view.getTag());
        chatIntent.putExtras(bundle);
        startActivity(chatIntent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        if (v.getId() == R.id.chatrow) {
            selectedUserIdChatRow = (String) v.getTag();
            inflater.inflate(R.menu.chatrow, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.deleteChatMenuItem:
                chatManager.deleteChatWithUserID(selectedUserIdChatRow);
                try {
                    final LinearLayout availableChatsViewContainer = (LinearLayout) getView().findViewById(R.id.availableChatsContainer);
                    View chatRowView = findChatRow(selectedUserIdChatRow, availableChatsViewContainer);
                    availableChatsViewContainer.removeView(chatRowView);
                } catch (ChatNotFoundException e1) {
                    return false;
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
