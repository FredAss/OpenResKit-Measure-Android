package htw.bui.openreskit.measure;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.measure.odata.MeasureRepository;

import javax.inject.Inject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import roboguice.activity.RoboFragmentActivity;

public class MeasureInfoActivity extends RoboFragmentActivity {

	@Inject
	private FragmentManager mFragMan;

	@Inject
	private MeasureRepository mRepository;

	private Activity mContext;
	private TextView mMeasureEntryDateTV;
	private EditText mMeasureEvaluationTV;
	private long mMeasureId;
	private long mCatalogId;
	private Measure mMeasure;
	private SimpleDateFormat mFormatter;
	private MeasureInfoFragment mMeasureInfoFragment;

	private RatingBar mMeasureRatingBar;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.measure_info_fragment);
		mFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
		mContext = this;

		Intent launchingIntent = getIntent();
		mMeasureId = launchingIntent.getExtras().getLong("MeasureId");
		mCatalogId = launchingIntent.getExtras().getLong("CatalogId");
		mMeasure = mRepository.getMeasureById(mMeasureId);
		mMeasureInfoFragment = (MeasureInfoFragment) mFragMan.findFragmentById(R.id.measure_info_fragment);
		mMeasureInfoFragment.getMeasureInfo((int)mMeasureId);

		ActionBar bar = getActionBar();
		bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setDisplayShowHomeEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.evaluateMeasure:
			evaluateMeasurePopUp();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void evaluateMeasurePopUp() 
	{

		View v = getLayoutInflater().inflate(R.layout.evaluate_measure, null);
		ImageButton datePicker = (ImageButton) v.findViewById(R.id.measureEntryDatePicker);		
		datePicker.setOnClickListener(mButtonListener);
		mMeasureEntryDateTV = (TextView) v.findViewById(R.id.measureEntryDateTV);
		mMeasureEvaluationTV = (EditText) v.findViewById(R.id.measureEvaluationTV);
		mMeasureRatingBar = (RatingBar) v.findViewById(R.id.measureEvaluationRatingBar);
		if (mMeasure.getEntryDate() == null) 
		{
			mMeasureEntryDateTV.setText(mFormatter.format(new Date()));
			mMeasure.setEntryDate(new Date());
		}
		else
		{
			mMeasureEntryDateTV.setText(mFormatter.format(mMeasure.getEntryDate()));
		}

		new AlertDialog.Builder(mContext)
		.setTitle("Maßnahme bewerten")
		.setMessage("Bitte geben Sie eine Bewertung für die Maßnahme ein")
		.setView(v)
		.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				mMeasure.setEvalutation(mMeasureEvaluationTV.getText().toString());
				mMeasure.setEvaluationRating(mMeasureRatingBar.getRating()/5); 
				mMeasure.setStatus(2);
				mMeasure.setManipulated(true);
				mRepository.setCatalogManipulated(mCatalogId);
				mRepository.persistCatalogs();
				mRepository.fireUpdate();
				invalidateOptionsMenu();
			}
		}).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				// Do nothing.
			}
		}).show();

	}

	private OnClickListener mButtonListener = new OnClickListener() 
	{
		public void onClick(View v) 
		{
			if (v.getId() == R.id.measureEntryDatePicker) 
			{
				showDatePicker();
			}
		}
	};

	OnDateSetListener ondate = new OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) 
		{
			Calendar c = Calendar.getInstance();
			c.set(Calendar.YEAR, year);
			c.set(Calendar.MONTH, monthOfYear);
			c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			mMeasure.setEntryDate(c.getTime());
			mMeasureEntryDateTV.setText(mFormatter.format(c.getTime()));
		}
	};

	private void showDatePicker() 
	{
		DatePickerFragment date = new DatePickerFragment();
		/**
		 * Set Up Current Date Into dialog
		 */
		Calendar calender = Calendar.getInstance();
		if (mMeasure.getEntryDate() != null) 
		{
			calender.setTime(mMeasure.getEntryDate());
		}
		else
		{
			calender.setTime(new Date());
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
		getMenuInflater().inflate(R.menu.measure_info_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{

		MenuItem evaluateMeasureMenuItem = menu.findItem(R.id.evaluateMeasure);

		if (mMeasure.getStatus() < 2) 
		{
			evaluateMeasureMenuItem.setVisible(true);
		} 
		else
		{
			evaluateMeasureMenuItem.setVisible(false);
		}

		if (mRepository.mResponsibleSubjects.size() < 1) 
		{
			evaluateMeasureMenuItem.setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onNavigateUp() 
	{
		onBackPressed();
		return true;	
	}

}
