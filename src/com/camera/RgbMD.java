package com.camera;

import android.graphics.Color;
import android.util.Log;

public class RgbMD
{
	private static final String TAG = "RgbMD";

	//Specific settings
	public static int mPixelThreshold = 25; //Difference in pixel (RGB)
	public static int mThreshold = 6000; //Number of different pixels (RGB)

	private static int[] mPrevious = null;
	private static int mPreviousWidth = 0;
	private static int mPreviousHeight = 0;
	
	private static int Range_rate = 5;

	private static int Width_limit = 0;
	private static int height_limit = 0;

	/**
	 * {@inheritDoc}
	 */
	public int[] getPrevious() {
		return ((mPrevious!=null)?mPrevious.clone():null);
	}

	//picture - pre_picture < mPixelThreshold
	protected static boolean isDifferent(int[] first, int width, int height) 
	{
		if (first==null) throw new NullPointerException();
		
		if (mPrevious==null) return false;
		if (first.length != mPrevious.length) return true;
		if (mPreviousWidth != width || mPreviousHeight != height) return true;

		int totDifferentPixels = 0;
		int size = height * width;
		
		Width_limit = width / Range_rate; 
		height_limit = height / Range_rate;

		Log.i(TAG, "limit: " + Width_limit + ", " + height_limit);

		for (int i = 0, ij=0; i < height; i++) {
			
			if (i < height_limit || i > height - height_limit) continue;
			
			for (int j = 0; j < width; j++, ij++) 
			{
				if (j < Width_limit || j > width - Width_limit) continue;
				
				int rpix = (0xff & ((int)first[ij]));
				int rotherPix = (0xff & ((int)mPrevious[ij]));
				int gpix = (0xff00 & ((int)first[ij]));
				int gotherPix = (0xff00 & ((int)mPrevious[ij]));
				int bpix = (0xff0000 & ((int)first[ij]));
				int botherPix = (0xff0000 & ((int)mPrevious[ij]));
				
				//Catch any pixels that are out of range
				if (rpix < 0) rpix = 0;
				if (rpix > 255) rpix = 255;
				if (rotherPix < 0) rotherPix = 0;
				if (rotherPix > 255) rotherPix = 255;
				//Catch any pixels that are out of range
				if (gpix < 0) gpix = 0;
				if (gpix > 255) gpix = 255;
				if (gotherPix < 0) gotherPix = 0;
				if (gotherPix > 255) gotherPix = 255;
				//Catch any pixels that are out of range
				if (bpix < 0) bpix = 0;
				if (bpix > 255) bpix = 255;
				if (botherPix < 0) botherPix = 0;
				if (botherPix > 255) botherPix = 255;				
				
				
				if (Math.abs(rpix - rotherPix) + Math.abs(gpix - gotherPix) + Math.abs(bpix - botherPix) >= mPixelThreshold) 
				{
					totDifferentPixels++;
					//Paint different pixel red
					//first[ij] = Color.RED;
				}
			}
		}
		
		if (totDifferentPixels <= 0) totDifferentPixels = 1;
		boolean different = totDifferentPixels > mThreshold;
		
		int percent = 100/(size/totDifferentPixels);
		String output = "Number of different pixels: " + totDifferentPixels + "> " + percent + "%";
		if (different) {
			Log.e(TAG, output);
		} else {
			Log.d(TAG, output);
		}

		return different;
	}

	public boolean detect(int[] rgb, int width, int height) {
		if (rgb==null) throw new NullPointerException();
		
		int[] original = rgb.clone();
		
		// Create the "mPrevious" picture, the one that will be used to check the next frame against.
		if(mPrevious == null) {
			mPrevious = original;
			mPreviousWidth = width;
			mPreviousHeight = height;
			Log.i(TAG, "Creating background image");
			return false;
		}

		long bDetection = System.currentTimeMillis();
		boolean motionDetected = isDifferent(rgb, width, height);
		long aDetection = System.currentTimeMillis();
		
		Log.d(TAG, "Detection "+(aDetection-bDetection));
		
		// Replace the current image with the previous.
		mPrevious = original;
		mPreviousWidth = width;
		mPreviousHeight = height;

		return motionDetected;
	}
}
