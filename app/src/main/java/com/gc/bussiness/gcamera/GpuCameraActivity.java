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

    @Inject
    GPUImage mGpuImage;
    @Inject
    CameraHelper mCameraHelper;

    private GPUImageFilter mFilter = new GPUImageFilter();
    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;

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
        mCaptureLayout.setButtonFeatures(CaptureButton.BUTTON_STATE_ONLY_CAPTURE);
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
                        try {
                            Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());

                            mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                            mGpuImage.saveToPictures(bitmap, "GPUCamera",
                                    System.currentTimeMillis() + ".jpg",
                                    new GPUImage.OnPictureSavedListener() {

                                        @Override
                                        public void onPictureSaved(final Uri uri) {
                                            pictureFile.delete();
                                            mCameraHelper.onPause();
                                            mCameraHelper.onResume(GpuCameraActivity.this,mGpuImage);
                                            mCaptureLayout.resetCaptureLayout();
                                        }
                                    });

                        } catch (Exception e) {
                            Log.d("ASDF", "Error decode file: " + e.getMessage());
                        }
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
                mCaptureLayout.setTextWithAnimation("暂时不支持视频");
            }

            @Override
            public void recordShort(final long time) {
                mCaptureLayout.setTextWithAnimation("录制时间过短");
                mSwitchCamera.setVisibility(VISIBLE);
                mFlashLamp.setVisibility(VISIBLE);

            }

            @Override
            public void recordEnd(long time) {
                mCaptureLayout.setTextWithAnimation("暂时不支持视频");
            }

            @Override
            public void recordZoom(float zoom) {
                mCaptureLayout.setTextWithAnimation("暂时不支持视频");
            }

            @Override
            public void recordError() {
                mCaptureLayout.setTextWithAnimation("暂时不支持视频");
            }
        });


        mCaptureLayout.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {
                finish();
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
        super.onPause();
        mCameraHelper.onPause();
    }

}
