package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.easing.Easing;
import it.sephiroth.android.library.imagezoom.easing.Linear;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.aviary.android.feather.library.graphics.CircleF;
import com.aviary.android.feather.library.graphics.LineF;
import com.aviary.android.feather.library.graphics.Point2D;

public class ImageViewTiltiShiftTouch extends ImageViewTouch {

	// tilt-shift draw modes
	public enum TiltShiftDrawMode {
		Radial, Linear, None
	};

	// tilt-shift draw listener
	public static interface OnTiltShiftDrawListener {

		// draw operation started
		void onDrawStart( float[] center, TiltShiftDrawMode drawMode, float radius, float angle, float left, float top, float right, float bottom );

		// drawing
		void onDrawing( float[] center, float radius, float angle, float left, float top, float right, float bottom );

		// draw operation completed ( usually after an action_up )
		void onDrawEnd();
	};

	private static final int DEFAULT_FADEOUT_TIMEOUT = 1000;

	private static float BRUSH_MULTIPLIER = 2.5f;
	
	/**
	 * After calling {@link #setTiltShiftDrawMode(TiltShiftDrawMode)}
	 * a default shape is created. This is the size of the default shape
	 * ( 0 to 1 of the current bitmap size )  
	 */
	private static final float DEFAULT_SIZE = 0.35f;

	// default foreground color
	int mForeColor = Color.WHITE;

	// default background color
	int mBackColor = Color.BLACK;

	/** The paint used to draw the shapes. */
	protected Paint mPaint;

	/** paint for the crosshair shape */
	protected Paint mCrossPaint;

	/** default paint alpha */
	protected int mPaintAlpha = 255;
	protected int mBackPaintAlpha = 127;

	protected int mPaintAlphaDefault = mPaintAlpha;
	protected int mBackPaintAlphaDefault = mBackPaintAlpha;

	protected int mFadeOutTimeout = DEFAULT_FADEOUT_TIMEOUT;

	protected int mFadeOutDuration = DEFAULT_ANIMATION_DURATION;

	/** The m current scale. */
	protected float mCurrentScale = 1;

	public static final int INVALID_POINTER_ID = -1;

	// used for linear tiltshift mode
	private RectF mDrawingRect;
	private Matrix mDrawingMatrix;
	private Matrix mCenterMatrix;
	protected Matrix mIdentityMatrix = new Matrix();
	protected Matrix mInvertedMatrix = new Matrix();

	// used for tiltshift radial mode
	private CircleF mDrawingCircle;

	// bounding rect of the drawing shape
	private RectF mShapeRect;
	private RectF mShapeRectInverted;

	private PointF mFirstPointOriginal;
	private PointF mSecondPointOriginal;

	private PointF mCenterPoint;
	private float mCurrentDistance;
	private float mCurrentAngle;

	int mActivePointerId;
	int mActivePointerIndex;

	private boolean mInitializedTouch;

	// used to track movement delta
	PointF mOldCenter = new PointF();
	float mOldDistance = 0;
	float mOldAngle = 0;

	/** The m draw listener. */
	private OnTiltShiftDrawListener mDrawListener;

	private TiltShiftDrawMode mTiltShiftDrawMode = TiltShiftDrawMode.None;

	/** maximum size of the shape ( in % ) */
	private int mMaxShapeSize = 75;
	/** minimum size of the shape ( in % ) */
	private int mMinShapeSize = 10;

	private float mMinShapeSizePixels = 40;

	private Path mPath;
	private Path mCrossPath;

	// current bitmap display rectangle
	private RectF mBitmapRect;

	// the value of the smallest side of the mBitmapRect
	private float mBitmapRectSideLength;

	// max length of the tilt shift moving rectangle
	private float mDrawingRectLength;

	private float[] mPoints = new float[8];

	private FadeOutRunnable mFadeOut;

