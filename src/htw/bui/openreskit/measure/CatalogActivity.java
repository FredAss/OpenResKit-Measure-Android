package htw.bui.openreskit.measure;

import htw.bui.openreskit.domain.measure.Catalog;
import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.measure.CatalogListFragment.ICatalogHandling;
import htw.bui.openreskit.measure.MeasureListFragment.IMeasureHandling;
import htw.bui.openreskit.measure.odata.MeasureRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import roboguice.activity.RoboFragmentActivity;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.commonsware.android.anim.threepane.ThreePaneLayout;
import com.google.inject.Inject;

public class CatalogActivity extends RoboFragmentActivity implements ICatalogHandling, IMeasureHandling {

	@Inject
	private android.support.v4.app.FragmentManager mFragMan;

	@Inject
	private MeasureRepository mRepository;

	private Activity mContext;
	private boolean isLeftShowing = true;

	private CatalogListFragment mCatalogListFragment = null;
	private MeasureListFragment mMeasureListFragment = null;
	private MeasureInfoFragment mMeasureInfoFragment = null;
	private LinearLayout mRootLayout = null;	
	
	private TextView mMeasureEntryDateTV;
	private EditText mMeasureEvaluationTV;
	private RatingBar mMeasureRatingBar;
	
	private int mCatalogId;
	private Measure mMeasure;
	private boolean mShowMeasureFiltering = false;
	
