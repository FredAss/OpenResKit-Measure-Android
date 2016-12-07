package htw.bui.openreskit.measure.adapters;

import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.measure.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;


public class MeasureAdapter extends BaseAdapter implements Filterable {

	
	private List<Measure> mMeasures;
	private List<Measure> mFilteredMeasures;
	private final int mRowResID;
	private final LayoutInflater mLayoutInflater;
	final SimpleDateFormat mFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

	public MeasureAdapter(Activity context, int rowResID, LayoutInflater layoutInflater, List<Measure> measures) 
	{
		mRowResID = rowResID;
		mFilteredMeasures = measures;
		mMeasures = new ArrayList<Measure>(measures);
		mLayoutInflater = layoutInflater;
	}

	public int getCount() 
	{
		return mFilteredMeasures.size();
	}

	//returns position in List
	public Object getItem(int position) 
	{
		return mFilteredMeasures.get(position);
	}

	//returns the id of the item
	public long getItemId(int position) {
		return mFilteredMeasures.get(position).getInternalId();
	}

	// connects Unit members to be displayed with (text)views in a layout
	// per item
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView = convertView;
		if (rowView == null) 
		{
			rowView = mLayoutInflater.inflate(mRowResID, null);

			MeasureViewHolder viewHolder = new MeasureViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.text1);
			viewHolder.description = (TextView) rowView.findViewById(R.id.text2);
			viewHolder.prio = (TextView) rowView.findViewById(R.id.measurePrio);
			viewHolder.status = (ImageView) rowView.findViewById(R.id.ImageView02);
			viewHolder.dateWarning = (ImageView) rowView.findViewById(R.id.ImageView03);
			viewHolder.attachement = (ImageView) rowView.findViewById(R.id.ImageView04);
			viewHolder.image = (ImageView) rowView.findViewById(R.id.ImageView05);
			rowView.setTag(viewHolder);
		}

		final Measure m = mFilteredMeasures.get(position);
		MeasureViewHolder holder = (MeasureViewHolder) rowView.getTag();
		holder.name.setText(m.getName());
		holder.description.setText(mFormatter.format(m.getDueDate()));
		if (m.getPriority() == 0) 
		{
			holder.prio.setBackgroundColor(Color.GREEN);
		}
		else if (m.getPriority() == 1)
		{
			holder.prio.setBackgroundColor(Color.YELLOW);
		}
		else if (m.getPriority() == 2)
		{
			holder.prio.setBackgroundColor(Color.RED);
		}
		if (m.getStatus() == 0) 
		{
			holder.status.setVisibility(View.INVISIBLE);
		}
		else if (m.getStatus() == 1) 
		{
			holder.status.setVisibility(View.VISIBLE);
			holder.status.setBackgroundResource(R.drawable.av_play);
		}
		else if (m.getStatus() == 2)
		{
			holder.status.setVisibility(View.VISIBLE);
			holder.status.setBackgroundResource(R.drawable.navigation_accept);
		}
	
		Date now = new Date();
		if (m.getEntryDate() != null) 
		{
			if (m.getDueDate().before(m.getEntryDate())) 
			{
				holder.dateWarning.setVisibility(View.VISIBLE);
			}
			else
			{
				holder.dateWarning.setVisibility(View.GONE);
			}	
		}
		else 
		{
			if (m.getDueDate().before(now)) 
			{
				holder.dateWarning.setVisibility(View.VISIBLE);
			}
			else
			{
				holder.dateWarning.setVisibility(View.GONE);
			}	
		}
		
		
		if(m.getAttachedDocuments() != null) 
		{
			if (m.getAttachedDocuments().size() > 0)
			{
				holder.attachement.setVisibility(View.VISIBLE);
			}
		}
		else
		{
			holder.attachement.setVisibility(View.GONE);
		}
		
		if(m.getImageSource() != null) 
		{
			if (m.getImageSource() != null)
			{
				holder.image.setVisibility(View.VISIBLE);
			}
		}
		else
		{
			holder.image.setVisibility(View.GONE);
		}
		
		
		return rowView;
	}

	static class MeasureViewHolder 
	{
		public TextView name;
		public TextView description;
		public TextView prio;
		public ImageView status;
		public ImageView dateWarning;
		public ImageView attachement;
		public ImageView image;
	}

	@Override
	public Filter getFilter() 
	{
		Filter myFilter = new Filter() 
		{

			@Override
			protected FilterResults performFiltering(CharSequence constraint) 
			{
				String searchText = String.valueOf(constraint);

//				if (mMeasures == null) 
//				{
//					mMeasures = new ArrayList<Measure>(mFilteredMeasures);
//				}
				List<Measure> result;
				FilterResults filterResults = new FilterResults();

				if(constraint == null || constraint.length() <= 0)
				{
					result = new ArrayList<Measure>(mMeasures);
				}
				else
				{
					result = new ArrayList<Measure>();
					for (Measure m : mMeasures)
					{
						if (m.getName().toLowerCase(Locale.GERMANY).contains(searchText.toLowerCase(Locale.GERMANY)) || m.getDescription().toLowerCase(Locale.GERMANY).contains(searchText.toLowerCase(Locale.GERMANY)))
						{
							result.add(m);
						}
					}
					// Now assign the values and count to the FilterResults object
				}
				filterResults.values = result;
				filterResults.count = result.size();
				return filterResults;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence contraint, FilterResults results) 
			{
				ArrayList<Measure> resMeasures = (ArrayList<Measure>)results.values;
				mFilteredMeasures.clear();
				if (results != null && results.count > 0)
				{
					mFilteredMeasures.addAll(resMeasures);
					notifyDataSetChanged();
				}
				else 
				{
					notifyDataSetInvalidated();
				}
			}
		};
		return myFilter;
	}

}



