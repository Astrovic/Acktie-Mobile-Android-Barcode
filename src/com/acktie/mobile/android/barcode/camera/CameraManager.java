package com.acktie.mobile.android.barcode.camera;

import java.util.List;

import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.kroll.common.Log;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.os.Handler;

public class CameraManager {
	private static final String LCAT = "AcktiemobileandroidbarcodeModule:CameraManager";
	private static final boolean DBG = TiConfig.LOGD;

	private CameraCallback cameraCallback = null;
	private Camera camera = null;
	private boolean isStopped = true;
	private boolean torchOn = false;
	private Handler autoFocusHandler = null;

	public CameraManager(CameraCallback cameraCallback) {
		this();
		this.cameraCallback = cameraCallback;
	}

	public CameraManager() {
		this.camera = getCamera();
	}

	// http://stackoverflow.com/questions/5540981/picture-distorted-with-camera-and-getoptimalpreviewsize
	public Camera.Size getBestPreviewSize(Camera camera, int width, int height) {
		Camera.Size result = null;
		Camera.Parameters parameters = getCameraParameters();

		// If null, likely called after camera has been released.
		if (parameters != null) {
			for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
				if (size.width <= width && size.height <= height) {
					if (result == null) {
						result = size;
					} else {
						int resultArea = result.width * result.height;
						int newArea = size.width * size.height;

						if (newArea > resultArea) {
							result = size;
						}
					}
				}
			}
		}

		return result;
	}

	public void toggleTorch() {
		if (!isStopped) {
			if (torchOn) {
				turnOffTorch();
			} else {
				turnOnTorch();
			}
		}
	}

	public void turnOnTorch() {
		Camera.Parameters parameters = getCameraParameters();

		if (parameters != null) {
			List<String> flashModes = parameters.getSupportedFlashModes();
			if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			}
			camera.setParameters(parameters);
			torchOn = true;
		}
	}

	public void turnOffTorch() {
		Camera.Parameters parameters = getCameraParameters();

		if (parameters != null) {
			List<String> flashModes = parameters.getSupportedFlashModes();
			if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			}
			camera.setParameters(parameters);
			torchOn = false;
		}
	}

	public void enableAutoFocus() {
		camera.autoFocus(autoFocusCB);
	}

	public void stop() {
		if (!isStopped) {
			turnOffTorch();
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			isStopped = true;
		}
	}

	public Camera getCamera() {
		if (isStopped) {
			camera = Camera.open();
			isStopped = false;
		}

		return camera;
	}

	public void takePicture() {
		if (isStopped) {
			return;
		} else if (cameraCallback == null) {
			Log.d(LCAT,
					"Must pass CameraManager a CameraCallback before calling takePicture.");
			return;
		}

		cameraCallback.setPictureTaken(true);
	}

	public boolean isStopped() {
		return isStopped;
	}

	public void setCameraCallback(CameraCallback cameraCallback) {
		this.cameraCallback = cameraCallback;
	}

	public Camera.Parameters getCameraParameters() {
		Camera.Parameters parameters = null;
		try {
			parameters = camera.getParameters();
		} catch (RuntimeException re) {
			// Ignoring exception. It will be thrown if the camera has been
			// released by another thread.
		}

		return parameters;
	}
    
    // Mimic continuous auto-focusing
    private AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
        	if(autoFocusHandler == null)
        	{
        		autoFocusHandler = new Handler();
        	}
        	
            autoFocusHandler.postDelayed(doAutoFocus, 2000);
        }
    };
    
	private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (!isStopped)
            {
            	camera.autoFocus(autoFocusCB);
            }       
        }
    };
}
