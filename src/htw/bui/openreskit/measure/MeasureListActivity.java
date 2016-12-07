package htw.bui.openreskit.measure;

import htw.bui.openreskit.measure.MeasureListFragment.IMeasureHandling;
import htw.bui.openreskit.measure.odata.MeasureRepository;

import javax.inject.Inject;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import roboguice.activity.RoboFragmentActivity;

public class MeasureListActivity extends RoboFragmentActivity implements IMeasureHandling
{
	@Inject
	private FragmentManager mFragMan;
	
	@Inject
	private MeasureRepository mRepository;
	
	private long mCatalogId;
	private MeasureListFragment mMeasureListFragment;

	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.measure_list_fragment);
		
        Intent launchingIntent = getIntent();
        mCatalogId = launchingIntent.getExtras().getLong("CatalogId");
        mMeasureListFragment = (MeasureListFragment) mFragMan.findFragmentById(R.id.measure_list_fragment);
        mMeasureListFragment.updateMeasures((int)mCatalogId);
        
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main_menu, menu);
		
		MenuItem searchItem = menu.findItem(R.id.searchMeasures);
		SearchView searchView = (SearchView) searchItem.getActionView();
		searchView.setQueryHint("Maﬂnahmen durchsuchen");
		searchView.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText)
			{
				mMeasureListFragment.filterMeasures(newText);
				return false;
			}
		});

		MenuItem startSyncMenuItem = menu.findItem(R.id.startSync);
		startSyncMenuItem.setVisible(false);
		MenuItem deleteDataMenuItem = menu.findItem(R.id.deleteLocalData);
		deleteDataMenuItem.setVisible(false);
		MenuItem writeDataMenuItem = menu.findItem(R.id.writeData);
		writeDataMenuItem.setVisible(false);
		MenuItem addCatalogMenuItem = menu.findItem(R.id.addCatalog);
		addCatalogMenuItem.setVisible(false);
		MenuItem evaluateMeasureMenuItem = menu.findItem(R.id.evaluateMeasure);
		evaluateMeasureMenuItem.setVisible(false);
		MenuItem addMeasureMenuItem = menu.findItem(R.id.addMeasure);
		if (mRepository.mResponsibleSubjects.size() < 1) 
		{
			addMeasureMenuItem.setVisible(false);
		}
		
		return true;
	}
	
	private void addMeasure() 
	{
		
		Intent startAddMeasure = new Intent(getApplicationContext(), AddMeasureActivity.class);
		Bundle bundle = new Bundle();
		bundle.putLong("CatalogId", mCatalogId);
		startAddMeasure.putExtras(bundle);
		startActivity(startAddMeasure);	
	}
	
	private void startPreferences()
	{
		Intent startPreferences = new Intent(this, Preferences.class);
		this.startActivity(startPreferences);
	}
	
	public void sortMeasures(int resId) 
	{
		mMeasureListFragment.sortMeasures(resId);
	}

	@Override
	public void onMeasureSelected(int measureId) 
	{
		Intent showMeasureInfo = new Intent(getApplicationContext(), MeasureInfoActivity.class);
		Bundle bundle = new Bundle();
		bundle.putLong("MeasureId", measureId);
		bundle.putLong("CatalogId", mCatalogId);
		showMeasureInfo.putExtras(bundle);
		startActivity(showMeasureInfo);	
	}
	
	@Override
	public boolean onNavigateUp() 
	{
		onBackPressed();
		return true;	
	}
}