	/**
	 * Instantiates a new image view spot draw.
	 * 
	 * @param context
	 *           the context
	 * @param attrs
	 *           the attrs
	 */
	public ImageViewTiltiShiftTouch( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	/**
	 * Sets the on draw start listener.
	 * 
	 * @param listener
	 *           the new on draw start listener
	 */
	public void setOnDrawStartListener( OnTiltShiftDrawListener listener ) {
		mDrawListener = listener;
	}

	@Override
	protected void init() {
		super.init();

		mPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
		mPaint.setFilterBitmap( false );
		mPaint.setDither( true );
		mPaint.setColor( mForeColor );
		mPaint.setAlpha( mPaintAlphaDefault );
		mPaint.setStyle( Paint.Style.STROKE );
		mPaint.setStrokeWidth( 2 );

		mCrossPaint = new Paint( mPaint );
		mCrossPaint.setStrokeWidth( 2 );
		mCrossPaint.setColor( mForeColor );
		mCrossPaint.setStrokeCap( Cap.SQUARE );

		mPath = new Path();
		mCrossPath = new Path();

		mDrawingRect = new RectF();
		mDrawingMatrix = new Matrix();
		mCenterMatrix = new Matrix();

		mDrawingCircle = new CircleF();

		mCenterPoint = new PointF();
		mShapeRect = new RectF();
		mShapeRectInverted = new RectF();
		mInitializedTouch = false;

		setCrossHairSize( 20, 5 );
	}

	public void setShapeLimits( int minSize, int maxSize ) {
		if ( minSize >= maxSize ) return; // WTF!
		mMinShapeSize = Math.max( minSize, 1 );
		mMaxShapeSize = Math.max( Math.min( maxSize, 100 ), mMinShapeSize + 1 );
		Log.d( LOG_TAG, "limits: " + mMinShapeSize + " - " + mMaxShapeSize );
		updateBitmapRect();
	}

	public void setPaintStrokeWidth( int value, int value2 ) {
		mPaint.setStrokeWidth( value );
		mCrossPaint.setStrokeWidth( value2 );
		postInvalidate();
	}

	public void setPaintStrokeColor( int value, int value2 ) {
		mPaintAlphaDefault = mPaintAlpha = Color.alpha( value );
		mBackPaintAlphaDefault = mBackPaintAlpha = Color.alpha( value2 );
		mForeColor = value;
		mBackColor = value2;
		mPaint.setColor( value );
		mCrossPaint.setColor( value );
		postInvalidate();
	}

	/**
	 * Update the crosshair path
	 * 
	 * @param cross_radius
	 *           Radius of the crosshair path
	 * @param cross_edge
	 *           Extra space for the crosshair lines
	 */
	public void setCrossHairSize( int cross_radius, int cross_edge ) {
		mCrossPath.reset();

		mCrossPath.addCircle( 0, 0, cross_radius, Direction.CW );

		mCrossPath.moveTo( -cross_radius, 0 );
		mCrossPath.lineTo( -cross_radius - cross_edge, 0 );

		mCrossPath.moveTo( cross_radius, 0 );
		mCrossPath.lineTo( cross_radius + cross_edge, 0 );

		mCrossPath.moveTo( 0, -cross_radius );
		mCrossPath.lineTo( 0, -cross_radius - cross_edge );

		mCrossPath.moveTo( 0, cross_radius );
		mCrossPath.lineTo( 0, cross_radius + cross_edge );
	}

	public void setFadeoutTimeout( int value ) {
		mFadeOutTimeout = value;
	}

	public void setFadeoutDuration( int value ) {
		mFadeOutDuration = value;
	}

	public void setTiltShiftDrawMode( TiltShiftDrawMode mode ) {
		Log.i( LOG_TAG, "setTiltShiftDrawMode. drawable: " + getDrawable() );
		mTiltShiftDrawMode = mode;

		if ( null != getDrawable() ) {
			onDrawModeChanged();
		}
	}

	public TiltShiftDrawMode getTiltShiftDrawMode() {
		return mTiltShiftDrawMode;
	}

	private void initializeTouch( float left, float top, float right, float bottom ) {
		mInitializedTouch = true;

		mFirstPointOriginal = new PointF( left, top );
		mSecondPointOriginal = new PointF( right, bottom );

		Point2D.getLerp( mFirstPointOriginal, mSecondPointOriginal, 0.5f, mCenterPoint );
		mCurrentDistance = (float) Math.max( mMinShapeSizePixels, Point2D.distance( mFirstPointOriginal, mSecondPointOriginal ) );
		mCurrentAngle = (float) -Point2D.angleBetweenPoints( mFirstPointOriginal, mSecondPointOriginal ) + 90;
	}

	@SuppressWarnings("unused")
	protected void onDrawModeChanged() {
		Log.w( LOG_TAG, "onDrawModeChanged" );

		// invalidate everything here!
		mInitializedTouch = false;
		mFirstPointOriginal = null;
		mSecondPointOriginal = null;

		// send a touch up.. just in case
		touch_up();

		// update the bitmap rectangle
		updateBitmapRect();

		if ( null != mBitmapRect ) {

			float x = mBitmapRect.centerX();
			float y = mBitmapRect.centerY();
			float w = mBitmapRect.width() * DEFAULT_SIZE;
			float h = mBitmapRect.height() * DEFAULT_SIZE;

			initializeTouch( x, y - h / 2, x, y + h / 2 );

			// send a default touch down
			touch_down();

			// first touch
			touch_move( mCenterPoint, mCurrentDistance, mCurrentAngle, true );

			// touch completed
			touch_up();
		}

		invalidate();
	}

	public RectF getImageRect() {
		if ( getDrawable() != null ) {
			return new RectF( 0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight() );
		} else {
			return null;
		}
	}

	// DEBUG
	LineF firstLine = new LineF( 0, 0, 0, 0 );
	LineF secondLine = new LineF( 0, 0, 0, 0 );

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );

		if ( null == mBitmapRect ) return;

		float strokeWidth = mCrossPaint.getStrokeWidth();

		// Cross-hair paint
		int count = canvas.save( Canvas.MATRIX_SAVE_FLAG );
		canvas.concat( mCenterMatrix );

		mCrossPaint.setStrokeWidth( strokeWidth + 1 );
		mCrossPaint.setColor( mBackColor );
		mCrossPaint.setAlpha( mBackPaintAlpha );
		canvas.drawPath( mCrossPath, mCrossPaint );

		mCrossPaint.setStrokeWidth( strokeWidth );
		mCrossPaint.setColor( mForeColor );
		mCrossPaint.setAlpha( mPaintAlpha );
		canvas.drawPath( mCrossPath, mCrossPaint );
		canvas.restoreToCount( count );

		// SHAPE PAINT
		strokeWidth = mPaint.getStrokeWidth();

		mPaint.setStrokeWidth( strokeWidth + 1 );
		mPaint.setColor( mBackColor );
		mPaint.setAlpha( mBackPaintAlpha );
		canvas.drawPath( mPath, mPaint );

		mPaint.setStrokeWidth( strokeWidth );
		mPaint.setColor( mForeColor );
		mPaint.setAlpha( mPaintAlpha );
		canvas.drawPath( mPath, mPaint );

	}

	private void touch_down() {
		fadeInShape();
	}

	private void touch_move( PointF center, float distance, float angle, boolean first_time ) {

		if ( null == mBitmapRect ) return;

		mPath.reset();

		mCenterMatrix.setTranslate( center.x, center.y );

		float radius = distance / 2;

		if ( mTiltShiftDrawMode == TiltShiftDrawMode.Radial ) {

			mDrawingCircle.set( center.x, center.y, radius );
			mPath.addCircle( mDrawingCircle.centerX(), mDrawingCircle.centerY(), mDrawingCircle.getRadius(), Direction.CW );

			// mDrawingCircle.getBounds( mShapeRect );
			mShapeRect.set( center.x - radius * BRUSH_MULTIPLIER, center.y - radius * BRUSH_MULTIPLIER, center.x + radius * BRUSH_MULTIPLIER, center.y + radius * BRUSH_MULTIPLIER );

		} else if ( mTiltShiftDrawMode == TiltShiftDrawMode.Linear ) {

			mDrawingMatrix.setRotate( angle, center.x, center.y );

			mDrawingRect.set( center.x - radius, center.y - mDrawingRectLength / 2, center.x + radius, center.y + mDrawingRectLength / 2 );
			mDrawingRect.sort();

			mPoints[0] = mDrawingRect.left;
			mPoints[1] = mDrawingRect.top;
			mPoints[2] = mDrawingRect.left;
			mPoints[3] = mDrawingRect.bottom;
			mPoints[4] = mDrawingRect.right;
			mPoints[5] = mDrawingRect.bottom;
			mPoints[6] = mDrawingRect.right;
			mPoints[7] = mDrawingRect.top;

			mDrawingMatrix.mapPoints( mPoints );

			firstLine.reset();
			secondLine.reset();
			firstLine.set( mPoints[0], mPoints[1], mPoints[2], mPoints[3] );
			secondLine.set( mPoints[4], mPoints[5], mPoints[6], mPoints[7] );

			RectF r1 = null;
			RectF r2 = null;

			PointF[] intersection = firstLine.intersect( mBitmapRect );
			if ( null != intersection && intersection.length == 2 ) {
				mPath.moveTo( intersection[0].x, intersection[0].y );
				mPath.lineTo( intersection[1].x, intersection[1].y );
				r1 = new RectF( intersection[0].x, intersection[0].y, intersection[1].x, intersection[1].y );
				r1.sort();
			}

			intersection = secondLine.intersect( mBitmapRect );
			if ( null != intersection && intersection.length == 2 ) {
				mPath.moveTo( intersection[0].x, intersection[0].y );
				mPath.lineTo( intersection[1].x, intersection[1].y );
				r2 = new RectF( intersection[0].x, intersection[0].y, intersection[1].x, intersection[1].y );
				r2.sort();

				if ( null != r1 ) {
					if ( r1.isEmpty() ) {
						r1.set( r1.left, r1.top, r2.right, r2.bottom );
						r1.sort();
					} else {
						r1.union( r2 );
					}
				} else {
					r1 = r2;
				}
			}

			if ( null != r1 ) {
				Point2D.grow( r1, radius * BRUSH_MULTIPLIER, radius * BRUSH_MULTIPLIER );
				mShapeRect.set( r1 );
			} else {
				mShapeRect.set( mBitmapRect );
			}
		}

		// Log.d( LOG_TAG, "shape rect: " + mShapeRect.toString() );

		if ( mDrawListener != null ) {
			float mappedPoints[] = new float[2];
			mappedPoints[0] = center.x;
			mappedPoints[1] = center.y;
			mInvertedMatrix.mapPoints( mappedPoints );
			mInvertedMatrix.mapRect( mShapeRectInverted, mShapeRect );

			if ( first_time ) {
				mDrawListener.onDrawStart( mappedPoints, mTiltShiftDrawMode, ( radius / mCurrentScale ), -angle - 90, mShapeRectInverted.left, mShapeRectInverted.top, mShapeRectInverted.right, mShapeRectInverted.bottom );
			} else {
				mDrawListener.onDrawing( mappedPoints, ( radius / mCurrentScale ), -angle - 90, mShapeRectInverted.left, mShapeRectInverted.top, mShapeRectInverted.right, mShapeRectInverted.bottom );
			}
		}
	}

	private void touch_up() {
		fadeOutShape( mFadeOutTimeout );
		
		if( null != mCenterPoint ) {
			Log.i( LOG_TAG, "center: " + mCenterPoint );
		}

		if ( mDrawListener != null ) {
			Log.d( LOG_TAG, "onDrawEnd" );
			mDrawListener.onDrawEnd();
		}
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {

		final int action = event.getAction();
		final int pointerCount = event.getPointerCount();
		int pointerIndex;
		float x, y;
		float x1, y1;

		switch ( action & MotionEvent.ACTION_MASK ) {

			case MotionEvent.ACTION_DOWN:
				Log.i( LOG_TAG, "action_down" );
				// first pointer dow, register the current pointer index
				// it's called only once per touch session
				touch_down();

				x = event.getX();
				y = event.getY();

				mActivePointerIndex = 0;
				mActivePointerId = event.getPointerId( mActivePointerIndex );

				if ( null == mFirstPointOriginal ) {
					Log.d( LOG_TAG, "original touch" );

					initializeTouch( x - mMinShapeSizePixels / 2, y - mMinShapeSizePixels / 2, x + mMinShapeSizePixels / 2, y + mMinShapeSizePixels / 2 );
					touch_move( mCenterPoint, mCurrentDistance, mCurrentAngle, true );
				} else {
					mFirstPointOriginal = new PointF( x, y );
					touch_move( mCenterPoint, mCurrentDistance, mCurrentAngle, true );
				}

				break;

			case MotionEvent.ACTION_POINTER_DOWN:
				Log.i( LOG_TAG, "action_pointer_down" );

				pointerIndex = ( action & MotionEvent.ACTION_POINTER_INDEX_MASK ) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

				if ( pointerCount > 1 && mInitializedTouch ) {

					x = event.getX( mActivePointerIndex );
					y = event.getY( mActivePointerIndex );

					x1 = event.getX( mActivePointerIndex == 0 ? 1 : 0 );
					y1 = event.getY( mActivePointerIndex == 0 ? 1 : 0 );

					mFirstPointOriginal.set( x, y );
					mSecondPointOriginal.set( x1, y1 );

					mOldDistance = (float) Point2D.distance( mFirstPointOriginal, mSecondPointOriginal );
					mOldAngle = (float) -Point2D.angleBetweenPoints( mFirstPointOriginal, mSecondPointOriginal ) + 180;
					mOldCenter = new PointF();

					Point2D.getLerp( mFirstPointOriginal, mSecondPointOriginal, 0.5f, mOldCenter );
					invalidate();
				}

				break;

			case MotionEvent.ACTION_POINTER_UP:
				Log.i( LOG_TAG, "action_pointer_up" );

				pointerIndex = ( action & MotionEvent.ACTION_POINTER_INDEX_MASK ) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
				int pointerId = event.getPointerId( pointerIndex );

				if ( pointerId == mActivePointerId && mInitializedTouch ) {
					// This was our active pointer going up. Choose a new
					// active pointer and adjust accordingly.
					int newPointerIndex = pointerIndex == 0 ? 1 : 0;
					x = event.getX( newPointerIndex );
					y = event.getY( newPointerIndex );
					mActivePointerId = event.getPointerId( newPointerIndex );
					mActivePointerIndex = newPointerIndex;

					mFirstPointOriginal.set( x, y );
				}
				break;

			case MotionEvent.ACTION_MOVE:
				// Log.i( LOG_TAG, "action_move" );
				pointerIndex = event.findPointerIndex( mActivePointerId );

				x = event.getX( pointerIndex );
				y = event.getY( pointerIndex );

				float dx = x - mFirstPointOriginal.x;
				float dy = y - mFirstPointOriginal.y;
				mFirstPointOriginal.set( x, y );

				if ( pointerCount > 1 && mInitializedTouch ) {
					int newPointerIndex = pointerIndex == 0 ? 1 : 0;
					x1 = event.getX( newPointerIndex );
					y1 = event.getY( newPointerIndex );

					// float dx1 = x1 - mSecondPointOriginal.x;
					// float dy1 = y1 - mSecondPointOriginal.y;

					mSecondPointOriginal.set( x1, y1 );

					// calculate new position, distance, angle
					PointF center = new PointF();
					Point2D.getLerp( mFirstPointOriginal, mSecondPointOriginal, 0.5f, center );

					float dist = (float) Point2D.distance( mFirstPointOriginal, mSecondPointOriginal );
					float angle = (float) -Point2D.angleBetweenPoints( mFirstPointOriginal, mSecondPointOriginal ) + 180;
					float distDiff = dist - mOldDistance;
					float angleDiff = angle - mOldAngle;

					// update the values
					mCurrentDistance = Math.max( mMinShapeSizePixels, Math.abs( mCurrentDistance + distDiff ) );
					mCurrentAngle += angleDiff;
					mCenterPoint.offset( center.x - mOldCenter.x, center.y - mOldCenter.y );

					// limit bounds

					mOldDistance = dist;
					mOldAngle = angle;
					mOldCenter.set( center.x, center.y );

				} else {
					mCenterPoint.offset( dx, dy );
				}

				checkLimits();
				touch_move( mCenterPoint, mCurrentDistance, mCurrentAngle, false );
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				// all pointers left the canvas!!!!
				mActivePointerId = INVALID_POINTER_ID;
				Log.d( LOG_TAG, "action_up" );
				touch_up();
				break;
		}
		invalidate();
		return true;
	}

	private void checkLimits() {

		if ( null == mBitmapRect ) return;

		// too big
		if ( mCurrentDistance > ( mBitmapRectSideLength / 100 ) * mMaxShapeSize ) {
			mCurrentDistance = ( mBitmapRectSideLength / 100 ) * mMaxShapeSize;
		}

		// outside the scope
		if ( !mBitmapRect.contains( mCenterPoint.x, mCenterPoint.y ) ) {
			if ( mCenterPoint.x > mBitmapRect.right )
				mCenterPoint.x = mBitmapRect.right;
			else if ( mCenterPoint.x < mBitmapRect.left ) mCenterPoint.x = mBitmapRect.left;

			if ( mCenterPoint.y > mBitmapRect.bottom )
				mCenterPoint.y = mBitmapRect.bottom;
			else if ( mCenterPoint.y < mBitmapRect.top ) mCenterPoint.y = mBitmapRect.top;
		}

	}

	public static float[] getMatrixValues( Matrix m ) {
		float[] values = new float[9];
		m.getValues( values );
		return values;
	}

	@Override
	protected void onImageMatrixChanged() {
		Log.i( LOG_TAG, "onImageMatrixChanged" );
		super.onImageMatrixChanged();
		updateBitmapRect();
	}

	private void updateBitmapRect() {

		Matrix m1 = new Matrix( getImageMatrix() );
		mInvertedMatrix.reset();

		float[] v1 = getMatrixValues( m1 );
		m1.invert( m1 );
		float[] v2 = getMatrixValues( m1 );

		mInvertedMatrix.postTranslate( -v1[Matrix.MTRANS_X], -v1[Matrix.MTRANS_Y] );
		mInvertedMatrix.postScale( v2[Matrix.MSCALE_X], v2[Matrix.MSCALE_Y] );
		
		mCurrentScale = getScale();
		
		RectF rect = getBitmapRect();
		
		if ( null != rect ) {
			
			boolean update = false;
			
			if( null != mBitmapRect ) {
				double size1 = Point2D.hypotenuse( mBitmapRect );
				double size2 = Point2D.hypotenuse( rect );
				
				float old_left = mBitmapRect.left;
				float old_top = mBitmapRect.top;
				
				float old_width = mBitmapRect.width();
				float old_height = mBitmapRect.height();
				
				float diff_w = rect.width() / old_width;
				float diff_h = rect.height() / old_height;
				
				update = !mBitmapRect.equals( rect );
				
				if( update ) {
					// update the center point and its size
					mCurrentDistance *= size2 / size1;
					mCenterPoint.offset( -old_left, -old_top );
					mCenterPoint.x *= diff_w;
					mCenterPoint.y *= diff_h;
					mCenterPoint.x += rect.left;
					mCenterPoint.y += rect.top;
				}
			}
			
			mBitmapRect = new RectF( rect );
			mBitmapRectSideLength = Math.max( mBitmapRect.width(), mBitmapRect.height() );
			mDrawingRectLength = (float) Math.sqrt( Math.pow( mBitmapRect.width(), 2 ) + Math.pow( mBitmapRect.height(), 2 ) );
			mDrawingRectLength = mBitmapRectSideLength * 1000;
			mMinShapeSizePixels = ( mBitmapRectSideLength / 100 ) * mMinShapeSize;

			if( update ) {
				touch_down();
				touch_move( mCenterPoint, mCurrentDistance, mCurrentAngle, true );
				touch_up();
			}
		} else {
			mBitmapRect = null;
		}
	}

	/**
	 * Fade out the shapes after a delay
	 */
	protected void fadeOutShape( long timeout ) {
		Handler handler = getHandler();
		if ( null != handler ) {
			handler.removeCallbacks( mFadeOut );
			mFadeOut = new FadeOutRunnable();
			handler.postDelayed( mFadeOut, timeout );
		}
	}

	/**
	 * Restore the paint original alpha
	 */
	protected void fadeInShape() {
		Handler handler = getHandler();
		if ( null != handler ) {
			handler.removeCallbacks( mFadeOut );
			mPaintAlpha = mPaintAlphaDefault;
			mBackPaintAlpha = mBackPaintAlphaDefault;
			invalidate();
		}
	}

	/**
	 * Runnable class for fade out the tiltshift storkes
	 * 
	 * @author alessandro
	 * 
	 */
	class FadeOutRunnable implements Runnable {

		private volatile boolean initialized;
		private int startAlpha, startAlpha2;
		private long startTime;
		private Easing mFadeoutEasing = new Linear();

		FadeOutRunnable() {
			initialized = false;
		}

		@Override
		public void run() {

			if ( null != getContext() ) {

				if ( !initialized ) {
					startTime = System.currentTimeMillis();
					startAlpha = mPaintAlpha;
					startAlpha2 = mBackPaintAlpha;
					initialized = true;
				}

				long now = System.currentTimeMillis();
				double currentMs = Math.min( mFadeOutDuration, now - startTime );

				double value1 = mFadeoutEasing.easeOut( currentMs, 0, startAlpha, mFadeOutDuration );
				double value2 = mFadeoutEasing.easeOut( currentMs, 0, startAlpha2, mFadeOutDuration );

				mPaintAlpha = startAlpha - (int) value1;
				mBackPaintAlpha = startAlpha2 - (int) value2;
				invalidate();

				if ( currentMs < mFadeOutDuration ) {
					Handler handler = getHandler();
					if ( null != handler ) handler.post( this );
				} else {
					invalidate();
				}
			}
		}
	}

}
