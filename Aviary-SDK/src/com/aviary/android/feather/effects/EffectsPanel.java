package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.json.JSONException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.aviary.android.feather.Constants;
import com.aviary.android.feather.R;
import com.aviary.android.feather.headless.filters.INativeFilter;
import com.aviary.android.feather.headless.filters.NativeFilterProxy;
import com.aviary.android.feather.headless.filters.impl.EffectFilter;
import com.aviary.android.feather.headless.gl.GLUtils;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.headless.moa.MoaResult;
import com.aviary.android.feather.library.content.FeatherIntent;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.plugins.PluginFactory.ICDSPlugin;
import com.aviary.android.feather.library.plugins.PluginFactory.InternalPlugin;
import com.aviary.android.feather.library.services.CDSPackage.CDSEntry;
import com.aviary.android.feather.library.services.EffectContext;
import com.aviary.android.feather.library.services.ImageCacheService;
import com.aviary.android.feather.library.services.PluginService;
import com.aviary.android.feather.library.threading.Future;
import com.aviary.android.feather.library.threading.FutureListener;
import com.aviary.android.feather.opengl.AviaryGLSurfaceView;
import com.aviary.android.feather.opengl.AviaryGLSurfaceView.GLRendererListener;
import com.aviary.android.feather.utils.UIUtils;
import com.aviary.android.feather.widget.ImageSwitcher;

public class EffectsPanel extends BordersPanel {

	/** enable/disable opengl for this panel */
	private boolean mOpenGlEnabled;

	/** opengl view */
	private AviaryGLSurfaceView mOpenGlView;
	
	/** enable/disable fast preview. */
	private boolean mEnableFastPreview = false;
	private Bitmap mSmallPreview;
	private static final int PREVIEW_SCALE_FACTOR = 4;

	public EffectsPanel( EffectContext context ) {
		super( context, FeatherIntent.PluginType.TYPE_FILTER );
		checkOpenGL( context );
	}

	@Override
	public void onCreate( Bitmap bitmap ) {
		super.onCreate( bitmap );
		
		mLogger.info( "OpenGL enabled: " + mOpenGlEnabled );
		mLogger.info( "FastPreview enabled: "+  mEnableFastPreview );

		if ( mOpenGlEnabled ) {
			mOpenGlView = (AviaryGLSurfaceView) getContentView().findViewById( R.id.imagegl );
			mOpenGlView.setOnGlRendererListener( mGLRendererListener );
		}
	}

	@Override
	protected void onPostActivate() {
		if ( !mOpenGlEnabled ) {
			super.onPostActivate();
		}
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();

		if ( mOpenGlEnabled ) {
			mOpenGlView.setOnGlRendererListener( null );
			mOpenGlView.setVisibility( View.GONE );
		}
	}
	
	@Override
	protected void onDispose() {
		super.onDispose();

		if ( mSmallPreview != null && !mSmallPreview.isRecycled() ) {
			mLogger.log( "Dispose the small preview" );
			mSmallPreview.recycle();
		}
		mSmallPreview = null;
	}

	@Override
	protected void onGenerateResult() {
		
		mLogger.log( "onGenerateResult" );
		
		if ( !mOpenGlEnabled ) {
			super.onGenerateResult();
		} else {

			// opengl-enabled
			if ( mIsRendering ) {
				FutureListener<Boolean> listener = new FutureListener<Boolean>() {

					@Override
					public void onFutureDone( Future<Boolean> arg0 ) {
						mLogger.log( "write bitmap completed!" );
						getHandler().post( new Runnable() {

							@Override
							public void run() {
								onComplete( mPreview, mActions );
							}
						} );
					}
				};
				mOpenGlView.writeBitmap( mPreview, listener );
			} else {
				super.onGenerateResult();
			}
		}
	}

	@Override
	protected void onProgressEnd() {
		if ( !mOpenGlEnabled && !mEnableFastPreview ) {
			super.onProgressModalEnd();
		} else {
			super.onProgressEnd();
		}
	}

	@Override
	protected void onProgressStart() {
		if ( !mOpenGlEnabled && !mEnableFastPreview ) {
			super.onProgressModalStart();
		} else {
			super.onProgressStart();
		}
	}
	
