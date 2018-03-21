package com.gc.bussiness.gcamera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gc.R;
import com.gc.bussiness.gcamera.hardware.encoder.MediaAudioEncoder;
import com.gc.bussiness.gcamera.hardware.encoder.MediaEncoder;
import com.gc.bussiness.gcamera.hardware.encoder.MediaMuxerWrapper;
import com.gc.bussiness.gcamera.hardware.encoder.MediaVideoEncoder;
import com.gc.bussiness.gcamera.hardware.listener.CaptureListener;
import com.gc.bussiness.gcamera.hardware.listener.ClickListener;
import com.gc.bussiness.gcamera.hardware.listener.TypeListener;
import com.gc.bussiness.gcamera.hardware.view.CameraGLView;
import com.gc.bussiness.gcamera.hardware.view.CaptureButton;
import com.gc.bussiness.gcamera.hardware.view.CaptureLayout;
import com.gc.bussiness.gcamera.hardware.view.FoucsView;
import com.gc.framework.mvp.ui.base.BaseActivity;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * @author：ly on 2018/3/21 22:19
 * @mail：liuyan@zhimei.ai
 */

public class GpuCameraActivity extends BaseActivity implements GpuCameraMvpView {

    private static final boolean DEBUG = false;
    private static final String TAG = "GpuCameraActivity";

    /**
     * for camera preview display
     */
    @BindView(R.id.cameraView)
    CameraGLView mCameraView;
    /**
     * for scale mode display
     */
    @BindView(R.id.scalemode_textview)
    TextView mScaleModeView;
    /**
     * button for start/stop recording
     */
    @BindView(R.id.image_switch)
    ImageView mSwitchCamera;
    @BindView(R.id.image_flash)
    ImageView mFlashLamp;
    @BindView(R.id.capture_layout)
    CaptureLayout mCaptureLayout;
    @BindView(R.id.fouce_view)
    FoucsView mFoucsView;

    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;

    String mSaveResultPath;

    private Object mEncodeLock = new Object();
    private boolean mEncodingFinished = false;

    @Inject
    GpuCameraMvpPresenter<GpuCameraMvpView> mPresenter;

    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context,GpuCameraActivity.class);
        return intent;
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gpu_camera);

        getActivityComponent().inject(this);

        setUnBinder(ButterKnife.bind(this));

        mPresenter.onAttach(this);

        setUp();
    }

    @Override
    protected void setUp() {
        mCameraView.setVideoSize(1920, 1080);
        updateScaleModeText();
        mCaptureLayout.setDuration(6 * 1000);
        mCaptureLayout.setButtonFeatures(CaptureButton.BUTTON_STATE_ONLY_RECORDER);
        initListener();
    }

    private void initListener() {
        //切换摄像头
        mSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.switchCamra();
            }
        });

        mFlashLamp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraView.switchFlash();
            }
        });
        //拍照 录像
        mCaptureLayout.setCaptureLisenter(new CaptureListener() {
            @Override
            public void takePictures() {

            }

            @Override
            public void recordStart() {
                mSwitchCamera.setVisibility(INVISIBLE);
                mFlashLamp.setVisibility(INVISIBLE);
                if(mMuxer == null) {
                    startRecording();
                }
            }

            @Override
            public void recordShort(final long time) {
                mCaptureLayout.setTextWithAnimation("录制时间过短");
                mSwitchCamera.setVisibility(VISIBLE);
                mFlashLamp.setVisibility(VISIBLE);

            }

            @Override
            public void recordEnd(long time) {

                stopRecording();

            }

            @Override
            public void recordZoom(float zoom) {

            }

            @Override
            public void recordError() {

            }
        });
        //确认 取消
        mCaptureLayout.setTypeLisenter(new TypeListener() {
            @Override
            public void cancel() {
                onCancel();
            }

            @Override
            public void confirm() {
                onConfirm();
            }
        });

        mCaptureLayout.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {

            }
        });
        mCaptureLayout.setRightClickListener(new ClickListener() {
            @Override
            public void onClick() {

            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume:");
        mCameraView.onResume();
    }

    @Override
    public void onPause() {
        if (DEBUG) Log.v(TAG, "onPause:");
        stopRecording();
        mCameraView.onPause();
        super.onPause();
    }

    /**
     * method when touch record button
     */
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            int id =  (view.getId());
            if(id == R.id.cameraView) {

                final int scale_mode = (mCameraView.getScaleMode() + 1) % 4;
                mCameraView.setScaleMode(scale_mode);
                updateScaleModeText();
            }

        }
    };

    private void onCancel() {
        if(TextUtils.isEmpty(mSaveResultPath)) {
            return;
        }

        File file = new File(mSaveResultPath);
        file.delete();

        mCaptureLayout.resetCaptureLayout();

    }

    private void onConfirm() {

        if(TextUtils.isEmpty(mSaveResultPath)) {
            return;
        }

        synchronized (mEncodeLock) {
            if(mEncodingFinished) {
                setActivityResult();
            }else {
                try {
                    mEncodeLock.wait(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                setActivityResult();
            }
        }
    }

    private void setActivityResult() {

        Intent resIntent = new Intent();
        resIntent.putExtra("videoFile", mSaveResultPath);


    }

    private void updateScaleModeText() {
        final int scale_mode = mCameraView.getScaleMode();
        mScaleModeView.setText(
                scale_mode == 0 ? "scale to fit"
                        : (scale_mode == 1 ? "keep aspect(viewport)"
                        : (scale_mode == 2 ? "keep aspect(matrix)"
                        : (scale_mode == 3 ? "keep aspect(crop center)" : ""))));
    }

    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */
    private void startRecording() {
        if (DEBUG) Log.v(TAG, "startRecording:");
        try {
            mMuxer = new MediaMuxerWrapper(".mp4");	// if you record audio only, ".m4a" is also OK.
            mSaveResultPath = mMuxer.getOutputPath();
            if (true) {
                // for video capturing
                new MediaVideoEncoder(mMuxer, mMediaEncoderListener, mCameraView.getVideoWidth(), mCameraView.getVideoHeight());
            }
            if (false) {
                // for audio capturing
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
            }
            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
            Log.e(TAG, "startCapture:", e);
        }
    }

    /**
     * request stop recording
     */
    private void stopRecording() {
        if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            // you should not wait here
        }
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder)
                mCameraView.setVideoEncoder((MediaVideoEncoder)encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
            if (encoder instanceof MediaVideoEncoder)
                mCameraView.setVideoEncoder(null);

            synchronized (mEncodeLock) {
                mEncodingFinished = true;
                mEncodeLock.notifyAll();
            }
        }
    };
}
