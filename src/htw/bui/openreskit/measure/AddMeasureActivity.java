package htw.bui.openreskit.measure;

import htw.bui.openreskit.domain.measure.MeasureImageSource;
import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;
import htw.bui.openreskit.measure.adapters.ResponsibleSubjectAdapter;
import htw.bui.openreskit.measure.odata.MeasureRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.inject.Inject;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import android.support.v4.app.FragmentManager;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import roboguice.activity.RoboFragmentActivity;
import roboguice.inject.InjectView;

public class AddMeasureActivity extends RoboFragmentActivity 
{
	@Inject 
	private MeasureRepository mRepository;
	
	@Inject
	private FragmentManager mFragMan;
	
	@InjectView
	(R.id.measureNameTV) EditText mMeasureNameTV;
	
	@InjectView
	(R.id.measureDescriptionTV) EditText mMeasureDescriptionTV;
	
	@InjectView
	(R.id.measureDueDateTV) TextView mMeasureDueDateTV;
	
	@InjectView
	(R.id.measureDueDatePicker) ImageButton mMeasureDueDatePicker;
	
	@InjectView
	(R.id.measureResponsibleSubjectTV) Spinner mMeasureResponsibleSubjectTV;
	
	@InjectView
	(R.id.prioritySlider) SeekBar mPrioritySlider;
	
	@InjectView
	(R.id.measureImage) ImageView mMeasureImage;
	
	private Activity mContext;
	private SimpleDateFormat mFormatter;
	private int mCatalogId;
	private Measure mNewMeasure;
	private Uri mOutputFileUri;
	
	OnDateSetListener ondate = new OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) 
		{
			Calendar c = Calendar.getInstance();
			c.set(Calendar.YEAR, year);
			c.set(Calendar.MONTH, monthOfYear);
			c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			mNewMeasure.setDueDate(c.getTime());
			mNewMeasure.setManipulated(true);
			mMeasureDueDateTV.setText(mFormatter.format(c.getTime()));
		}
	};
	
	private OnClickListener mButtonListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			if (v.getId() == R.id.measureDueDatePicker) 
			{
				showDatePicker();
			}
			if (v.getId() == R.id.measureImage) 
			{
				takePictureForMeasure();
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_measure);
		mContext = this;
		mFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
		Intent launchingIntent = getIntent();
		long catalogId = launchingIntent.getExtras().getLong("CatalogId");
		if (catalogId > 0) 
		{
			mCatalogId = (int)catalogId;
		}
		mNewMeasure = new Measure();
		
		
		mMeasureDueDatePicker.setOnClickListener(mButtonListener);
		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayShowHomeEnabled(true);
		
		initAddMeasureForm();
	}
	
	private void initAddMeasureForm() 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String responsibleSubjectId = prefs.getString("default_responsibleSubject", "none");
		
		List<ResponsibleSubject> responsibleSubjects = mRepository.mResponsibleSubjects;
		ResponsibleSubjectAdapter responsibleSubjectAdapter = new ResponsibleSubjectAdapter(this, R.layout.default_spinner_item, getLayoutInflater(), responsibleSubjects);
		mMeasureResponsibleSubjectTV.setAdapter(responsibleSubjectAdapter);
		//preselect ResponsibleSubject if specified in settings
		if (responsibleSubjectId != "none") 
		{
			int count = 0;
			for (ResponsibleSubject r : responsibleSubjects) 
			{
				if (r.getId() == Long.parseLong(responsibleSubjectId)) 
				{
					mMeasureResponsibleSubjectTV.setSelection(count);
					break;
				}
				count++;
			}
		}
		
		mMeasureImage.setOnClickListener(mButtonListener);
	}

	private void showDatePicker() 
	{
		DatePickerFragment date = new DatePickerFragment();
		/**
		 * Set Up Current Date Into dialog
		 */
		Calendar calender = Calendar.getInstance();
		if (mNewMeasure.getDueDate() != null) 
		{
			calender.setTime(mNewMeasure.getDueDate());
		}
		Bundle args = new Bundle();
		args.putInt("year", calender.get(Calendar.YEAR));
		args.putInt("month", calender.get(Calendar.MONTH));
		args.putInt("day", calender.get(Calendar.DAY_OF_MONTH));
		date.setArguments(args);
		/**
		 * Set Call back to capture selected date
		 */
		date.setCallBack(ondate);
		date.show(mFragMan, "Date Picker");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.add_measure_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.saveAdd:
			saveNewMeasure();
			return true;
		case R.id.cancelAdd:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveNewMeasure() 
	{
		String errorMessage = "Das Formular ist nicht vollständig ausgefüllt!\n";
		boolean errorOccured = false;
		if (mMeasureNameTV.getText().toString().trim().length() < 1) 
		{
			errorMessage += "Bitte geben Sie eine Maßnahmenbezeichnung ein!\n";
			errorOccured = true;
		}
	
		if (mNewMeasure.getDueDate() == null) 
		{
			errorMessage += "Bitte geben Sie eine Erledigungsdatum ein!\n";
			errorOccured = true;
		}

		if (!errorOccured) 
		{
			mNewMeasure.setInternalId((int)mRepository.getMaxMeasureId()+1);
			mNewMeasure.setName(mMeasureNameTV.getText().toString());
			mNewMeasure.setCreationDate(new Date());
			mNewMeasure.setDescription(mMeasureDescriptionTV.getText().toString());
			mNewMeasure.setResponsibleSubject((ResponsibleSubject) mMeasureResponsibleSubjectTV.getSelectedItem());
			//DueDate set in onDateSet Callback of DatePicker
			mNewMeasure.setPriority(mPrioritySlider.getProgress());
			mRepository.addMeasureToCatalog(mNewMeasure, mCatalogId);
			mRepository.persistCatalogs();
			finish();
		}
		else 
		{
			Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
		}
		
		
	}

	private void takePictureForMeasure() 
	{
		if (isIntentAvailable(mContext, MediaStore.ACTION_IMAGE_CAPTURE)) 
		{
			File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp.jpg");
			mOutputFileUri = Uri.fromFile(file);
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mOutputFileUri);
			startActivityForResult(takePictureIntent, 54233);
		}
	}
	
	public static boolean isIntentAvailable(Context context, String action) 
	{
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
				packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{

		if (requestCode == 54233)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				Bitmap bmpBitmap;
				try {
					bmpBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), mOutputFileUri);

					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					Bitmap finalBitmap = Utils.scaleToFill(bmpBitmap, 1134, 756);
					finalBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
					byte[] byteArray = stream.toByteArray();
					
					MeasureImageSource imageSource = new MeasureImageSource();
					imageSource.setInternalId((int)mRepository.getMaxImageId()+1);
					imageSource.setBinarySource(Base64.encodeToString(byteArray, Base64.NO_WRAP));
					
					mNewMeasure.setImageSource(imageSource);
					mMeasureImage.setImageBitmap(finalBitmap);

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}