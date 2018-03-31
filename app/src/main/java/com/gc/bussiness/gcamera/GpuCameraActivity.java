package com.gc.bussiness.gcamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.gc.R;

import butterknife.OnClick;
import jp.co.cyberagent.android.encoder.MediaEncoder;
import jp.co.cyberagent.android.encoder.MediaMuxerWrapper;

import com.gc.bussiness.gcamera.camera.CameraConsts;
import com.gc.bussiness.gcamera.camera.CameraHelper;
import com.gc.bussiness.gcamera.camera.GPUImageFilterTools;
import com.gc.bussiness.gcamera.hardware.listener.CaptureListener;
import com.gc.bussiness.gcamera.hardware.listener.ClickListener;
import com.gc.bussiness.gcamera.hardware.listener.TypeListener;
import com.gc.bussiness.gcamera.hardware.view.CaptureButton;
import com.gc.bussiness.gcamera.hardware.view.CaptureLayout;
import com.gc.bussiness.gcamera.hardware.view.FocusView;
import com.gc.framework.mvp.ui.base.BaseActivity;
import com.gc.framework.mvp.ui.custom.UltimateBar;
import com.gc.framework.mvp.utils.CommonUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageColorBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;

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
    CameraHelper mCameraHelper;

    private GPUImageFilter mFilter = new GPUImageColorBlendFilter();
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;

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
        mGpuImage.setFilter(mFilter);
        mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);

        UltimateBar ultimateBar = new UltimateBar(this);
        ultimateBar.setColorBarForDrawer(Color.BLACK, 0, Color.BLACK, 0);

        mCaptureLayout.setDuration(6 * 1000);
        mCaptureLayout.setButtonFeatures(CaptureButton.BUTTON_STATE_BOTH);
        initListener();

    }

    /**
     * 切换摄像头
     * */
    @OnClick(R.id.image_flash)
    void onFlashLamp() {
        mCameraHelper.switchFlash();
    }
    /**
     * 切换闪光灯
     */
    @OnClick(R.id.image_switch)
    void onSwitchCamera() {
        mCameraHelper.switchCamera(this,mGpuImage);
    }

    private void takePicture() {
        // TODO get a size that is about the size of the screen
        Camera.Parameters params = mCameraHelper.getCameraInstance().getParameters();
        params.setRotation(90);
        mCameraHelper.getCameraInstance().setParameters(params);

        mCameraHelper.getCameraInstance().takePicture(null, null,
                new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, final Camera camera) {

                        final File pictureFile = CommonUtils.getOutputMediaFile(CameraConsts.MEDIA_TYPE_IMAGE);
                        if (pictureFile == null) {
                            Log.d("ASDF",
                                    "Error creating media file, check storage permissions");
                            return;
                        }

                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            Log.d("ASDF", "File not found: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d("ASDF", "Error accessing file: " + e.getMessage());
                        }

                        data = null;
                        Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());

                        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                        mGpuImage.saveToPictures(bitmap, "GPUImage",
                                System.currentTimeMillis() + ".jpg",
                                new GPUImage.OnPictureSavedListener() {

                                    @Override
                                    public void onPictureSaved(final Uri uri) {
                                        pictureFile.delete();
                                        camera.startPreview();
                                        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                                    }
                                });
                    }
                });
    }

    private void switchFilterTo(final GPUImageFilter filter) {
        if (mFilter == null
                || (filter != null && !mFilter.getClass().equals(filter.getClass()))) {
            mFilter = filter;
            mGpuImage.setFilter(mFilter);
            mFilterAdjuster = new GPUImageFilterTools.FilterAdjuster(mFilter);
        }
    }

    private void initListener() {

        //拍照 录像
        mCaptureLayout.setCaptureLisenter(new CaptureListener() {
            @Override
            public void takePictures() {
                if (mCameraHelper.getCameraInstance().getParameters().getFocusMode().equals(
                        Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    takePicture();
                } else {
                    mCameraHelper.getCameraInstance().autoFocus(new Camera.AutoFocusCallback() {

                        @Override
                        public void onAutoFocus(final boolean success, final Camera camera) {
                            takePicture();
                        }
                    });
                }
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
                finish();
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
        mCameraHelper.onResume(this,mGpuImage);
    }

    @Override
    public void onPause() {
        if (DEBUG) {Log.v(TAG, "onPause:");}

        stopRecording();

        super.onPause();
        mCameraHelper.onPause();
    }

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
