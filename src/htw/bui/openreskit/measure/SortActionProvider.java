package htw.bui.openreskit.measure;

import android.content.Context;
import android.view.ActionProvider;
import android.view.MenuInflater;
import android.view.SubMenu;
import android.view.View;
import htw.bui.openreskit.measure.R;

public class SortActionProvider extends ActionProvider {

	private Context mContext;

	public SortActionProvider(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public View onCreateActionView() 
	{
		return null;
	}

	@Override
	public boolean hasSubMenu(){
		return true;
	}

	@Override
	public void onPrepareSubMenu(SubMenu subMenu){
		subMenu.clear();
		MenuInflater inflater = new MenuInflater(mContext);
		inflater.inflate(R.menu.measure_sort_menu, subMenu);
	}
}
