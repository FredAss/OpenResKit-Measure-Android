package htw.bui.openreskit.measure;
import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.measure.odata.MeasureRepository;
import roboguice.activity.RoboActivity;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.google.inject.Inject;

public class ImageDetailActivity extends RoboActivity 
{

	@Inject
	MeasureRepository mRepository;

	private TouchImageView mMeasureImage;
	private int mMeasureId;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_detail);
		ViewGroup layout = (ViewGroup) findViewById(R.id.imageContainer);
		mMeasureImage = new TouchImageView(this);
		mMeasureImage.setMaxZoom(4f); //change the max level of zoom, default is 3f
		mMeasureImage.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.addView(mMeasureImage);
		
		Intent launchingIntent = getIntent();
		mMeasureId = launchingIntent.getExtras().getInt("MeasureId");
				
		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);

		getImage();

	}
	
	private void getImage() 
	{
		Measure measure = mRepository.getMeasureById(mMeasureId);
		byte[] byteArray = Base64.decode(measure.getImageSource().getBinarySource(),0);
		Bitmap bMap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
		mMeasureImage.setImageBitmap(bMap);	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.image_detail_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.closeInfo:
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
