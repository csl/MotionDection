package com.camera;

import java.io.File;import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MontionDection extends Activity
{
	private static final String TAG = "MontionDection";
	
	static int DELAY_TAKEPICTURE = 3500;

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
	public static int stop=0;

	private TextView trgbthr, tnumthr;
	private SeekBar rgbthrbar, numthrbar;
	private TextView trgbthrres, tnumthrres;
	public int h, w;
	
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		stop=0;
		my = this;
		preview = (SurfaceView)findViewById(R.id.preview);
		previewHolder = preview.getHolder();
		previewHolder.addCallback(surfaceCallback);
		previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		//using rgb
		detector = new RgbMD();
	}
	public boolean onCreateOptionsMenu(Menu menu)
	  {
	    super.onCreateOptionsMenu(menu);
	    
	    menu.add(0 , 0, 0 , "Parameter").setAlphabeticShortcut('S');
	    menu.add(0 , 1, 0 , "Exit").setAlphabeticShortcut('S');
	    return true;
	  }

	  @Override
	  public boolean onOptionsItemSelected(MenuItem item)
	  {
	    switch (item.getItemId())
	      {
	        case 0:
	        	camera.stopPreview();
	            AlertDialog.Builder alert = new AlertDialog.Builder(this);

	            alert.setTitle("change Parameter");
	            alert.setMessage("");
	            ScrollView sv = new ScrollView(this);
	            LinearLayout ll = new LinearLayout(this);
	            ll.setOrientation(LinearLayout.VERTICAL);
	            sv.addView(ll);

	            float value = (float)RgbMD.mPixelThreshold / (float)255;
	            Log.i(TAG, Integer.toString(RgbMD.mPixelThreshold) + "," + Float.valueOf(value));
	            trgbthr = new TextView(this);
	            trgbthr.setText("Pixel Threshold: ");
	            ll.addView(trgbthr);
	            rgbthrbar = new SeekBar(this);
	            rgbthrbar.setMax(100);
	            rgbthrbar.setProgress((int) (value * 100));
	            rgbthrbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

	            	   @Override
	            	   public void onProgressChanged(SeekBar seekBar, int progress,
	            	     boolean fromUser) {
	            	    // TODO Auto-generated method stub
	            		   float value = Float.valueOf(progress)/100;
	            		   trgbthrres.setText(Float.toString(value));
	            	   }

	            	   @Override
	            	   public void onStartTrackingTouch(SeekBar seekBar) {
	            	    // TODO Auto-generated method stub
	            	   }

	            	   @Override
	            	   public void onStopTrackingTouch(SeekBar seekBar) {
	            	    // TODO Auto-generated method stub
	            	   }
	           });
	            	               
	            ll.addView(rgbthrbar);

	            trgbthrres = new TextView(this);
	            trgbthrres.setText(Float.toString(value));
	            ll.addView(trgbthrres);

	            float tvalue = (float) RgbMD.mThreshold / (float) (h*w);

	            tnumthr = new TextView(this);
	            tnumthr.setText("Number Pixels: ");
	            ll.addView(tnumthr);
	            numthrbar = new SeekBar(this);
	            numthrbar.setMax(100);
	            numthrbar.setProgress((int) ( tvalue * 100));
	            numthrbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

	            	   @Override
	            	   public void onProgressChanged(SeekBar seekBar, int progress,
	            	     boolean fromUser) {
	            	    // TODO Auto-generated method stub
	            		   float value = Float.valueOf(progress)/100;
	            		   tnumthrres.setText(Float.toString(value));
	            	   }

	            	   @Override
	            	   public void onStartTrackingTouch(SeekBar seekBar) {
	            	    // TODO Auto-generated method stub
	            	   }

	            	   @Override
	            	   public void onStopTrackingTouch(SeekBar seekBar) {
	            	    // TODO Auto-generated method stub
	            	   }
	           });
	            
	            ll.addView(numthrbar);

	            tnumthrres = new TextView(this);
	            tnumthrres.setText(Float.toString(tvalue));
	            ll.addView(tnumthrres);
	            
	            // Set an EditText view to get user input 
	            alert.setView(sv);
	            
	            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) 
	            {
	            	RgbMD.mPixelThreshold =  (int) (Float.valueOf(trgbthrres.getText().toString()) * 255);    
	            	RgbMD.mThreshold = (int) (h * w * Float.valueOf(tnumthrres.getText().toString()));    
					camera.startPreview();
	            }
	            });
	          
	            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                  // Canceled.
	                }
	              });
	          
	              alert.show();      
		          return true;
	        case 1:
	          //locationManager.removeUpdates(locationListener);
	          android.os.Process.killProcess(android.os.Process.myPid());
	          finish();
	          return true;
	      }
	    
	  return true ;
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
		public void onPreviewFrame(byte[] data, Camera cam) 
		{
			if (data == null) return;
			Camera.Size size = cam.getParameters().getPreviewSize();
			if (size == null) return;
			
			w = size.width;
			h = size.height;
			
			if (stop == 1)
			{
				camera.stopPreview();
				SystemClock.sleep(2300);
				camera.startPreview();
				stop = 0;
			}

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
				//callback
				camera.setPreviewCallback(previewCallback);
				//redraw
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
						stop = 1;
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