	@Override
	protected FramesListAdapter createListAdapter( Context context, List<EffectPack> result ) {
		return new EffectsListAdapter( context, R.layout.feather_effect_thumb, R.layout.feather_effect_external_thumb, R.layout.feather_stickers_pack_divider_empty,
				R.layout.feather_getmore_stickers_thumb, R.layout.feather_getmore_stickers_thumb_inverted, result );
	}	
	
	@Override
	protected RenderTask createRenderTask( int position ) {
		return new EffectsRenderTask( position );
	}

	@Override
	public View makeView() {
		View view = super.makeView();
		( (ImageViewTouch) view ).setScrollEnabled( false );
		( (ImageViewTouch) view ).setScaleEnabled( false );
		return view;
	}
	
	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		if( !mOpenGlEnabled ) {
			mEnableFastPreview = Constants.getFastPreviewEnabled();
		}
		
		if( !mOpenGlEnabled )
			return super.generateContentView( inflater );
		else
			return inflater.inflate( R.layout.feather_native_effects_content_gl, null );
	}

	@Override
	public void onClosing() {
		if ( mOpenGlEnabled ) {
			// TODO: unload opengl stuff here...
		}
		super.onClosing();
	}

	@Override
	protected void initContentImage( ImageSwitcher imageView ) {

		if ( !mOpenGlEnabled ) {
			Animation inAnimation = null;
			Animation outAnimation = null;
			if ( mEnableFastPreview ) {
				try {
					mSmallPreview = Bitmap.createBitmap( mBitmap.getWidth() / PREVIEW_SCALE_FACTOR, mBitmap.getHeight() / PREVIEW_SCALE_FACTOR, Config.ARGB_8888 );
					inAnimation = AnimationUtils.loadAnimation( getContext().getBaseContext(), android.R.anim.fade_in );
					outAnimation = AnimationUtils.loadAnimation( getContext().getBaseContext(), android.R.anim.fade_out );
				} catch ( OutOfMemoryError e ) {
					mEnableFastPreview = false;
				}
			}
			
			imageView.setSwitchEnabled( mEnableFastPreview );
			imageView.setFactory( this );
			
			if( mEnableFastPreview ){
				imageView.setImageBitmap( mBitmap, null );
				if( null != inAnimation && null != outAnimation ) {
					imageView.setInAnimation( inAnimation );
					imageView.setOutAnimation( outAnimation );
				}
			} else {
				@SuppressWarnings ( "unused" ) Matrix current = getContext().getCurrentImageViewMatrix();
				imageView.setImageBitmap( mBitmap, null );
			}
			imageView.setAnimateFirstView( false );
		}
	}

	private void checkOpenGL( final EffectContext context ) {
		// check if OpenGL 2.0 is available for this application
		if ( GLUtils.getGlEsEnabled( context.getBaseContext() ) && getPluginType() == FeatherIntent.PluginType.TYPE_FILTER ) {
			mOpenGlEnabled = GLUtils.getGlEsVersion( context.getBaseContext() ) >= 0x20000;
		} else {
			mOpenGlEnabled = false;
		}
	}

	@Override
	protected INativeFilter loadNativeFilter( final EffectPack pack, int position, final CharSequence name, boolean hires ) {
		EffectFilter filter = (EffectFilter) FilterLoaderFactory.get( Filters.EFFECTS );
		
		long effectId = pack.getItemIdAt( position );
		if( !mCDSService.opened() ) mCDSService.open();
		byte[] content = mCDSService.loadEntryContent( effectId );
		if( null != content ) {
			String contentString = new String( content );
			filter.setMoaLiteEffectContent( contentString );
		}
		
		filter.setEffectName( name );
		return filter;
	}
	
	@Override
	protected CharSequence[] getOptionalEffectsLabels() {
		return new CharSequence[] { "Original" };
	}
	
	@Override
	protected CharSequence[] getOptionalEffectsValues() {
		return new CharSequence[] { "-1" };
	}
	
	@Override
	protected List<Long> loadPluginIds( InternalPlugin plugin ) {
		List<Long> result = null;
		if( plugin instanceof ICDSPlugin ) {
			result = new ArrayList<Long>();
			for( int i = 0; i < plugin.size(); i++ ) {
				CDSEntry item = ( (ICDSPlugin) plugin ).getItemAt( i );
				result.add( item.getId() );
			}
		}
		return result;
	}
	
	@Override
	protected List<Pair<String, String>> loadPluginItems( InternalPlugin plugin ) {
		List<Pair<String, String>> result = new ArrayList<Pair<String,String>>();
		
		if( plugin instanceof ICDSPlugin ) {
			for( int i = 0; i < plugin.size(); i++ ) {
				CDSEntry item = ( (ICDSPlugin) plugin ).getItemAt( i );
				result.add( Pair.create( String.valueOf( item.getIdentifier() ), item.getDisplayName() ) );
			}
		}
		return result;		
	}
	
	private void renderFirstTime( String label ) {
		mLogger.log( "renderFirstTime: " + label );
		FutureListener<Boolean> listener = new FutureListener<Boolean>() {

			@Override
			public void onFutureDone( Future<Boolean> arg0 ) {
				mLogger.log( "renderedFirstTime!" );
				mOpenGlView.requestRender();
				contentReady();
			}
		};
		mOpenGlView.executeEffect( label, mPreview, true, listener );
	}

	GLRendererListener mGLRendererListener = new GLRendererListener() {

		boolean mInitializedFirstTime;

		@Override
		public void OnSurfaceCreated() {
			mLogger.info( "onSurfaceCreated" );
		}

		@Override
		public void OnSurfaceChanged( boolean changed, int width, int height ) {
			mLogger.info( "onSurfaceChanged: " + changed, width, height );

			if ( !mInitializedFirstTime ) {
				renderFirstTime( "original" );

				// register for plugins updates
				mPluginService.registerOnUpdateListener( EffectsPanel.this );
				updateInstalledPacks( true );
			} else {
				//setSelectedEffect( mHList.getItemAt( mSelectedPosition ), mSelectedPosition, true );
				mHList.setSelectedPosition( mSelectedPosition, false );
			}
			mInitializedFirstTime = true;
		}
	};
	
	
	protected class EffectsRenderTask extends RenderTask {
		
		private Object mOpenGlCompleted = new Object();
		
		FutureListener<Boolean> mOpenGlBackgroundListener = new FutureListener<Boolean>() {

			@Override
			public void onFutureDone( Future<Boolean> arg0 ) {
				mLogger.info( "mOpenGlBackgroundListener::onFutureDone" );
				synchronized ( mOpenGlCompleted ) {
					mOpenGlCompleted.notify();
				}
			}
		};		
		
		public EffectsRenderTask( int position ) {
			super( position );
		}

		@Override
		protected MoaResult initPreview( INativeFilter filter ) {
			if( !mOpenGlEnabled && mEnableFastPreview ) {
				try {
					mLogger.log( "initPreview: " + mSmallPreview );
					return filter.prepare( mBitmap, mSmallPreview, mSmallPreview.getWidth(), mSmallPreview.getHeight() );
				} catch ( JSONException e ) {
					e.printStackTrace();
				}				
			}
			return null;
		}
		
		@Override
		public void doFullPreviewInBackground( final String effectName ) {
			mLogger.log( "doFullPreviewInBackground: " + effectName );
			if( mOpenGlEnabled ) {
				mOpenGlView.executeEffect( effectName, mPreview, true, mOpenGlBackgroundListener ); // <-- listener here!

				synchronized ( mOpenGlCompleted ) {
					try {
						mOpenGlCompleted.wait();
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
				}
			} else {
				super.doFullPreviewInBackground( effectName );
			}
		}
		
		@Override
		public Bitmap doInBackground( EffectPack... params ) {
			mLogger.info( "doInBackground" );
			Bitmap result = super.doInBackground( params );
			if( mOpenGlEnabled ){
				return mPreview;
			}
			return result;
		}
		
		@Override
		protected void onRestoreOriginalBitmap() {
			mLogger.info( "onRestoreOriginalBitmap" );
			if( mOpenGlEnabled ) {
				mOpenGlView.requestRender();
			} else {
				super.onRestoreOriginalBitmap();
			}
		}
		
		@Override
		protected void onApplyNewBitmap( Bitmap result ) {
			mLogger.info( "onApplyNewBitmap" );
			if( mOpenGlEnabled ) {
				mOpenGlView.requestRender();
			} else {
				super.onApplyNewBitmap( result );
			}
		}
	}
	
	
	class EffectsListAdapter extends FramesListAdapter {

		public EffectsListAdapter( Context context, int mainResId, int externalResId, int dividerResId, int altResId, int altResId2, List<EffectPack> objects ) {
			super( context, mainResId, externalResId, dividerResId, altResId, altResId2, objects );
		}
		
		@Override
		protected Callable<Bitmap> createExternalContentCallable( String iconUrl ) {
			return new ExternalEffectsThumbnailCallable( iconUrl, mCacheService, mExternalFolderIcon, this.getContext().getResources(), R.drawable.feather_iap_dialog_image_na );
		}
		
		@Override
		protected Callable<Bitmap> createContentCallable( EffectPack item, int position, String effectName ) {
			return new FilterThumbnailCallable( item.getItemIdAt( position ), effectName, mThumbBitmap, item.mStatus == PluginService.ERROR_NONE, updateArrowBitmap );
		}
		
		@Override
		protected BitmapDrawable getExternalBackgroundDrawable( Context context ) {
			return (BitmapDrawable) context.getResources().getDrawable( R.drawable.feather_effects_pack_background );
		}
	}
	

	/**
	 * Render the passed effect in a thumbnail
	 * 
	 * @author alessandro
	 * 
	 */
	class FilterThumbnailCallable implements Callable<Bitmap> {

		INativeFilter mFilter;
		String mEffectName;
		Bitmap srcBitmap;
		Bitmap invalidBitmap;
		boolean isValid;
		long mEffectId;

		public FilterThumbnailCallable( long effectId, String effectName, Bitmap bitmap, boolean valid, Bitmap invalid_bitmap ) {
			mEffectName = effectName;
			mEffectId = effectId;
			srcBitmap = bitmap;
			isValid = valid;
			invalidBitmap = invalid_bitmap;
		}
		
		INativeFilter loadFilter( long effectId, CharSequence effectName ) {
			EffectFilter filter = (EffectFilter) FilterLoaderFactory.get( Filters.EFFECTS );
			
			if( null != mCDSService ) {
				if( !mCDSService.opened() ) mCDSService.open();
				byte[] content = mCDSService.loadEntryContent( effectId );
				if( null != content ) {
					String contentString = new String( content );
					filter.setMoaLiteEffectContent( contentString );
				}
			}
			filter.setEffectName( effectName );
			return filter;			
		}

		@Override
		public Bitmap call() throws Exception {

			if ( null == mFilter ) {
				try {
					mFilter = loadFilter( mEffectId, mEffectName );
				} catch( Throwable t ) {
					t.printStackTrace();
					isValid = false;
				}
			}

			MoaActionList actionList = actionsForRoundedThumbnail( isValid, mFilter );
			MoaResult moaresult = NativeFilterProxy.prepareActions( actionList, srcBitmap, null, 1, 1 );
			moaresult.execute();
			Bitmap result = moaresult.outputBitmap;
			
			// Bitmap result = mFilter.execute( srcBitmap, null, 1, 1 );

			if ( !isValid ) {
				addUpdateArrow( result );
			}
			return result;
		}

		void addUpdateArrow( Bitmap bitmap ) {

			if ( null != invalidBitmap && !invalidBitmap.isRecycled() ) {

				final double w = Math.floor( bitmap.getWidth() * 0.75 );
				final double h = Math.floor( bitmap.getHeight() * 0.75 );

				final int paddingx = (int) ( bitmap.getWidth() - w ) / 2;
				final int paddingy = (int) ( bitmap.getHeight() - h ) / 2;

				Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG );
				Rect src = new Rect( 0, 0, invalidBitmap.getWidth(), invalidBitmap.getHeight() );
				Rect dst = new Rect( paddingx, paddingy, paddingx + (int) w, paddingy + (int) h );
				Canvas canvas = new Canvas( bitmap );
				canvas.drawBitmap( invalidBitmap, src, dst, paint );
			}
		}
	}
	
	static class ExternalEffectsThumbnailCallable extends ExternalFramesThumbnailCallable {
		
		public ExternalEffectsThumbnailCallable( String uri, ImageCacheService cacheService, BitmapDrawable folderBackground, Resources resources, int fallbackResId ) {
			super( uri, cacheService, folderBackground, resources, fallbackResId );
		}

		@SuppressWarnings("deprecation")
		@Override
		Bitmap generateBitmap( Bitmap icon ) {
			return UIUtils.drawFolderBitmap( mFolder, new BitmapDrawable( icon ), 1.0f );
		}
	}
}
