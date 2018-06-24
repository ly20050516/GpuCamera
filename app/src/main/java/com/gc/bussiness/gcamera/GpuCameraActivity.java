package com.gc.bussiness.gcamera;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.TextureView;
import android.widget.ImageView;

import com.gc.R;
import com.gc.bussiness.gcamera.camera.CameraHelper;
import com.gc.bussiness.gcamera.camera2.AutoFitTextureView;
import com.gc.bussiness.gcamera.camera2.Camera2Helper;
import com.gc.bussiness.gcamera.hardware.listener.CaptureListener;
import com.gc.bussiness.gcamera.hardware.listener.ClickListener;
import com.gc.bussiness.gcamera.hardware.view.CaptureButton;
import com.gc.bussiness.gcamera.hardware.view.CaptureLayout;
import com.gc.bussiness.gcamera.hardware.view.FocusView;
import com.gc.framework.mvp.ui.base.BaseActivity;
import com.gc.framework.mvp.ui.custom.UltimateBar;
import com.gc.framework.mvp.utils.AppLogger;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.co.cyberagent.android.gpuimage.GPUImage;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * @author：ly on 2018/3/21 22:19
 * @mail：liuyan@zhimei.ai
 */

public class GpuCameraActivity extends BaseActivity implements GpuCameraMvpView {

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    /**
     * for camera preview display
     */
    @BindView(R.id.camera_gl_view)
    AutoFitTextureView mGlSurfaceView;
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

        UltimateBar ultimateBar = new UltimateBar(this);
        ultimateBar.setColorBarForDrawer(Color.BLACK, 0, Color.BLACK, 0);

        mCaptureLayout.setDuration(6 * 1000);
        mCaptureLayout.setButtonFeatures(CaptureButton.BUTTON_STATE_ONLY_CAPTURE);

        initCamera2Helper();
        initListener();

    }

    private void initCamera2Helper() {
        Camera2Helper.getInstance().initHelper(getApplicationContext(), new Camera2Helper.Camera2HelperCallback() {
            @Override
            public void setPreviewAspectRatio(int width, int height) {
                mGlSurfaceView.setAspectRatio(width,height);
                AppLogger.d("initCamera2Helper setPreviewAspectRatio width = " + width + ",height = " + height);
            }

            @Override
            public SurfaceTexture getPreviewSurfaceTexture() {
                AppLogger.d("initCamera2Helper getPreviewSurfaceTexture");
                return mGlSurfaceView.getSurfaceTexture();

            }

            @Override
            public void setPreviewTransform(Matrix matrix) {
                mGlSurfaceView.setTransform(matrix);
                AppLogger.d("initCamera2Helper setPreviewTransform",matrix);

            }

            @Override
            public void onConfigureFailed() {

            }

            @Override
            public void onCaptureCompleted(String filePath) {

            }

            @Override
            public void onCameraDeviceError() {

            }
        });

    }

    /**
     * 切换摄像头
     * */
    @OnClick(R.id.image_flash)
    void onFlashLamp() {

    }
    /**
     * 切换闪光灯
     */
    @OnClick(R.id.image_switch)
    void onSwitchCamera() {

    }


    private void initListener() {

        //拍照 录像
        mCaptureLayout.setCaptureLisenter(new CaptureListener() {
            @Override
            public void takePictures() {
                Camera2Helper.getInstance().takePicture();
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
        AppLogger.d( "onResume:");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        Camera2Helper.getInstance().startBackgroundThread();
        if(mGlSurfaceView.isAvailable()) {
            AppLogger.d( "onResume: TextureView is available");
            Camera2Helper.getInstance().openCamera(mGlSurfaceView.getWidth(),mGlSurfaceView.getHeight());
        }else {
            AppLogger.d( "onResume: TextureView is not available");
            mGlSurfaceView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        AppLogger.d( "onPause:");

        Camera2Helper.getInstance().closeCamera();
        Camera2Helper.getInstance().stopBackgroundThread();

    }

    private void requestCameraPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                new ConfirmationDialog().show(getSupportFragmentManager(), "Permission Ask");
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        }
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events of a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Camera2Helper.getInstance().openCamera(width, height);
            AppLogger.d("SurfaceTextureListener onSurfaceTextureAvailable width = " + width + ",height = " + height);

        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            Camera2Helper.getInstance().configureTransform(width, height);
            AppLogger.d("SurfaceTextureListener onSurfaceTextureSizeChanged width = " + width + ",height = " + height);

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            AppLogger.d("SurfaceTextureListener onSurfaceTextureDestroyed");

            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {


        }

    };

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                    .create();
        }
    }
}
