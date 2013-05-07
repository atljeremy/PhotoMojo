package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnDrawableChangeListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnLayoutChangeListener;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aviary.android.feather.R;
import com.aviary.android.feather.graphics.RepeatableHorizontalDrawable;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.filters.MemeFilter;
import com.aviary.android.feather.library.graphics.drawable.EditableDrawable;
import com.aviary.android.feather.library.graphics.drawable.MemeTextDrawable;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.EffectContext;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.MatrixUtils;
import com.aviary.android.feather.utils.TypefaceUtils;
import com.aviary.android.feather.widget.DrawableHighlightView;
import com.aviary.android.feather.widget.ImageViewDrawableOverlay;
import com.aviary.android.feather.widget.ImageViewDrawableOverlay.OnDrawableEventListener;

/**
 * The Class MemePanel.
 */
public class MemePanel extends AbstractContentPanel implements OnEditorActionListener, OnClickListener, OnDrawableEventListener,
		OnLayoutChangeListener {

	Button editTopButton, editBottomButton;
	EditText editTopText, editBottomText;
	InputMethodManager mInputManager;
	Canvas mCanvas;
	DrawableHighlightView topHv, bottomHv;
	Typeface mTypeface;
	String fontName;
	Button clearButtonTop, clearButtonBottom;
	
	Handler mInputHandler = new Handler();
	ResultReceiver mInputReceiver = new ResultReceiver( mInputHandler );

	public MemePanel( EffectContext context ) {
		super( context );

		ConfigService config = context.getService( ConfigService.class );
		if ( config != null ) {
			fontName = config.getString( R.string.feather_meme_default_font );
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate( Bitmap bitmap ) {
		super.onCreate( bitmap );

		editTopButton = (Button) getOptionView().findViewById( R.id.button1 );
		editBottomButton = (Button) getOptionView().findViewById( R.id.button2 );

		mImageView = (ImageViewTouch) getContentView().findViewById( R.id.overlay );
		editTopText = (EditText) getContentView().findViewById( R.id.invisible_text_1 );
		editBottomText = (EditText) getContentView().findViewById( R.id.invisible_text_2 );

		clearButtonTop = (Button) getOptionView().findViewById( R.id.clear_button_top );
		clearButtonBottom = (Button) getOptionView().findViewById( R.id.clear_button_bottom );

		mImageView.setDisplayType( DisplayType.FIT_TO_SCREEN );
		mImageView.setDoubleTapEnabled( false );
		mImageView.setScaleEnabled( false );
		mImageView.setScrollEnabled( false );

		createAndConfigurePreview();

		mImageView.setOnDrawableChangedListener( new OnDrawableChangeListener() {

			@Override
			public void onDrawableChanged( Drawable drawable ) {
				
				mLogger.info( "onDrawableChanged" );

				final Matrix mImageMatrix = mImageView.getImageViewMatrix();
				float[] matrixValues = getMatrixValues( mImageMatrix );
				final int height = (int) ( mBitmap.getHeight() * matrixValues[Matrix.MSCALE_Y] );
				View view = getContentView().findViewById( R.id.feather_meme_dumb );
				LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) view.getLayoutParams();
				p.height = height - 30;
				view.setLayoutParams( p );
				view.requestLayout();
			}
		} );

		mImageView.setImageBitmap( mPreview, null, ImageViewTouchBase.ZOOM_INVALID, ImageViewTouchBase.ZOOM_INVALID );

		View content = getOptionView().findViewById( R.id.content );
		content.setBackgroundDrawable( RepeatableHorizontalDrawable.createFromView( content ) );
	}

	@Override
	public void onActivate() {
		super.onActivate();

		createTypeFace();

		onAddTopText();
		onAddBottomText();

		( (ImageViewDrawableOverlay) mImageView ).setOnDrawableEventListener( this );
		( (ImageViewDrawableOverlay) mImageView ).setOnLayoutChangeListener( this );

		mInputManager = (InputMethodManager) getContext().getBaseContext().getSystemService( Context.INPUT_METHOD_SERVICE );
		editTopButton.setOnClickListener( this );
		editBottomButton.setOnClickListener( this );

		editTopText.setVisibility( View.VISIBLE );
		editBottomText.setVisibility( View.VISIBLE );
		editTopText.getBackground().setAlpha( 0 );
		editBottomText.getBackground().setAlpha( 0 );

		clearButtonTop.setOnClickListener( this );
		clearButtonBottom.setOnClickListener( this );

		getContentView().setVisibility( View.VISIBLE );
		contentReady();
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();

		endEditView( topHv );
		endEditView( bottomHv );

		( (ImageViewDrawableOverlay) mImageView ).setOnDrawableEventListener( null );
		( (ImageViewDrawableOverlay) mImageView ).setOnLayoutChangeListener( null );

		editTopButton.setOnClickListener( null );
		editBottomButton.setOnClickListener( null );
		clearButtonTop.setOnClickListener( null );
		clearButtonBottom.setOnClickListener( null );

		if ( mInputManager.isActive( editTopText ) ) mInputManager.hideSoftInputFromWindow( editTopText.getWindowToken(), 0 );
		if ( mInputManager.isActive( editBottomText ) ) mInputManager.hideSoftInputFromWindow( editBottomText.getWindowToken(), 0 );

	}

	@Override
	public void onDestroy() {
		mCanvas = null;
		mInputManager = null;
		( (ImageViewDrawableOverlay) mImageView ).clearOverlays();
		super.onDestroy();
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.feather_meme_content, null );
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.feather_meme_panel, parent, false );
	}

	@Override
	protected void onGenerateResult() {
		MemeFilter filter = (MemeFilter) FilterLoaderFactory.get( Filters.MEME );

		flattenText( topHv, filter );
		flattenText( bottomHv, filter );

		MoaActionList actionList = (MoaActionList) filter.getActions().clone();
		super.onGenerateResult( actionList );
	}

	@Override
	public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {

		mLogger.info( "onEditorAction", v, actionId, event );

		if ( v != null ) {
			if ( actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED ) {
				final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;
				if ( image.getSelectedHighlightView() != null ) {
					DrawableHighlightView d = image.getSelectedHighlightView();
					if ( d.getContent() instanceof EditableDrawable ) {
						endEditView( d );
					}
				}
			}
		}

		return false;
	}

	/**
	 * Flatten text.
	 */
	private void flattenText( final DrawableHighlightView hv, final MemeFilter filter ) {

		if ( hv != null ) {
			hv.setHidden( true );
			final Matrix mImageMatrix = mImageView.getImageViewMatrix();
			float[] matrixValues = getMatrixValues( mImageMatrix );

			// TODO: check this modification
			final int width = (int) ( mBitmap.getWidth() );
			final int height = (int) ( mBitmap.getHeight() );

			final RectF cropRect = hv.getCropRectF();
			final Rect rect = new Rect( (int) cropRect.left, (int) cropRect.top, (int) cropRect.right, (int) cropRect.bottom );
			final MemeTextDrawable editable = (MemeTextDrawable) hv.getContent();

			final int saveCount = mCanvas.save( Canvas.MATRIX_SAVE_FLAG );

			// force end edit and hide the blinking cursor
			editable.endEdit();
			editable.invalidateSelf();

			editable.setContentSize( width, height );
			editable.setBounds( rect.left, rect.top, rect.right, rect.bottom );
			editable.draw( mCanvas );

			if ( topHv == hv ) {

				filter.setTopText( (String) editable.getText(), (double) editable.getTextSize() / mBitmap.getWidth() );
				filter.setTopOffset( ( cropRect.left + (double) editable.getXoff() ) / mBitmap.getWidth(),
						( cropRect.top + (double) editable.getYoff() ) / mBitmap.getHeight() );
			} else {

				filter.setBottomText( (String) editable.getText(), (double) editable.getTextSize() / mBitmap.getWidth() );
				filter.setBottomOffset( ( cropRect.left + (double) editable.getXoff() ) / mBitmap.getWidth(),
						( cropRect.top + (double) editable.getYoff() ) / mBitmap.getHeight() );
			}

			filter.setTextScale( matrixValues[Matrix.MSCALE_X] );

			mCanvas.restoreToCount( saveCount );
			mImageView.invalidate();
		}

		onPreviewChanged( mPreview, false );
	}

	/**
	 * Creates the and configure preview.
	 */
	private void createAndConfigurePreview() {

		if ( ( mPreview != null ) && !mPreview.isRecycled() ) {
			mPreview.recycle();
			mPreview = null;
		}

		mPreview = BitmapUtils.copy( mBitmap, mBitmap.getConfig() );
		mCanvas = new Canvas( mPreview );
	}

	@Override
	public void onClick( View v ) {
		
		if( null == v ) return;
		
		final int id = v.getId();
		
		if( id == editTopButton.getId() ) {
			onTopClick( topHv );
		} else if( id == editBottomButton.getId() ) {
			onTopClick( bottomHv );
		} else if( id == clearButtonTop.getId() ) {
			clearEditView( topHv );
			endEditView( topHv );
		} else if( id == clearButtonBottom.getId() ) {
			clearEditView( bottomHv );
			endEditView( bottomHv );
		}
	}

	/**
	 * In top editable text click
	 */
	public void onTopClick( final DrawableHighlightView view ) {

		mLogger.info( "onTopClick", view );

		if ( view != null ) { 
			if ( view.getContent() instanceof EditableDrawable ) {
				beginEditView( view );
			}
		}
	}

	/**
	 * Extract a value form the matrix
	 */
	public static float[] getMatrixValues( Matrix m ) {
		float[] values = new float[9];
		m.getValues( values );
		return values;
	}

	/**
	 * Creates and places the top editable text
	 */
	private void onAddTopText() {
		final Matrix mImageMatrix = mImageView.getImageViewMatrix();
		final int width = (int) ( mBitmap.getWidth() );
		final int height = (int) ( mBitmap.getHeight() );

		final MemeTextDrawable text = new MemeTextDrawable( "", (float) mBitmap.getHeight() / 7.f, mTypeface );
		text.setTextColor( Color.WHITE );
		text.setTextStrokeColor( Color.BLACK );
		text.setContentSize( width, height );

		topHv = new DrawableHighlightView( mImageView, text );
		topHv.setAlignModeV( DrawableHighlightView.AlignModeV.Top );

		final int cropHeight = text.getIntrinsicHeight();
		final int x = 0;
		final int y = 0;

		final Matrix matrix = new Matrix( mImageMatrix );
		matrix.invert( matrix );

		final float[] pts = new float[] { x, y, x + width, y + cropHeight };

		MatrixUtils.mapPoints( matrix, pts );
		final RectF cropRect = new RectF( pts[0], pts[1], pts[2], pts[3] );
		addEditable( topHv, mImageMatrix, cropRect );
	}

	/**
	 * Create and place the bottom editable text.
	 */
	private void onAddBottomText() {

		final Matrix mImageMatrix = mImageView.getImageViewMatrix();
		final int width = (int) ( mBitmap.getWidth() );
		final int height = (int) ( mBitmap.getHeight() );

		final MemeTextDrawable text = new MemeTextDrawable( "", (float) mBitmap.getHeight() / 7.0f, mTypeface );
		text.setTextColor( Color.WHITE );
		text.setTextStrokeColor( Color.BLACK );
		text.setContentSize( width, height );

		bottomHv = new DrawableHighlightView( mImageView, text );
		bottomHv.setAlignModeV( DrawableHighlightView.AlignModeV.Bottom );

		final int cropHeight = text.getIntrinsicHeight();
		final int x = 0;
		final int y = 0;

		final Matrix matrix = new Matrix( mImageMatrix );
		matrix.invert( matrix );

		final float[] pts = new float[] { x, y + height - cropHeight - ( height / 30 ), x + width, y + height - ( height / 30 ) };

		MatrixUtils.mapPoints( matrix, pts );
		final RectF cropRect = new RectF( pts[0], pts[1], pts[2], pts[3] );

		addEditable( bottomHv, mImageMatrix, cropRect );
	}

	/**
	 * Adds the editable.
	 * 
	 * @param hv
	 *           the hv
	 * @param imageMatrix
	 *           the image matrix
	 * @param cropRect
	 *           the crop rect
	 */
	private void addEditable( DrawableHighlightView hv, Matrix imageMatrix, RectF cropRect ) {
		final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;

		hv.setRotateAndScale( true );
		hv.showAnchors( false );
		hv.drawOutlineFill( false );
		hv.drawOutlineStroke( false );
		hv.setup( getContext().getBaseContext(), imageMatrix, null, cropRect, false );
		hv.getOutlineFillPaint().setXfermode( new PorterDuffXfermode( android.graphics.PorterDuff.Mode.SRC_ATOP ) );
		hv.setMinSize( 10 );
		hv.setOutlineFillColor( new ColorStateList( new int[][]{ {android.R.attr.state_active } }, new int[]{0} ) );
		hv.setOutlineStrokeColor( new ColorStateList( new int[][]{ {android.R.attr.state_active } }, new int[]{0} ) );
		image.addHighlightView( hv );
	}

	abstract class MyTextWatcher implements TextWatcher {

		public DrawableHighlightView view;
	}

	private final MyTextWatcher mEditTextWatcher = new MyTextWatcher() {

		@Override
		public void afterTextChanged( final Editable s ) {}

		@Override
		public void beforeTextChanged( final CharSequence s, final int start, final int count, final int after ) {}

		@Override
		public void onTextChanged( final CharSequence s, final int start, final int before, final int count ) {

			mLogger.info( "onTextChanged", view );

			if ( ( view != null ) && ( view.getContent() instanceof EditableDrawable ) ) {
				final EditableDrawable editable = (EditableDrawable) view.getContent();

				if ( !editable.isEditing() ) return;

				editable.setText( s.toString() );

				if ( topHv.equals( view ) ) {
					editTopButton.setText( s );
					clearButtonTop.setVisibility( s != null && s.length() > 0 ? View.VISIBLE : View.INVISIBLE );
				} else if ( bottomHv.equals( view ) ) {
					editBottomButton.setText( s );
					clearButtonBottom.setVisibility( s != null && s.length() > 0 ? View.VISIBLE : View.INVISIBLE );
				}

				view.forceUpdate();
				setIsChanged( true );
			}
		}
	};

	@Override
	public void onFocusChange( DrawableHighlightView newFocus, DrawableHighlightView oldFocus ) {

		mLogger.info( "onFocusChange", newFocus, oldFocus );

		if ( oldFocus != null ) {
			if ( newFocus == null ) {
				endEditView( oldFocus );
			}
		}
	}

	/**
	 * Terminates an edit view.
	 */
	private void endEditView( DrawableHighlightView hv ) {
		EditableDrawable text = (EditableDrawable) hv.getContent();
		
		if ( text.isEditing() ) {
			text.endEdit();
			endEditText( hv );
		}

		CharSequence value = text.getText();
		if ( topHv.equals( hv ) ) {
			editTopButton.setText( value );
			clearButtonTop.setVisibility( value != null && value.length() > 0 ? View.VISIBLE : View.INVISIBLE );
		} else if ( bottomHv.equals( hv ) ) {
			editBottomButton.setText( value );
			clearButtonBottom.setVisibility( value != null && value.length() > 0 ? View.VISIBLE : View.INVISIBLE );
		}
	}

	/**
	 * Begins an edit view.
	 */
	private void beginEditView( DrawableHighlightView hv ) {
		mLogger.info( "beginEditView" );
		
		final EditableDrawable text = (EditableDrawable) hv.getContent();

		if ( hv == topHv ) {
			endEditView( bottomHv );
		} else {
			endEditView( topHv );
		}
		
		if ( !text.isEditing() ) {
			text.beginEdit();
		}
		
		beginEditText( hv );
	}

	private void clearEditView( DrawableHighlightView hv ) {
		final MemeTextDrawable text = (MemeTextDrawable) hv.getContent();
		text.setText( "" );
		text.invalidateSelf();
		hv.forceUpdate();
	}

	@Override
	public void onDown( DrawableHighlightView view ) {

	}

	@Override
	public void onMove( DrawableHighlightView view ) {}

	@Override
	public void onClick( DrawableHighlightView view ) {
		if ( view != null ) {
			if ( view.getContent() instanceof EditableDrawable ) {
				beginEditView( view );
			}
		}

	}

	/**
	 * Begin edit text.
	 */
	private void beginEditText( final DrawableHighlightView view ) {
		mLogger.info( "beginEditText", view );

		EditText editText = null;

		if ( view == topHv ) {
			editText = editTopText;
		} else if ( view == bottomHv ) {
			editText = editBottomText;
		}

		if ( editText != null ) {
			mEditTextWatcher.view = null;
			editText.removeTextChangedListener( mEditTextWatcher );

			final EditableDrawable editable = (EditableDrawable) view.getContent();
			final String oldText = (String) editable.getText();
			editText.setText( oldText );
			editText.setSelection( editText.length() );
			editText.setImeOptions( EditorInfo.IME_ACTION_DONE );
			editText.requestFocusFromTouch();
			
			if ( !mInputManager.showSoftInput( editText, 0, mInputReceiver ) ) {
				mInputManager.toggleSoftInput( InputMethodManager.SHOW_FORCED, 0 ); // TODO: verify
			}

			mEditTextWatcher.view = view;
			editText.setOnEditorActionListener( this );
			editText.addTextChangedListener( mEditTextWatcher );

			( (ImageViewDrawableOverlay) mImageView ).setSelectedHighlightView( view );
			( (EditableDrawable) view.getContent() ).setText( ( (EditableDrawable) view.getContent() ).getText() );
			view.forceUpdate();
		}
	}

	/**
	 * End edit text.
	 */
	private void endEditText( final DrawableHighlightView view ) {

		mLogger.info( "endEditText", view );

		mEditTextWatcher.view = null;
		EditText editText = null;

		if ( view == topHv )
			editText = editTopText;
		else if ( view == bottomHv ) editText = editBottomText;

		if ( editText != null ) {
			editText.removeTextChangedListener( mEditTextWatcher );
			editText.setOnEditorActionListener( null );
			
			if ( mInputManager.isActive( editText ) ) {
				mInputManager.hideSoftInputFromWindow( editText.getWindowToken(), 0 );
			}
			editText.clearFocus();
		}
	}

	/**
	 * Creates the type face used for meme.
	 */
	private void createTypeFace() {
		try {
			mTypeface = TypefaceUtils.createFromAsset( getContext().getBaseContext().getAssets(), fontName );
		} catch ( Exception e ) {
			mTypeface = Typeface.DEFAULT;
		}
	}

	@Override
	public void onLayoutChanged( boolean changed, int left, int top, int right, int bottom ) {
		if ( changed ) {
			final Matrix mImageMatrix = mImageView.getImageViewMatrix();
			float[] matrixValues = getMatrixValues( mImageMatrix );
			final float w = mBitmap.getWidth();
			final float h = mBitmap.getHeight();
			final float scale = matrixValues[Matrix.MSCALE_X];

			if ( topHv != null ) {
				MemeTextDrawable text = (MemeTextDrawable) topHv.getContent();
				text.setContentSize( w * scale, h * scale );
			}

			if ( bottomHv != null ) {
				MemeTextDrawable text = (MemeTextDrawable) bottomHv.getContent();
				text.setContentSize( w * scale, h * scale );
			}
		}
	}
}
