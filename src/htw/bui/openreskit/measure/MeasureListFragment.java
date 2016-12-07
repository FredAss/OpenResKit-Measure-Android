package htw.bui.openreskit.measure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import static htw.bui.openreskit.measure.enums.MeasureComparator.*;

import htw.bui.openreskit.domain.measure.*;
import htw.bui.openreskit.measure.adapters.MeasureAdapter;
import htw.bui.openreskit.measure.odata.MeasureRepository;
import htw.bui.openreskit.measure.odata.RepositoryChangedListener;
import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.google.inject.Inject;


public class MeasureListFragment extends RoboFragment 
{
	@Inject
	private MeasureRepository mRepository;

	@InjectView
	(R.id.measureListView) ListView mMeasureListView;

	private Activity mContext;
	private Parcelable mListState;
	private IMeasureHandling mListener;
	private List<Measure> mMeasures;
	private int mCatalogId;
	private MeasureAdapter mAdapter;
	private int mCurrentSortMethod;
	private boolean mSortNameDesc = false;
	private boolean mSortCreationDateDesc = false;
	private boolean mSortDueDateDesc = false;
	private boolean mSortPriorityDesc = false;
	private boolean mSortStatusDesc = false;

	private RepositoryChangedListener mRepositoryChangedListener = new RepositoryChangedListener() 
	{
		public void handleRepositoryChange(EventObject e) 
		{
			mAdapter.notifyDataSetChanged();
		}
	};
	
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();

		if (savedInstanceState != null) 
		{
			mListState = savedInstanceState.getParcelable("listState");
		}
		mMeasureListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mMeasureListView.setCacheColorHint(Color.TRANSPARENT);
		mMeasureListView.setOnItemClickListener(mListItemClickListener);

		mRepository.addEventListener(mRepositoryChangedListener);
		
		Bundle args = getArguments();
		if (args != null) 
		{
			mCatalogId = args.getInt("CatalogId");
		}
		populateMeasures(mCatalogId);
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.measure_list, container, false);
	}

	protected void populateMeasures(int catalogId) 
	{
		mCatalogId = catalogId;
		Catalog c = mRepository.getCatalogById(catalogId);
		if (c != null) 
		{			
			if (c.getMeasures() != null) 
			{
				mMeasures = new ArrayList<Measure>(c.getMeasures());
			} 
			else 
			{
				mMeasures = new ArrayList<Measure>();
			}
		}
		else 
		{
			mMeasures = new ArrayList<Measure>();
		}
		
		mAdapter = new MeasureAdapter(mContext, R.layout.measure_list_row, mContext.getLayoutInflater(), mMeasures);
		mMeasureListView.setAdapter(mAdapter);
	}

	public void updateMeasures(int catalogId) 
	{
		mContext = getActivity();
		populateMeasures(catalogId);
	}

	@Override
	public void onSaveInstanceState (Bundle outState) 
	{
		super.onSaveInstanceState(outState);
		Parcelable state = mMeasureListView.onSaveInstanceState();
		outState.putParcelable("listState", state);
		int listPosition = mMeasureListView.getSelectedItemPosition();
		outState.putInt("listPosition", listPosition);
	}

	@Override
	public void onResume() 
	{
		super.onResume();
		if(mListState!=null)
		{
			mMeasureListView.onRestoreInstanceState(mListState);
		} 
		mListState = null;
		populateMeasures(mCatalogId);
	}

	private OnItemClickListener mListItemClickListener = new OnItemClickListener() 
	{
		public void onItemClick(AdapterView<?> arg0, View view, int position, long id) 
		{
			mListener.onMeasureSelected((int)id);
		}
	};

	// Container Activity must implement this interface
	public interface IMeasureHandling 
	{
		public void onMeasureSelected(int id);
	}

	//Throw if interface not implemented
	@Override
	public void onAttach(Activity activity) 
	{
		super.onAttach(activity);
		try 
		{
			mListener = (IMeasureHandling) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement IMeasureHandling");
		}
	}

	public void sortMeasures(int resId) 
	{
		switch (resId) 
		{
		case R.id.sortName:
			if (mCurrentSortMethod == R.id.sortName) 
			{
				mSortNameDesc = !mSortNameDesc;
			}  
			if (mSortNameDesc) 
			{
				Collections.sort(mMeasures, decending(NAME));
			}
			else
			{
				Collections.sort(mMeasures, NAME);
			}
			mCurrentSortMethod = R.id.sortName;
			break;
		case R.id.sortCreationDate:
			if (mCurrentSortMethod == R.id.sortCreationDate) 
			{
				mSortCreationDateDesc = !mSortCreationDateDesc;
			}  
			if (mSortCreationDateDesc) 
			{
				Collections.sort(mMeasures, decending(CREATION_DATE));
			}
			else
			{
				Collections.sort(mMeasures, CREATION_DATE);
			}
			mCurrentSortMethod = R.id.sortCreationDate;
			break;
		case R.id.sortDueDate:
			if (mCurrentSortMethod == R.id.sortDueDate) 
			{
				mSortDueDateDesc = !mSortDueDateDesc;
			}  
			if (mSortDueDateDesc) 
			{
				Collections.sort(mMeasures, decending(DUE_DATE));
			}
			else
			{
				Collections.sort(mMeasures, DUE_DATE);
			}
			mCurrentSortMethod = R.id.sortDueDate;
			break;
		case R.id.sortPriority:
			if (mCurrentSortMethod == R.id.sortPriority) 
			{
				mSortPriorityDesc = !mSortPriorityDesc;
			}  
			if (mSortPriorityDesc) 
			{
				Collections.sort(mMeasures, decending(PRIORITY));
			}
			else
			{
				Collections.sort(mMeasures, PRIORITY);
			}
			mCurrentSortMethod = R.id.sortPriority;
			break;
		case R.id.sortStatus:
			if (mCurrentSortMethod == R.id.sortStatus) 
			{
				mSortStatusDesc = !mSortStatusDesc;
			}  
			if (mSortStatusDesc) 
			{
				Collections.sort(mMeasures, decending(STATUS));
			}
			else
			{
				Collections.sort(mMeasures, STATUS);
			}
			mCurrentSortMethod = R.id.sortStatus;
			break;
		} 
		mAdapter.notifyDataSetChanged();		
	}

	public void filterMeasures(String newText)
	{
		if (mAdapter != null) 
		{
			mAdapter.getFilter().filter(newText);
			mAdapter.notifyDataSetChanged();		
		}
	}
}
