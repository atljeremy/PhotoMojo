package com.aviary.android.feather.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher.ViewFactory;
import com.aviary.android.feather.R;

// TODO: Auto-generated Javadoc
/**
 * The Class ToolbarView.
 */
public class ToolbarView extends ViewFlipper implements ViewFactory {

	/**
	 * The listener interface for receiving onToolbarClick events. The class that is interested in processing a onToolbarClick event
	 * implements this interface, and the object created with that class is registered with a component using the component's
	 * <code>addOnToolbarClickListener<code> method. When
	 * the onToolbarClick event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnToolbarClickEvent
	 */
	public static interface OnToolbarClickListener {
		void onSaveClick();
		void onApplyClick();
		void onCancelClick();
	};

	public static enum STATE {
		STATE_SAVE,
		STATE_APPLY,
	};

	private Button mApplyButton;

	private Button mSaveButton;

	private TextSwitcher mTitleText;

	@SuppressWarnings("unused")
	private boolean isAnimating;

	private STATE mCurrentState;

	private Animation mOutAnimation;

	private Animation mInAnimation;

	private OnToolbarClickListener mListener;

	private boolean mClickable;

	private class SetDisplayChildRunnable implements Runnable {
		
		private int child;
		
		public SetDisplayChildRunnable( int value ) {
			child = value;
		}
		
		@Override
		public void run() {
			setDisplayedChild( child );
		}
	};

	/**
	 * Instantiates a new toolbar view.
	 * 
	 * @param context
	 *           the context
	 */
	public ToolbarView( Context context ) {
		super( context );
		init( context, null );
	}

	/**
	 * Instantiates a new toolbar view.
	 * 
	 * @param context
	 *           the context
	 * @param attrs
	 *           the attrs
	 */
	public ToolbarView( Context context, AttributeSet attrs ) {
		super( context, attrs );
		init( context, attrs );
	}

	/**
	 * Inits the.
	 * 
	 * @param context
	 *           the context
	 * @param attrs
	 *           the attrs
	 */
	private void init( Context context, AttributeSet attrs ) {
		mCurrentState = STATE.STATE_SAVE;
		setAnimationCacheEnabled( true );
		setAlwaysDrawnWithCacheEnabled( true );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#setClickable(boolean)
	 */
	@Override
	public void setClickable( boolean clickable ) {
		mClickable = clickable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#isClickable()
	 */
	@Override
	public boolean isClickable() {
		return mClickable;
	}

	/**
	 * Gets the in animation time.
	 * 
	 * @return the in animation time
	 */
	public long getInAnimationTime() {
		return mInAnimation.getDuration() + mInAnimation.getStartOffset();
	}

	/**
	 * Gets the out animation time.
	 * 
	 * @return the out animation time
	 */
	public long getOutAnimationTime() {
		return mOutAnimation.getDuration() + mOutAnimation.getStartOffset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onFinishInflate()
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		mApplyButton = (Button) findViewById( R.id.toolbar_content_panel ).findViewById( R.id.button_apply );
		mSaveButton = (Button) findViewById( R.id.toolbar_main_panel ).findViewById( R.id.button_save );
		mTitleText = (TextSwitcher) findViewById( R.id.toolbar_title );
		mTitleText.setFactory( this );

		mInAnimation = AnimationUtils.loadAnimation( getContext(), R.anim.feather_push_up_in );
		mInAnimation.setStartOffset( 100 );
		mOutAnimation = AnimationUtils.loadAnimation( getContext(), R.anim.feather_push_up_out );
		mOutAnimation.setStartOffset( 100 );

		mOutAnimation.setAnimationListener( mInAnimationListener );
		mInAnimation.setAnimationListener( mInAnimationListener );

		setInAnimation( mInAnimation );
		setOutAnimation( mOutAnimation );

		mApplyButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				if ( mListener != null && mCurrentState == STATE.STATE_APPLY && isClickable() ) mListener.onApplyClick();
			}
		} );

		mSaveButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				if ( mListener != null && mCurrentState == STATE.STATE_SAVE && isClickable() ) mListener.onSaveClick();
			}
		} );
	}

	/**
	 * Change the current toolbar state creating an animation between the current and the new view state.
	 * 
	 * @param state
	 *           the state
	 * @param showMiddle
	 *           the show middle
	 */
	public void setState( STATE state, final boolean showMiddle ) {
		if ( state != mCurrentState ) {
			mCurrentState = state;

			post( new Runnable() {

				@Override
				public void run() {
					switch ( mCurrentState ) {
						case STATE_APPLY:
							showApplyState();
							break;

						case STATE_SAVE:
							showSaveState( showMiddle );
							break;
					}
				}
			} );
		}
	}

	/**
	 * Return the current toolbar state.
	 * 
	 * @return the state
	 * @see #STATE
	 */
	public STATE getState() {
		return mCurrentState;
	}

	/**
	 * Set the toolbar click listener.
	 * 
	 * @param listener
	 *           the new on toolbar click listener
	 * @see OnToolbarClickListener
	 */
	public void setOnToolbarClickListener( OnToolbarClickListener listener ) {
		mListener = listener;
	}

	/**
	 * Sets the apply enabled.
	 * 
	 * @param value
	 *           the new apply enabled
	 */
	public void setApplyEnabled( boolean value ) {
		mApplyButton.setEnabled( value );
	}
	
	public void setApplyVisibility( boolean visible ) {
		mApplyButton.setVisibility( visible ? View.VISIBLE : View.GONE );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#setSaveEnabled(boolean)
	 */
	@Override
	public void setSaveEnabled( boolean value ) {
		mSaveButton.setEnabled( value );
	}

	/**
	 * Sets the title.
	 * 
	 * @param value
	 *           the new title
	 */
	public void setTitle( CharSequence value ) {
		mTitleText.setText( value );
	}
	
	public void setTitle( CharSequence value, boolean animate ) {
		if( !animate ){
			Animation inAnimation = mTitleText.getInAnimation();
			Animation outAnimation = mTitleText.getOutAnimation();
			mTitleText.setInAnimation( null );
			mTitleText.setOutAnimation( null );
			mTitleText.setText( value );
			mTitleText.setInAnimation( inAnimation );
			mTitleText.setOutAnimation( outAnimation );
		} else {
			setTitle( value );
		}
	}

	/**
	 * Sets the title.
	 * 
	 * @param resourceId
	 *           the new title
	 */
	public void setTitle( int resourceId ) {
		setTitle( getContext().getString( resourceId ) );
	}
	
	public void setTitle( int resourceId, boolean animate ) {
		setTitle( getContext().getString( resourceId ), animate );
	}	

	/**
	 * Show apply state.
	 */
	private void showApplyState() {
		setDisplayedChild( getChildCount() - 1 );
	}

	/**
	 * Show save state.
	 * 
	 * @param showMiddle
	 *           the show middle
	 */
	private void showSaveState( boolean showMiddle ) {
		if ( showMiddle && getChildCount() == 3 )
			setDisplayedChild( 1 );
		else
			setDisplayedChild( 0 );
	}

	/**
	 * Enable children cache.
	 */
	@SuppressWarnings("unused")
	private void enableChildrenCache() {

		setChildrenDrawnWithCacheEnabled( true );
		setChildrenDrawingCacheEnabled( true );

		for ( int i = 0; i < getChildCount(); i++ ) {
			final View child = getChildAt( i );
			child.setDrawingCacheEnabled( true );
			child.buildDrawingCache( true );
		}
	}

	@SuppressWarnings("unused")
	private void clearChildrenCache() {
		setChildrenDrawnWithCacheEnabled( false );
	}


	AnimationListener mInAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart( Animation animation ) {
			isAnimating = true;
		}

		@Override
		public void onAnimationRepeat( Animation animation ) {}

		@Override
		public void onAnimationEnd( Animation animation ) {
			
			isAnimating = false;
			if ( getDisplayedChild() == 1 && getChildCount() > 2 ) {
				Handler handler = getHandler();
				if( null != handler ) {
					handler.postDelayed( new SetDisplayChildRunnable( 0 ), 300 );
				}
			}
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ViewSwitcher.ViewFactory#makeView()
	 */
	@Override
	public View makeView() {
		View text = LayoutInflater.from( getContext() ).inflate( R.layout.feather_toolbar_title_text, null );
		return text;
	}
}
