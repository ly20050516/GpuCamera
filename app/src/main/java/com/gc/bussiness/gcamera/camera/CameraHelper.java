/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gc.bussiness.gcamera.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.Surface;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import jp.co.cyberagent.android.gpuimage.GPUImage;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.GINGERBREAD;

/**
 * @author ly
 */
public class CameraHelper {

    public static final String TAG = "CameraHelper";

    private final CameraHelperImpl mImpl;
    private int mCurrentCameraId = 0;
    private Camera mCameraInstance;

    @Inject
    public CameraHelper(final Context context) {
        if (SDK_INT >= GINGERBREAD) {
            mImpl = new CameraHelperGB();
        } else {
            mImpl = new CameraHelperBase(context);
        }
    }

    public void onResume(final Activity activity, GPUImage gpuImage) {
        setUpCamera(mCurrentCameraId, activity, gpuImage);
    }

    public void onPause() {
        releaseCamera();
    }

    public void switchCamera(final Activity activity, GPUImage gpuImage) {
        releaseCamera();
        mCurrentCameraId = (mCurrentCameraId + 1) % getNumberOfCameras();
        setUpCamera(mCurrentCameraId, activity, gpuImage);
    }

    public void switchFlash() {

        if (mCameraInstance == null || mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return;
        }

        Camera.Parameters cameraParameters = mCameraInstance.getParameters();
        if (cameraParameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        try {
            mCameraInstance.setParameters(cameraParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpCamera(final int id, final Activity activity, GPUImage gpuImage) {
        mCameraInstance = getCameraInstance(id);
        Camera.Parameters parameters = mCameraInstance.getParameters();
        // TODO adjust by getting supportedPreviewSizes and then choosing
        // the best one for screen size (best fill screen)
        final List<String> focusModes = parameters.getSupportedFocusModes();
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        // let's try fastest frame rate. You will get near 60fps, but your device become hot.
        final List<int[]> supportedFpsRange = parameters.getSupportedPreviewFpsRange();
        final int n = supportedFpsRange != null ? supportedFpsRange.size() : 0;
        int[] range;
        for (int i = 0; i < n; i++) {
            range = supportedFpsRange.get(i);
            Log.i(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
        }
        final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
        Log.i(TAG, String.format("fps:%d-%d", max_fps[0], max_fps[1]));
        parameters.setPreviewFpsRange(max_fps[0], max_fps[1]);

        mCameraInstance.setParameters(parameters);

        int orientation = getCameraDisplayOrientation(activity, mCurrentCameraId);
        CameraInfo2 cameraInfo = new CameraInfo2();
        getCameraInfo(mCurrentCameraId, cameraInfo);
        boolean flipHorizontal = cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
        gpuImage.setUpCamera(mCameraInstance, orientation, flipHorizontal, false);
    }


    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(final int id) {
        Camera c = null;
        try {
            c = openCamera(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    public Camera getCameraInstance() {
        return mCameraInstance;
    }

    private void releaseCamera() {
        mCameraInstance.setPreviewCallback(null);
        mCameraInstance.stopPreview();
        mCameraInstance.release();
        mCameraInstance = null;
    }

    public interface CameraHelperImpl {
        int getNumberOfCameras();

        Camera openCamera(int id);

        Camera openDefaultCamera();

        Camera openCameraFacing(int facing);

        boolean hasCamera(int cameraFacingFront);

        void getCameraInfo(int cameraId, CameraInfo2 cameraInfo);
    }

    public int getNumberOfCameras() {
        return mImpl.getNumberOfCameras();
    }

    public Camera openCamera(final int id) {
        return mImpl.openCamera(id);
    }

    public Camera openDefaultCamera() {
        return mImpl.openDefaultCamera();
    }

    public Camera openFrontCamera() {
        return mImpl.openCameraFacing(CameraInfo.CAMERA_FACING_FRONT);
    }

    public Camera openBackCamera() {
        return mImpl.openCameraFacing(CameraInfo.CAMERA_FACING_BACK);
    }

    public boolean hasFrontCamera() {
        return mImpl.hasCamera(CameraInfo.CAMERA_FACING_FRONT);
    }

    public boolean hasBackCamera() {
        return mImpl.hasCamera(CameraInfo.CAMERA_FACING_BACK);
    }

    public void getCameraInfo(final int cameraId, final CameraInfo2 cameraInfo) {
        mImpl.getCameraInfo(cameraId, cameraInfo);
    }

    public void setCameraDisplayOrientation(final Activity activity,
            final int cameraId, final Camera camera) {
        int result = getCameraDisplayOrientation(activity, cameraId);
        camera.setDisplayOrientation(result);
    }

    public int getCameraDisplayOrientation(final Activity activity, final int cameraId) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        CameraInfo2 info = new CameraInfo2();
        getCameraInfo(cameraId, info);
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static class CameraInfo2 {
        public int facing;
        public int orientation;
    }
}
