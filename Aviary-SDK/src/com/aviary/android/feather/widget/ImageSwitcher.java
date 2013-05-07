package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ViewSwitcher;

// TODO: Auto-generated Javadoc
/**
 * The Class ImageSwitcher.
 */
public class ImageSwitcher extends ViewSwitcher {

	/** The m switch enabled. */
	protected boolean mSwitchEnabled = true;

	/**
	 * Instantiates a new image switcher.
	 * 
	 * @param context
	 *           the context
	 */
	public ImageSwitcher( Context context ) {
		super( context );
	}

	/**
	 * Instantiates a new image switcher.
	 * 
	 * @param context
	 *           the context
	 * @param attrs
	 *           the attrs
	 */
	public ImageSwitcher( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public void setImageBitmap( Bitmap bitmap, Matrix matrix ) {
		ImageViewTouch image = null;

		if ( mSwitchEnabled )
			image = (ImageViewTouch) this.getNextView();
		else
			image = (ImageViewTouch) this.getChildAt( 0 );

		image.setImageBitmap( bitmap, matrix, ImageViewTouchBase.ZOOM_INVALID, ImageViewTouchBase.ZOOM_INVALID );

		if ( mSwitchEnabled )
			showNext();
		else
			setDisplayedChild( 0 );
	}

	/**
	 * Sets the image drawable.
	 * 
	 * @param drawable
	 *           the drawable
	 * @param reset
	 *           the reset
	 * @param matrix
	 *           the matrix
	 * @param maxZoom
	 *           the max zoom
	 */
	public void setImageDrawable( Drawable drawable, Matrix matrix ) {
		ImageViewTouch image = null;

		if ( mSwitchEnabled )
			image = (ImageViewTouch) this.getNextView();
		else
			image = (ImageViewTouch) this.getChildAt( 0 );

		image.setImageDrawable( drawable, matrix, ImageViewTouchBase.ZOOM_INVALID, ImageViewTouchBase.ZOOM_INVALID );

		if ( mSwitchEnabled )
			showNext();
		else
			setDisplayedChild( 0 );
	}

	/**
	 * Sets the switch enabled.
	 * 
	 * @param enable
	 *           the new switch enabled
	 */
	public void setSwitchEnabled( boolean enable ) {
		mSwitchEnabled = enable;
	}
}
