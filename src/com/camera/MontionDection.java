package com.camera;

import java.io.File;import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class MontionDection extends Activity
{
	private static final String TAG = "MontionDection";
	
	static int DELAY_TAKEPICTURE = 5000;

	//camera use
	private static SurfaceView preview = null;
	private static
	SurfaceHolder previewHolder = null;
	private static Camera camera = null;
	private static boolean inPreview = false;
	private static long mReferenceTime = 0;
	
	private static RgbMD detector = null;
	private static volatile AtomicBoolean processing = new AtomicBoolean(false);
	public static MontionDection my;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		my = this;
		preview = (SurfaceView)findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		//using rgb
		detector = new RgbMD();
	}

    @Override
    public void onPause() {
        super.onPause();

        camera.setPreviewCallback(null);
        if (inPreview) camera.stopPreview();
        inPreview = false;
        camera.release();
        camera = null;
    }

	@Override
	public void onResume() {
		super.onResume();

		camera = Camera.open();
	}

	private PreviewCallback previewCallback = new PreviewCallback() 
	{
		@Override
		public void onPreviewFrame(byte[] data, Camera cam) {
			if (data == null) return;
			Camera.Size size = cam.getParameters().getPreviewSize();
			if (size == null) return;

		    DetectionThread thread = new DetectionThread(data,size.width,size.height);
		    thread.start();
		}
	};

	private SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				camera.setPreviewDisplay(previewHolder);
				camera.setPreviewCallback(previewCallback);
			} catch (Throwable t) {
				Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
			}
		}

		@Override
		//fetch surfaceChanged event
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
		{
			Camera.Parameters parameters = camera.getParameters();
			Camera.Size size = getBestPreviewSize(width, height, parameters);
			if (size!=null) {
				parameters.setPreviewSize(size.width, size.height);
				Log.d(TAG, "Using width=" + size.width + " height=" + size.height);
			}
			
			camera.setParameters(parameters);
			camera.startPreview();
			inPreview=true;
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// Ignore
		}
	};

	private static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
		Camera.Size result=null;

		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width<=width && size.height<=height) {
				if (result==null) {
					result=size;
				} else {
					int resultArea=result.width*result.height;
					int newArea=size.width*size.height;

					if (newArea>resultArea) result=size;
				}
			}
		}

		return result;
	}

	//thread use
	private static final class DetectionThread extends Thread 
	{
		private byte[] data;
		private int width;
		private int height;
		
		public DetectionThread(byte[] data, int width, int height) {
			this.data = data;
			this.width = width;
			this.height = height;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
	    public void run() 
		{
			if (!processing.compareAndSet(false, true)) return;

			Log.d(TAG, "detect running...");
			try {
	        	//Previous frame
	        	int[] pre = null;
				
				//Current frame (with changes)
				long bConversion = System.currentTimeMillis();
				int[] img = null;
				img = ImageProcessing.decodeYUV420SPtoRGB(data, width, height);
				long aConversion = System.currentTimeMillis();

				Log.d(TAG, "Converstion="+(aConversion-bConversion));
				
				//Current frame (without changes)
				int[] org = null;
				if (img!=null) org = img.clone();
				
				if (img!=null && detector.detect(img, width, height)) {
					// The delay is necessary to avoid taking a picture while in the
					// middle of taking another. This problem can causes some phones
					// to reboot.
					long now = System.currentTimeMillis();
					if (now > (mReferenceTime + DELAY_TAKEPICTURE)) {
						
						mReferenceTime = now;
						
						Bitmap bitmap = null;						
					    //if (org!=null) 
							//original = ImageProcessing.rgbToBitmap(org, width, height);
						bitmap = ImageProcessing.rgbToBitmap(org, width, height);
						
						Log.i(TAG,"Saving.."  + bitmap);
						Looper.prepare();
		
						//save picture
						new SaveTask().execute(bitmap);
					} else {
						Log.i(TAG, "Not taking picture because not enough time has passed since the creation of the Surface");
					}
				}
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            processing.set(false);
	        }
	        
			processing.set(false);
	    }
	};

	private static final class SaveTask extends AsyncTask<Bitmap, Integer, Integer> {
		@Override
		protected Integer doInBackground(Bitmap... data) 
		{
			for (int i=0; i<data.length; i++) {
				Bitmap bitmap = data[i];
				String name = String.valueOf(System.currentTimeMillis());
				if (bitmap!=null) save(name, bitmap);
			}
			return 1;
		}
		
		private void save(String name, Bitmap bitmap) 
		{
		    
		    
			File photo=new File(Environment.getExternalStorageDirectory(), name+".jpg");
			if (photo.exists()) photo.delete();

			try {
				FileOutputStream fos=new FileOutputStream(photo.getPath());
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
				fos.close();
			} catch (java.io.IOException e) {
				Log.e("PictureDemo", "Exception in photoCallback", e);
			}
		}

	}
}