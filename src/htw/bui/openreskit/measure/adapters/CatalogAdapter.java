package htw.bui.openreskit.measure.adapters;

import htw.bui.openreskit.domain.measure.Catalog;
import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.measure.R;

import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CatalogAdapter extends BaseAdapter {

	private final Activity mContext;
	private final List<Catalog> mCatalogs;
	private final int mRowResID;
	private final LayoutInflater mLayoutInflater;

	public CatalogAdapter(final Activity context, final int rowResID, final List<Catalog> catalogs) 
	{
		mContext = context;
		mRowResID = rowResID;
		mCatalogs = catalogs;
		mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() 
	{
		return mCatalogs.size();
	}

	//returns position in List
	public Object getItem(int position) 
	{
		return mCatalogs.get(position);
	}

	//returns the Database id of the item
	public long getItemId(int position) {
		return mCatalogs.get(position).getInternalId();
	}

	// connects Unit members to be displayed with (text)views in a layout
	// per item
	public View getView(int position, View convertView, ViewGroup parent) {

		View rowView = convertView;
		if (rowView == null) 
		{
			rowView = mLayoutInflater.inflate(mRowResID, null);

			CatalogViewHolder viewHolder = new CatalogViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.text1);
			viewHolder.description = (TextView) rowView.findViewById(R.id.text2);
			rowView.setTag(viewHolder);
		}

		final Catalog c = mCatalogs.get(position);
		CatalogViewHolder holder = (CatalogViewHolder) rowView.getTag();
		holder.name.setText(c.getName().toString());
		if (c.getMeasures() != null) 
		{
			
			int closed = 0;
			int total = 0;
			for (Measure m : c.getMeasures()) 
			{
				switch (m.getStatus()) 
				{
				case 0:
					total++;
					break;
				case 1:
					total++;
					break;
				case 2:
					closed++;
					total++;
					break;
				}
			}
			
			holder.description.setText(closed + " von " + total + " Maﬂnahmen abgeschlossen");
		}		
		else
		{
			holder.description.setText("keine Maﬂnahmen");	
		}

		return rowView;
	}

	static class CatalogViewHolder 
	{
		public TextView name;
		public TextView description;
	}

}



