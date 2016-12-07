package htw.bui.openreskit.measure;

import java.util.EventObject;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.google.inject.Inject;
import htw.bui.openreskit.measure.adapters.CatalogAdapter;
import htw.bui.openreskit.measure.odata.MeasureRepository;
import htw.bui.openreskit.measure.odata.RepositoryChangedListener;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

public class CatalogListFragment extends RoboFragment 
{
	@Inject
	private MeasureRepository mRepository;
	
	@InjectView
	(R.id.catalogListView) ListView mCatalogListView;
	
	private Activity mContext;
	private Parcelable mListState;
	private ICatalogHandling mListener;
	
	private RepositoryChangedListener mRepositoryChangedListener = new RepositoryChangedListener() 
	{
		public void handleRepositoryChange(EventObject e) 
		{
			populateCatalogs();
			//quick and dirty
			getActivity().invalidateOptionsMenu();
		}
	};
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.catalog_list, container, false);
	}
	
	protected void populateCatalogs() 
	{
		CatalogAdapter adapter = new CatalogAdapter(mContext, R.layout.catalog_list_row, mRepository.mCatalogs);
		mCatalogListView.setAdapter(adapter);
		
	}

	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
		
		mRepository.addEventListener(mRepositoryChangedListener);

		if (savedInstanceState != null) 
		{
			mListState = savedInstanceState.getParcelable("listState");
		}
		mCatalogListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mCatalogListView.setCacheColorHint(Color.TRANSPARENT);
		mCatalogListView.setOnItemClickListener(mListItemClickListener);

		populateCatalogs();
		
	}
	
	@Override
	public void onSaveInstanceState (Bundle outState) 
	{
		super.onSaveInstanceState(outState);
		Parcelable state = mCatalogListView.onSaveInstanceState();
		outState.putParcelable("listState", state);
		int listPosition = mCatalogListView.getSelectedItemPosition();
		outState.putInt("listPosition", listPosition);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(mListState!=null){
			mCatalogListView.onRestoreInstanceState(mListState);
		} 
		mListState = null;
	}
	
	private OnItemClickListener mListItemClickListener = new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) 
		{
			mListener.onCatalogSelected((int)id);
		}
	};

	// Container Activity must implement this interface
	public interface ICatalogHandling 
	{
		public void onCatalogSelected(int id);

	}

	//Throw if interface not implemented
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (ICatalogHandling) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement ICatalogHandling");
		}
	}

}
