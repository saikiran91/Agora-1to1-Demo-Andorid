package io.agora.demo1to1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

import static io.agora.rtc.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_320x180;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_320x240;
import static io.agora.rtc.video.VideoEncoderConfiguration.VD_640x360;

public class VideoChatViewActivity extends AppCompatActivity {

    private static final String LOG_TAG = VideoChatViewActivity.class.getSimpleName();
    public static final String CHANNEL_ID_KEY = "CHANNEL_ID_KEY";
    private String mChannelID;


    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private boolean mShowInfo = true;

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
                    TextView detailsTv = findViewById(R.id.remote_detail_tv);
                    detailsTv.setText("");
                    detailsTv.setVisibility(View.GONE);
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
                    if (mShowInfo) {
                        TextView detailsTv = findViewById(R.id.remote_detail_tv);
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
                    if (mShowInfo) {
                        TextView detailsTv = findViewById(R.id.local_detail_tv);
                        detailsTv.setVisibility(View.VISIBLE);
                        String details = "Res: Na" + "\n" +
                                "Bitrate: " + stats.sentBitrate + "\n" +
                                "FrameRate: " + stats.sentFrameRate;
                        detailsTv.setText(details);
                    }
                }
            });
        }

        @Override
        public void onNetworkQuality(int uid, final int txQuality, final int rxQuality) {
            super.onNetworkQuality(uid, txQuality, rxQuality);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (mShowInfo) {
                        TextView detailsTv = findViewById(R.id.network_detail_tv);
                        detailsTv.setVisibility(View.VISIBLE);
                        String details =
                                "Uplink: " + getReadableQuality(txQuality) +
                                        " Downlink: " + getReadableQuality(rxQuality);
                        detailsTv.setText(details);
                    }
                }
            });
        }
    };

    public static String getReadableQuality(int value) {
        switch (value) {
            case 0:
                return "Unknown";
            case 1:
                return "Excellent";
            case 2:
                return "Good";
            case 3:
                return "Poor";
            case 4:
                return "Bad";
            case 5:
                return "Very Bad";
            case 6:
                return "Down";
            default:
                return "Unknown";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat_view);
        mChannelID = getIntent().getStringExtra(CHANNEL_ID_KEY);
        if (mChannelID == null) throw new RuntimeException("Channel ID cannot be null");
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)) {
            initAgoraEngineAndJoinChannel();
        }
    }

    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();     // Tutorial Step 1
        setupVideoProfile();         // Tutorial Step 2
        setupLocalVideo();           // Tutorial Step 3
        joinChannel();               // Tutorial Step 4
    }

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

        FrameLayout container = findViewById(R.id.local_video_view_container);
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
            mRtcEngine.setParameters("{\"extSmoothMode\": true}");
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
        VideoEncoderConfiguration.VideoDimensions dimensions = VD_320x180; // VD_320x240 or VD_640x360
        VideoEncoderConfiguration config = new VideoEncoderConfiguration(dimensions, FRAME_RATE_FPS_15, 600, ORIENTATION_MODE_ADAPTIVE);
        mRtcEngine.setVideoEncoderConfiguration(config);
//        mRtcEngine.setVideoProfile(360,640,15,600);
    }

    // Tutorial Step 3
    private void setupLocalVideo() {
        FrameLayout container = findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    // Tutorial Step 4
    private void joinChannel() {
        mRtcEngine.joinChannel(null, mChannelID, "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
    }

    // Tutorial Step 5
    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);

        if (container.getChildCount() >= 1) {
            return;
        }

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));

        surfaceView.setTag(uid); // for mark purpose
        View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); // optional UI
        tipMsg.setVisibility(View.GONE);
    }

    // Tutorial Step 6
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    // Tutorial Step 7
    private void onRemoteUserLeft() {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        container.removeAllViews();

        View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); // optional UI
        tipMsg.setVisibility(View.VISIBLE);
    }

    // Tutorial Step 10
    private void onRemoteUserVideoMuted(int uid, boolean muted) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);

        SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);

        Object tag = surfaceView.getTag();
        if (tag != null && (Integer) tag == uid) {
            surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
        }
    }

    public void onInfoButtonClicked(View view) {
        int visibility = mShowInfo ? View.GONE : View.VISIBLE;
        findViewById(R.id.network_detail_tv).setVisibility(visibility);
        findViewById(R.id.local_detail_tv).setVisibility(visibility);
        findViewById(R.id.remote_detail_tv).setVisibility(visibility);
        mShowInfo = !mShowInfo;
    }
}