	private SimpleDateFormat mFormatter;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.catalog_activity);

		mFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
		mRootLayout = (LinearLayout)findViewById(R.id.root);

		mContext = this;

		if (Utils.isTablet(this)) 
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
		}
		else
		{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
		}

		if (mFragMan.findFragmentById(R.id.left) == null) 
		{
			mCatalogListFragment = new CatalogListFragment();
			mFragMan.beginTransaction().add(R.id.left, mCatalogListFragment).commit();
		}
		mCatalogListFragment = (CatalogListFragment)mFragMan.findFragmentById(R.id.middle);

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
		case R.id.startSync:
			startSync();
			return true;
		case R.id.writeData:
			writeData();
			return true;
		case R.id.deleteLocalData:
			resetView();
			mRepository.deleteLocalData();
			return true;
		case R.id.showPreferences:
			startPreferences();
			return true;
		case R.id.sortName:
			sortMeasures(R.id.sortName);
			return false;
		case R.id.sortCreationDate:
			sortMeasures(R.id.sortCreationDate);
			return false;
		case R.id.sortDueDate:
			sortMeasures(R.id.sortDueDate);
			return false;
		case R.id.sortPriority:
			sortMeasures(R.id.sortPriority);
			return false;
		case R.id.sortStatus:
			sortMeasures(R.id.sortStatus);			
			return false;
		case R.id.searchMeasures:
			return false;
		case R.id.addMeasure:
			addMeasure();
			return true;
		case R.id.addCatalog:
			addCatalog();
			return true;
		case R.id.evaluateMeasure:
			evaluateMeasurePopUp();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void writeData() 
	{
		mRepository.writeDataToOdataService(mContext);
	}

	private void startSync() 
	{
		boolean unsavedData = false;
		if ( mRepository.mCatalogs != null) 
		{
			for (Catalog c : mRepository.mCatalogs) 
			{
				if (c.isManipulated()) 
				{
					unsavedData = true;
					break;
				}
			}

			for (Measure m : mRepository.mMeasures) 
			{
				if (m.isManipulated()) 
				{
					unsavedData = true;
					break;
				}
			}
		}

		if (unsavedData) 
		{
			// 1. Instantiate an AlertDialog.Builder with its constructor
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

			// 2. Chain together various setter methods to set the dialog characteristics
			builder.setMessage("Es gibt ungespeicherte Data. Durch ein erneutes Abrufen gehen diese verloren! Mˆchten sie fortfahren?")
			.setTitle("Ungespeicherte Daten");
			builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {

				}
			});

			builder.setPositiveButton("Fortfahren", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					syncAndResetView();
				}
			});

			// 3. Get the AlertDialog from create()
			AlertDialog dialog = builder.create();
			dialog.show();
		}
		else
		{
			syncAndResetView();
		}
	}

	private void syncAndResetView() 
	{
		resetView();
		mRepository.deleteLocalData();
		mRepository.getDataFromOdataService(mContext);
		invalidateOptionsMenu();
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
		.setTitle("Maﬂnahme bewerten")
		.setMessage("Bitte geben Sie eine Bewertung f¸r die Maﬂnahme ein")
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
	
	private void addCatalog() 
	{
		
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);

		final EditText titleBox = new EditText(mContext);
		titleBox.setHint("Name");
		layout.addView(titleBox);

		final EditText descriptionBox = new EditText(mContext);
		descriptionBox.setHint("Beschreibung");
		layout.addView(descriptionBox);
		
		new AlertDialog.Builder(mContext)
		.setTitle("Katalog hinzuf¸gen")
		.setMessage("Bitte geben Sie Name und Beschreibung des Katalogs an")
		.setView(layout)
		.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				if (titleBox.getText().length() > 0) 
				{
					Catalog newCatalog = new Catalog();
					newCatalog.setInternalId((int)mRepository.getMaxCatalogId()+1);
					newCatalog.setName(titleBox.getText().toString());
					newCatalog.setDescription(descriptionBox.getText().toString());
					newCatalog.setManipulated(true);
					mRepository.addCatalog(newCatalog);
					mRepository.persistCatalogs();
				}
			}
		}).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) 
			{
				// Do nothing.
			}
		}).show();
		
	}

	private void addMeasure() 
	{
		Intent startAddMeasure = new Intent(getApplicationContext(), AddMeasureActivity.class);
		Bundle bundle = new Bundle();
		bundle.putLong("CatalogId", mCatalogId);
		startAddMeasure.putExtras(bundle);
		startActivity(startAddMeasure);	
	}

	private void showLeft()
	{
		ThreePaneLayout tableRoot = (ThreePaneLayout) mRootLayout;
		tableRoot.showLeft();
		isLeftShowing=true;
		invalidateOptionsMenu();
		setTitle("Maﬂnahmen");
	}

	private void resetView() 
	{	
		if (!isLeftShowing) 
		{
			showLeft();
		}
		if (mShowMeasureFiltering) 
		{
			mShowMeasureFiltering = false;
			mMeasureListFragment.populateMeasures(0);
		}
		invalidateOptionsMenu();
	}

	private void startPreferences()
	{
		Intent startPreferences = new Intent(this, Preferences.class);
		this.startActivity(startPreferences);
	}

	@Override
	public boolean onNavigateUp() {
		if (!isLeftShowing){
			onBackPressed();
		}
		return true;	
	}

	@Override
	public void onBackPressed() {
		if (!isLeftShowing) 
		{
			showLeft();
		}
		else 
		{
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main_menu, menu);
		if (mShowMeasureFiltering) 
		{
			MenuItem searchItem = menu.findItem(R.id.searchMeasures);
			SearchView searchView = (SearchView) searchItem.getActionView();
			searchView.setQueryHint("Maﬂnahmen durchsuchen");
			searchView.setOnQueryTextListener(new OnQueryTextListener() {

				@Override
				public boolean onQueryTextSubmit(String query) 
				{
					return false;
				}

				@Override
				public boolean onQueryTextChange(String newText)
				{
					if(mMeasureListFragment != null) {
						mMeasureListFragment.filterMeasures(newText);
					}
					return false;
				}
			});
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		MenuItem sortMenuItem = menu.findItem(R.id.sortMeasures);
		MenuItem searchMenuItem = menu.findItem(R.id.searchMeasures);
		MenuItem addMeasureMenuItem = menu.findItem(R.id.addMeasure);
		MenuItem addCatalogMenuItem = menu.findItem(R.id.addCatalog);
		MenuItem evaluateMeasureMenuItem = menu.findItem(R.id.evaluateMeasure);
		
		sortMenuItem.setVisible(mShowMeasureFiltering);
		searchMenuItem.setVisible(mShowMeasureFiltering);
		addMeasureMenuItem.setVisible(mShowMeasureFiltering);
		addCatalogMenuItem.setVisible(true);
		
		if (!isLeftShowing) 
		{
			addCatalogMenuItem.setVisible(isLeftShowing);
		}
		
		
		if (!isLeftShowing && mMeasure != null) 
		{
			if (mMeasure.getStatus() < 2) 
			{
				evaluateMeasureMenuItem.setVisible(true);
			} 
			else
			{
				evaluateMeasureMenuItem.setVisible(false);
			}
		}
		else 
		{
			evaluateMeasureMenuItem.setVisible(false);
		}
		
		if (mRepository.mResponsibleSubjects.size() < 1) 
		{
			addMeasureMenuItem.setVisible(false);
			addCatalogMenuItem.setVisible(false);
			evaluateMeasureMenuItem.setVisible(false);
		}
		
		return true;
	}

	@Override
	public void onCatalogSelected(int catalogId) 
	{
		setTitle("Maﬂnahmen");
		mCatalogId = catalogId;
		if (Utils.isTablet(mContext))
		{
			mShowMeasureFiltering = true;
			invalidateOptionsMenu();
			if (mFragMan.findFragmentById(R.id.middle) == null)
			{
				mMeasureListFragment = new MeasureListFragment();
				Bundle args = new Bundle();
				args.putInt("CatalogId", catalogId);
				mMeasureListFragment.setArguments(args);
				mFragMan.beginTransaction().add(R.id.middle, mMeasureListFragment).commit();
			} 
			else
			{
				mMeasureListFragment.updateMeasures(catalogId);
			}
		}
		else
		{
			//if Phone start measureListActivity
			Intent showMeasureList = new Intent(getApplicationContext(), MeasureListActivity.class);
			Bundle bundle = new Bundle();
			bundle.putLong("CatalogId", catalogId);
			showMeasureList.putExtras(bundle);
			startActivity(showMeasureList);	
		}
	}

	@Override
	public void onMeasureSelected(int measureId) 
	{
		setTitle("Maﬂnahmendetails");
		mMeasure = mRepository.getMeasureById(measureId);
		
		invalidateOptionsMenu();
		if (isLeftShowing) 
		{
			ThreePaneLayout tabletLayout = (ThreePaneLayout) mRootLayout;
			tabletLayout.hideLeft();
			isLeftShowing=false;
		}
		if (mFragMan.findFragmentById(R.id.right) == null)
		{
			mMeasureInfoFragment = new MeasureInfoFragment();
			Bundle args = new Bundle();
			args.putInt("MeasureId", measureId);
			args.putInt("CatalogId", mCatalogId);
			mMeasureInfoFragment.setArguments(args);
			mFragMan.beginTransaction().add(R.id.right, mMeasureInfoFragment).commit();
		} 
		else
		{
			mMeasureInfoFragment.updateMeasureInfo(measureId);
		}
	}

	public void sortMeasures(int resId) 
	{
		mMeasureListFragment.sortMeasures(resId);
	}
}
