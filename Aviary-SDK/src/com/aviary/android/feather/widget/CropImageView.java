package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.graphics.IBitmapDrawable;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

import com.aviary.android.feather.library.graphics.RectD;
import com.aviary.android.feather.library.utils.UIConfiguration;

public class CropImageView extends ImageViewTouch {

	/**
	 * The listener interface for receiving onHighlightSingleTapUpConfirmed events. The class that is interested in processing a
	 * onHighlightSingleTapUpConfirmed event implements this interface, and the object created with that class is registered with a
	 * component using the component's <code>addOnHighlightSingleTapUpConfirmedListener<code> method. When
	 * the onHighlightSingleTapUpConfirmed event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnHighlightSingleTapUpConfirmedEvent
	 */
	public interface OnHighlightSingleTapUpConfirmedListener {

		/**
		 * On single tap up confirmed.
		 */
		void onSingleTapUpConfirmed();
	}

	/** The Constant GROW. */
	public static final int GROW = 0;

	/** The Constant SHRINK. */
	public static final int SHRINK = 1;

	/** The m motion edge. */
	private int mMotionEdge = HighlightView.GROW_NONE;

	/** The m highlight view. */
	private HighlightView mHighlightView;

	/** The m highlight single tap up listener. */
	private OnHighlightSingleTapUpConfirmedListener mHighlightSingleTapUpListener;

	/** The m motion highlight view. */
	private HighlightView mMotionHighlightView;

	/** The m crop min size. */
	private int mCropMinSize = 10;

	protected Handler mHandler = new Handler();

