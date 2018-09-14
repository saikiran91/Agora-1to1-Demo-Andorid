package io.agora.demo1to1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import io.agora.AgoraAPIOnlySignal;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24;
import static io.agora.rtc.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
import static io.agora.rtc.video.VideoEncoderConfiguration.STANDARD_BITRATE;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_640x360;

public class VideoChatViewActivity extends AppCompatActivity {

    private static final String LOG_TAG = VideoChatViewActivity.class.getSimpleName();
    public static final String CHANNEL_ID_KEY = "CHANNEL_ID_KEY";
    public static final String CLIENT_TYPE = "CLIENT_TYPE";
    public static final String USER_NAME = "USER_NAME";
    private String mChannelID;
    private String mUserName;
    private int mClientType;
    private AgoraAPIOnlySignal mSignaling;
    private Gson mGson = new GsonBuilder().setPrettyPrinting().create();
    private ChatAdapter mChatAdapter;

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private boolean mShowInfo = false;

    private RtcEngine mRtcEngine;// Tutorial Step 1
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1
        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) { // Tutorial Step 5
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) { // Tutorial Step 7
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft();
                }
            });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean muted) { // Tutorial Step 10
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserVideoMuted(uid, muted);
                }
            });
        }

        @Override
        public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
            super.onFirstLocalVideoFrame(width, height, elapsed);
        }

        @Override
        public void onUserEnableLocalVideo(int uid, boolean enabled) {
            super.onUserEnableLocalVideo(uid, enabled);
        }

        @Override
        public void onRemoteVideoStats(final RemoteVideoStats stats) {
            super.onRemoteVideoStats(stats);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mShowInfo && isAudience()) {
                        TextView detailsTv = findViewById(R.id.video_detail_tv);
                        detailsTv.setVisibility(View.VISIBLE);
                        String details = "Res: " + stats.width + "w " + stats.height + "h" + "\n" +
                                "Bitrate: " + stats.receivedBitrate + "\n" +
                                "FrameRate: " + stats.receivedFrameRate + "\n" +
                                "uid: " + stats.uid + "\n" +
                                "delay: " + stats.delay + "\n" +
                                "StreamType: " + stats.rxStreamType + "\n" +
                                "Channel ID:" + mChannelID;
                        detailsTv.setText(details);
                    }
                }
            });

        }

        @Override
        public void onLocalVideoStats(final LocalVideoStats stats) {
            super.onLocalVideoStats(stats);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mShowInfo && isBroadcaster()) {
                        TextView detailsTv = findViewById(R.id.video_detail_tv);
                        detailsTv.setVisibility(View.VISIBLE);
                        String details = "Res: Na" + "\n" +
                                "Bitrate: " + stats.sentBitrate + "\n" +
                                "FrameRate: " + stats.sentFrameRate;
                        detailsTv.setText(details);
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat_view);
        mChannelID = getIntent().getStringExtra(CHANNEL_ID_KEY);
        mClientType = getIntent().getIntExtra(CLIENT_TYPE, Constants.CLIENT_ROLE_AUDIENCE);
        mUserName = getIntent().getStringExtra(USER_NAME);
        Log.d(LOG_TAG, "mChannelID = " + mChannelID +
                " mClientType = " + mClientType + " mUserName = " + mUserName);
        initUI();
        if (mChannelID == null) throw new RuntimeException("Channel ID cannot be null");
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)) {
            initAgoraEngineAndJoinChannel();
        }


        mChatAdapter = new ChatAdapter();
        RecyclerView recyclerView = findViewById(R.id.chat_list);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(mChatAdapter.getAdapter());
    }

    private void initUI() {
        if (isAudience()) {
            findViewById(R.id.mute_video_btn).setVisibility(View.GONE);
            findViewById(R.id.mute_audio_btn).setVisibility(View.GONE);
            findViewById(R.id.switch_camera_btn).setVisibility(View.GONE);
        }
    }

    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();     // Tutorial Step 1
        setupVideoProfile();         // Tutorial Step 2
        setupLocalVideo();           // Tutorial Step 3
        joinChannel();               // Tutorial Step 4
        initSignaling();
    }

    private void initSignaling() {
        mSignaling = AgoraAPIOnlySignal.getInstance(this, getString(R.string.agora_app_id));
        mSignaling.login2(getString(R.string.agora_app_id), String.valueOf(mUserName.hashCode()), "_no_need_token", 0, "", 5, 2);
        mSignaling.callbackSet(signalCallback);
    }

    private SignalCallBack signalCallback = new SignalCallBack() {
        @Override
        public void onMessageChannelReceive(String channelID, String account, int uid, final String msg) {
            super.onMessageChannelReceive(channelID, account, uid, msg);
            Log.e(LOG_TAG, "onMessageChannelReceive message " + msg);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (msg != null) {
                        try {
                            Chat chat = mGson.fromJson(msg, Chat.class);
                            mChatAdapter.getListOfChat().add(chat);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        @Override
        public void onLoginSuccess(int uid, int fd) {
            super.onLoginSuccess(uid, fd);
            Log.e(LOG_TAG, "Signaling onLoginSuccess ");
            mSignaling.channelJoin(mChannelID);
        }

        @Override
        public void onLoginFailed(int p0) {
            super.onLoginFailed(p0);
            Log.e(LOG_TAG, "Signaling onLoginFailed ");
        }
    };


    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA);
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
            case PERMISSION_REQ_ID_CAMERA: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
                    showLongToast("No permission for " + Manifest.permission.CAMERA);
                    finish();
                }
                break;
            }
        }
    }

    public final void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;

    }

    // Tutorial Step 10
    public void onLocalVideoMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.muteLocalVideoStream(iv.isSelected());

        FrameLayout container = findViewById(R.id.video_view_container);
        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
        surfaceView.setZOrderMediaOverlay(!iv.isSelected());
        surfaceView.setVisibility(iv.isSelected() ? View.GONE : View.VISIBLE);
    }

    // Tutorial Step 9
    public void onLocalAudioMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.muteLocalAudioStream(iv.isSelected());
    }

    // Tutorial Step 8
    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    // Tutorial Step 6
    public void onEncCallClicked(View view) {
        finish();
    }

    // Tutorial Step 1
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            mRtcEngine.setClientRole(mClientType);
            String sdkLogPath = Environment.getExternalStorageDirectory().toString() + "/" + getPackageName() + "/";
            File sdkLogDir = new File(sdkLogPath);
            sdkLogDir.mkdirs();
            mRtcEngine.setLogFile(sdkLogPath);
            Log.e(LOG_TAG, "SDK_log_path = " + sdkLogPath);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    // Tutorial Step 2
    private void setupVideoProfile() {
        mRtcEngine.enableVideo();
        VideoEncoderConfiguration config = new VideoEncoderConfiguration(
                VD_640x360, FRAME_RATE_FPS_24,
                STANDARD_BITRATE, ORIENTATION_MODE_ADAPTIVE);
        mRtcEngine.setVideoEncoderConfiguration(config);
    }

    // Tutorial Step 3
    private void setupLocalVideo() {
        if (isBroadcaster()) {
            FrameLayout container = findViewById(R.id.video_view_container);
            SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
            surfaceView.setZOrderMediaOverlay(true);
            container.addView(surfaceView);
            mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
        }
    }

    // Tutorial Step 4
    private void joinChannel() {
        if (isAudience()) mRtcEngine.muteLocalAudioStream(true);
        mRtcEngine.joinChannel(null, mChannelID, "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
    }

    // Tutorial Step 5
    private void setupRemoteVideo(int uid) {
        if (isAudience()) {
            FrameLayout container = findViewById(R.id.video_view_container);

            if (container.getChildCount() >= 1) {
                return;
            }

            SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
            container.addView(surfaceView);
            mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));

            surfaceView.setTag(uid); // for mark purpose
        }
    }

    // Tutorial Step 6
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
        mSignaling.logout();
    }

    // Tutorial Step 7
    private void onRemoteUserLeft() {
        if (isAudience()) {
            FrameLayout container = findViewById(R.id.video_view_container);
            container.removeAllViews();
        }
    }

    // Tutorial Step 10
    private void onRemoteUserVideoMuted(int uid, boolean muted) {

        if (isAudience()) {
            FrameLayout container = findViewById(R.id.video_view_container);

            SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);

            Object tag = surfaceView.getTag();
            if (tag != null && (Integer) tag == uid) {
                surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
            }
        }
    }

    public void onInfoButtonClicked(View view) {
        int visibility = mShowInfo ? View.GONE : View.VISIBLE;
        findViewById(R.id.video_detail_tv).setVisibility(visibility);
        mShowInfo = !mShowInfo;
    }


    public void onMessageSendClicked(View view) {
        EditText chatEt = findViewById(R.id.chat_et);
        String chatMessage = chatEt.getText().toString().trim();
        if (!chatMessage.isEmpty()) {
            String id = UUID.randomUUID().toString();
            String msg = mGson.toJson(new Chat(id, chatMessage,
                    String.valueOf(mUserName.hashCode()), mUserName,
                    mChannelID, new Date().getTime()));
            mSignaling.messageChannelSend(mChannelID, msg, id);

            chatEt.getText().clear();
            chatEt.clearFocus();
            hideKeyboard(chatEt);
        } else {
            showLongToast("Cant send empty message");
        }
    }

    private boolean isBroadcaster() {
        return mClientType == Constants.CLIENT_ROLE_BROADCASTER;
    }

    private boolean isAudience() {
        return mClientType == Constants.CLIENT_ROLE_AUDIENCE;
    }

    private void hideKeyboard(EditText chatEt) {
        InputMethodManager input = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (input != null)
            input.hideSoftInputFromWindow(chatEt.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
