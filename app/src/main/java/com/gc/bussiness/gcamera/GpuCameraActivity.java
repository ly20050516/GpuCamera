package com.gc.bussiness.gcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gc.R;
import jp.co.cyberagent.android.encoder.MediaAudioEncoder;
import jp.co.cyberagent.android.encoder.MediaEncoder;
import jp.co.cyberagent.android.encoder.MediaMuxerWrapper;
import jp.co.cyberagent.android.encoder.MediaVideoEncoder;
import com.gc.bussiness.gcamera.hardware.listener.CaptureListener;
import com.gc.bussiness.gcamera.hardware.listener.ClickListener;
import com.gc.bussiness.gcamera.hardware.listener.TypeListener;
import com.gc.bussiness.gcamera.hardware.view.CaptureButton;
import com.gc.bussiness.gcamera.hardware.view.CaptureLayout;
import com.gc.bussiness.gcamera.hardware.view.FocusView;
import com.gc.framework.mvp.ui.base.BaseActivity;
import com.gc.framework.mvp.ui.custom.UltimateBar;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageColorBlendFilter;
import jp.co.cyberagent.android.gpuimage.GpuGLView;

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
    @BindView(R.id.camera_gl_view)
    GLSurfaceView mGlSurfaceView;
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
    FocusView mFocusView;

    /**
     * muxer for audio/video recording
     */
    private MediaMuxerWrapper mMuxer;
    @Inject
    GPUImage mGpuImage;
    @Inject
    Camera mCamera;

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
        mGpuImage.setGLSurfaceView(mGlSurfaceView);
        mGpuImage.setFilter(new GPUImageColorBlendFilter());
        mGpuImage.setUpCamera(mCamera);

        UltimateBar ultimateBar = new UltimateBar(this);
        ultimateBar.setColorBarForDrawer(Color.BLACK, 0, Color.BLACK, 0);

        updateScaleModeText();
        mCaptureLayout.setDuration(6 * 1000);
        mCaptureLayout.setButtonFeatures(CaptureButton.BUTTON_STATE_BOTH);
        initListener();


    }

    private void setUpResultFile() {
        File saveDir = getExternalFilesDir("GpuImage");
        if(!saveDir.exists()) {
            saveDir.mkdir();
        }
        File resultFile = new File(saveDir,"gpu-image.jpg");
        mSaveResultPath = resultFile.getAbsolutePath();
    }

    private void initListener() {
        //切换摄像头
        mSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mFlashLamp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        //拍照 录像
        mCaptureLayout.setCaptureLisenter(new CaptureListener() {
            @Override
            public void takePictures() {
                mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        File file = new File(mSaveResultPath);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                        mGpuImage.saveToPictures(bitmap,getExternalFilesDir("GpuImage").getAbsolutePath(),"gpu-image.jpg",null);

                    }
                });
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
        if (DEBUG) {Log.v(TAG, "onResume:");}

    }

    @Override
    public void onPause() {
        if (DEBUG) {Log.v(TAG, "onPause:");}

        stopRecording();

        super.onPause();
    }

    /**
     * method when touch record button
     */
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            int id =  (view.getId());


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


    }

    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */
    private void startRecording() {
        if (DEBUG) {Log.v(TAG, "startRecording:");}
        try {
            /**
             *  if you record audio only, ".m4a" is also OK
             */
            mMuxer = new MediaMuxerWrapper(".mp4");
            mSaveResultPath = mMuxer.getOutputPath();

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
        if (DEBUG) {Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);}
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
            if (DEBUG) {Log.v(TAG, "onPrepared:encoder=" + encoder);}
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) {Log.v(TAG, "onStopped:encoder=" + encoder);}

            synchronized (mEncodeLock) {
                mEncodingFinished = true;
                mEncodeLock.notifyAll();
            }
        }
    };
}