	/**
	 * Instantiates a new crop image view.
	 * 
	 * @param context
	 *           the context
	 * @param attrs
	 *           the attrs
	 */
	public CropImageView( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	/**
	 * Sets the on highlight single tap up confirmed listener.
	 * 
	 * @param listener
	 *           the new on highlight single tap up confirmed listener
	 */
	public void setOnHighlightSingleTapUpConfirmedListener( OnHighlightSingleTapUpConfirmedListener listener ) {
		mHighlightSingleTapUpListener = listener;
	}

	/**
	 * Sets the min crop size.
	 * 
	 * @param value
	 *           the new min crop size
	 */
	public void setMinCropSize( int value ) {
		mCropMinSize = value;
		if ( mHighlightView != null ) {
			mHighlightView.setMinSize( value );
		}
	}

	@Override
	protected void init() {
		super.init();
		mGestureDetector = null;
		mGestureListener = null;
		mScaleListener = null;

		// mScaleDetector = null;
		// mScaleDetector = new ScaleGestureDetector( getContext(), new CropScaleListener() );
		mGestureDetector = new GestureDetector( getContext(), new CropGestureListener(), null, true );
		mGestureDetector.setIsLongpressEnabled( false );

		// mTouchSlop = 20 * 20;
	}

	@Override
	public void setImageDrawable(Drawable drawable, Matrix initial_matrix, float min_zoom, float max_zoom) {
		mMotionHighlightView = null;
		super.setImageDrawable( drawable, initial_matrix, min_zoom, max_zoom );
	}

	@Override
	protected void onLayoutChanged( int left, int top, int right, int bottom ) {
		super.onLayoutChanged( left, top, right, bottom );
		mHandler.post( onLayoutRunnable );
	}

	Runnable onLayoutRunnable = new Runnable() {

		@Override
		public void run() {
			final Drawable drawable = getDrawable();

			if ( drawable != null && ( (IBitmapDrawable) drawable ).getBitmap() != null ) {
				if ( mHighlightView != null ) {
					if ( mHighlightView.isRunning() ) {
						mHandler.post( this );
					} else {
						Log.d( LOG_TAG, "onLayoutRunnable.. running" );
						mHighlightView.getMatrix().set( getImageMatrix() );
						mHighlightView.invalidate();
					}
				}
			}
		}
	};

	@Override
	protected void postTranslate( float deltaX, float deltaY ) {
		super.postTranslate( deltaX, deltaY );

		if ( mHighlightView != null ) {

			if ( mHighlightView.isRunning() ) {
				return;
			}

			if ( getScale() != 1 ) {
				float[] mvalues = new float[9];
				getImageMatrix().getValues( mvalues );
				final float scale = mvalues[Matrix.MSCALE_X];
				mHighlightView.getCropRectD().offset( -deltaX / scale, -deltaY / scale );
			}

			mHighlightView.getMatrix().set( getImageMatrix() );
			mHighlightView.invalidate();
		}
	}

	private Rect mRect1 = new Rect();
	private Rect mRect2 = new Rect();

	@Override
	protected void postScale( float scale, float centerX, float centerY ) {
		if ( mHighlightView != null ) {

			if ( mHighlightView.isRunning() ) return;

			RectD cropRect = mHighlightView.getCropRectD();
			mHighlightView.getDisplayRect( getImageViewMatrix(), mHighlightView.getCropRectD(), mRect1 );

			super.postScale( scale, centerX, centerY );

			mHighlightView.getDisplayRect( getImageViewMatrix(), mHighlightView.getCropRectD(), mRect2 );

			float[] mvalues = new float[9];
			getImageViewMatrix().getValues( mvalues );
			final float currentScale = mvalues[Matrix.MSCALE_X];

			cropRect.offset( ( mRect1.left - mRect2.left ) / currentScale, ( mRect1.top - mRect2.top ) / currentScale );
			cropRect.right += -( mRect2.width() - mRect1.width() ) / currentScale;
			cropRect.bottom += -( mRect2.height() - mRect1.height() ) / currentScale;

			mHighlightView.getMatrix().set( getImageMatrix() );
			mHighlightView.getCropRectD().set( cropRect );
			mHighlightView.invalidate();
		} else {
			super.postScale( scale, centerX, centerY );
		}
	}

	/**
	 * Ensure visible.
	 * 
	 * @param hv
	 *           the hv
	 */
	private void ensureVisible( HighlightView hv ) {
		Rect r = hv.getDrawRect();
		int panDeltaX1 = Math.max( 0, getLeft() - r.left );
		int panDeltaX2 = Math.min( 0, getRight() - r.right );
		int panDeltaY1 = Math.max( 0, getTop() - r.top );
		int panDeltaY2 = Math.min( 0, getBottom() - r.bottom );
		int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
		int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

		if ( panDeltaX != 0 || panDeltaY != 0 ) {
			panBy( panDeltaX, panDeltaY );
		}
	}

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );
		if ( mHighlightView != null ) mHighlightView.draw( canvas );
	}

	/**
	 * Sets the highlight view.
	 * 
	 * @param hv
	 *           the new highlight view
	 */
	public void setHighlightView( HighlightView hv ) {
		if ( mHighlightView != null ) {
			mHighlightView.dispose();
		}

		mMotionHighlightView = null;
		mHighlightView = hv;
		invalidate();
	}

	/**
	 * Gets the highlight view.
	 * 
	 * @return the highlight view
	 */
	public HighlightView getHighlightView() {
		return mHighlightView;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		super.onTouchEvent( event );
		int action = event.getAction() & MotionEvent.ACTION_MASK;

		switch ( action ) {
			case MotionEvent.ACTION_UP:

				if ( mHighlightView != null ) {
					mHighlightView.setMode( HighlightView.Mode.None );
				}

				mMotionHighlightView = null;
				mMotionEdge = HighlightView.GROW_NONE;
				break;

		}

		return true;
	}


	/**
	 * The listener interface for receiving cropGesture events. The class that is interested in processing a cropGesture event
	 * implements this interface, and the object created with that class is registered with a component using the component's
	 * <code>addCropGestureListener<code> method. When
	 * the cropGesture event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see CropGestureEvent
	 */
	class CropGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDown( MotionEvent e ) {
			mMotionHighlightView = null;
			HighlightView hv = mHighlightView;

			if ( hv != null ) {

				int edge = hv.getHit( e.getX(), e.getY() );
				if ( edge != HighlightView.GROW_NONE ) {
					mMotionEdge = edge;
					mMotionHighlightView = hv;
					mMotionHighlightView.setMode( ( edge == HighlightView.MOVE ) ? HighlightView.Mode.Move : HighlightView.Mode.Grow );
				}
			}
			return super.onDown( e );
		}

		@Override
		public boolean onSingleTapConfirmed( MotionEvent e ) {
			mMotionHighlightView = null;

			return super.onSingleTapConfirmed( e );
		}

		@Override
		public boolean onSingleTapUp( MotionEvent e ) {
			mMotionHighlightView = null;

			if ( mHighlightView != null && mMotionEdge == HighlightView.MOVE ) {

				if ( mHighlightSingleTapUpListener != null ) {
					mHighlightSingleTapUpListener.onSingleTapUpConfirmed();
				}
			}
			return super.onSingleTapUp( e );
		}

		@Override
		public boolean onDoubleTap( MotionEvent e ) {
			if ( mDoubleTapEnabled ) {
				mMotionHighlightView = null;

				float scale = getScale();
				float targetScale = scale;
				targetScale = CropImageView.this.onDoubleTapPost( scale, getMaxScale() );
				targetScale = Math.min( getMaxScale(), Math.max( targetScale, 1 ) );
				zoomTo( targetScale, e.getX(), e.getY(), 200 );
				invalidate();
			}
			return super.onDoubleTap( e );
		}

		@Override
		public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
			if ( e1 == null || e2 == null ) return false;
			if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
			if ( mScaleDetector.isInProgress() ) return false;

			if ( mMotionHighlightView != null && mMotionEdge != HighlightView.GROW_NONE ) {
				mMotionHighlightView.handleMotion( mMotionEdge, -distanceX, -distanceY );
				ensureVisible( mMotionHighlightView );
				return true;
			} else {
				scrollBy( -distanceX, -distanceY );
				invalidate();
				return true;
			}
		}

		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
			if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
			if ( mScaleDetector.isInProgress() ) return false;
			if ( mMotionHighlightView != null ) return false;

			float diffX = e2.getX() - e1.getX();
			float diffY = e2.getY() - e1.getY();

			if ( Math.abs( velocityX ) > 800 || Math.abs( velocityY ) > 800 ) {
				scrollBy( diffX / 2, diffY / 2, 300 );
				invalidate();
			}
			return super.onFling( e1, e2, velocityX, velocityY );
		}
	}

	/**
	 * The listener interface for receiving cropScale events. The class that is interested in processing a cropScale event implements
	 * this interface, and the object created with that class is registered with a component using the component's
	 * <code>addCropScaleListener<code> method. When
	 * the cropScale event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see CropScaleEvent
	 */
	class CropScaleListener extends SimpleOnScaleGestureListener {

		@Override
		public boolean onScaleBegin( ScaleGestureDetector detector ) {
			return super.onScaleBegin( detector );
		}

		@Override
		public void onScaleEnd( ScaleGestureDetector detector ) {
			super.onScaleEnd( detector );
		}

		@Override
		public boolean onScale( ScaleGestureDetector detector ) {
			float span = detector.getCurrentSpan() - detector.getPreviousSpan();
			float targetScale = getScale() * detector.getScaleFactor();
			if ( span != 0 ) {
				targetScale = Math.min( getMaxScale(), Math.max( targetScale, 1 ) );
				zoomTo( targetScale, detector.getFocusX(), detector.getFocusY() );
				mDoubleTapDirection = 1;
				invalidate();
			}
			return true;
		}
	}

	/** The m aspect ratio. */
	protected double mAspectRatio = 0;

	/** The m aspect ratio fixed. */
	private boolean mAspectRatioFixed;

	/**
	 * Set the new image display and crop view. If both aspect
	 * 
	 * @param bitmap
	 *           Bitmap to display
	 * @param aspectRatio
	 *           aspect ratio for the crop view. If 0 is passed, then the crop rectangle can be free transformed by the user,
	 *           otherwise the width/height are fixed according to the aspect ratio passed.
	 */
	public void setImageBitmap( Bitmap bitmap, double aspectRatio, boolean isFixed ) {
		mAspectRatio = aspectRatio;
		mAspectRatioFixed = isFixed;
		setImageBitmap( bitmap, null, ImageViewTouchBase.ZOOM_INVALID, UIConfiguration.IMAGE_VIEW_MAX_ZOOM );
	}

	/**
	 * Sets the aspect ratio.
	 * 
	 * @param value
	 *           the value
	 * @param isFixed
	 *           the is fixed
	 */
	public void setAspectRatio( double value, boolean isFixed ) {

		if ( getDrawable() != null ) {
			mAspectRatio = value;
			mAspectRatioFixed = isFixed;
			updateCropView( false );
		}
	}

	@Override
	protected void onDrawableChanged( Drawable drawable ) {
		super.onDrawableChanged( drawable );
		
		if ( null != getHandler() ) {
			getHandler().post( new Runnable() {

				@Override
				public void run() {
					updateCropView( true );
				}
			} );
		}
	}

	/**
	 * Update crop view.
	 */
	public void updateCropView( boolean bitmapChanged ) {

		if ( bitmapChanged ) {
			setHighlightView( null );
		}

		if ( getDrawable() == null ) {
			setHighlightView( null );
			invalidate();
			return;
		}

		if ( getHighlightView() != null ) {
			updateAspectRatio( mAspectRatio, getHighlightView(), true );
		} else {
			HighlightView hv = new HighlightView( this );
			hv.setMinSize( mCropMinSize );
			updateAspectRatio( mAspectRatio, hv, false );
			setHighlightView( hv );
		}
		invalidate();
	}

	/**
	 * Update aspect ratio.
	 * 
	 * @param aspectRatio
	 *           the aspect ratio
	 * @param hv
	 *           the hv
	 */
	private void updateAspectRatio( double aspectRatio, HighlightView hv, boolean animate ) {
		Log.d( LOG_TAG, "updateAspectRatio: " + aspectRatio );

		float width = getDrawable().getIntrinsicWidth();
		float height = getDrawable().getIntrinsicHeight();
		RectD imageRect = new RectD( 0, 0, (int) width, (int) height );
		Matrix mImageMatrix = getImageMatrix();
		RectD cropRect = computeFinalCropRect( aspectRatio );

		if ( animate ) {
			hv.animateTo( mImageMatrix, imageRect, cropRect, mAspectRatioFixed );
		} else {
			hv.setup( mImageMatrix, imageRect, cropRect, mAspectRatioFixed );
		}
	}

	public void onConfigurationChanged( Configuration config ) {
		// Log.d( LOG_TAG, "onConfigurationChanged" );
		if ( null != getHandler() ) {
			getHandler().postDelayed( new Runnable() {

				@Override
				public void run() {
					setAspectRatio( mAspectRatio, getAspectRatioIsFixed() );
				}
			}, 500 );
		}
		postInvalidate();
	}

	private RectD computeFinalCropRect( double aspectRatio ) {
		Log.i( LOG_TAG, "computeCropRect: " + aspectRatio );
		
		float scale = getScale();
		
		Log.d( LOG_TAG, "scale: " + scale );

		float width = getDrawable().getIntrinsicWidth();
		float height = getDrawable().getIntrinsicHeight();
		
		Log.d( LOG_TAG, "width: " + width + ", height: " + height );
		
		RectF viewRect = new RectF( 0, 0, getWidth(), getHeight() );
		RectF bitmapRect = getBitmapRect();
		
		RectF rect = new RectF( Math.max( viewRect.left, bitmapRect.left ), Math.max( viewRect.top, bitmapRect.top ), Math.min( viewRect.right, bitmapRect.right ), Math.min( viewRect.bottom, bitmapRect.bottom ) );
		
		
		Log.d( LOG_TAG, "view: " + rect );
		
		
		Log.d( LOG_TAG, "bitmap.rect: " + bitmapRect );

		double cropWidth = Math.min( Math.min( width / scale, rect.width() ), Math.min( height / scale, rect.height() ) ) * 0.8f;
		double cropHeight = cropWidth;
		
		Log.d( LOG_TAG, "cropWidth: " + cropWidth + ", cropHeight: " + cropHeight );

		if ( aspectRatio != 0 ) {
			if ( aspectRatio > 1 ) {
				cropHeight = cropHeight / (double) aspectRatio;
			} else {
				cropWidth = cropWidth * (double) aspectRatio;
			}
		}
		
		Log.d( LOG_TAG, "cropWidth: " + cropWidth + ", cropHeight: " + cropHeight );
		
		Matrix mImageMatrix = getImageMatrix();
		
		Matrix tmpMatrix = new Matrix();

		if( !mImageMatrix.invert( tmpMatrix ) ){
			Log.e( LOG_TAG, "cannot invert matrix" );
		}
		
		tmpMatrix.mapRect( viewRect );

		double x = viewRect.centerX() - cropWidth / 2;
		double y = viewRect.centerY() - cropHeight / 2;
		RectD cropRect = new RectD( x, y, (x + cropWidth), (y + cropHeight) );
		
		Log.d( LOG_TAG, "cropRect: " + cropRect );
		
		return cropRect;
	}

	/**
	 * Gets the aspect ratio.
	 * 
	 * @return the aspect ratio
	 */
	public double getAspectRatio() {
		return mAspectRatio;
	}

	public boolean getAspectRatioIsFixed() {
		return mAspectRatioFixed;
	}
}