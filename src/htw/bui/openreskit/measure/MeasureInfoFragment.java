package htw.bui.openreskit.measure;

import htw.bui.openreskit.domain.organisation.Document;
import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.domain.organisation.Employee;
import htw.bui.openreskit.domain.organisation.EmployeeGroup;
import htw.bui.openreskit.measure.enums.Priority;
import htw.bui.openreskit.measure.enums.Status;
import htw.bui.openreskit.measure.odata.DownloadCompleteListener;
import htw.bui.openreskit.measure.odata.MeasureRepository;
import htw.bui.openreskit.measure.odata.RepositoryChangedListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.EventObject;
import java.util.Locale;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.inject.Inject;

public class MeasureInfoFragment extends RoboFragment 
{
	@Inject
	private MeasureRepository mRepository;

	@InjectView
	(R.id.measureNameTV) TextView mMeasureNameTV; 

	@InjectView
	(R.id.measureDueDateTV) TextView mMeasureDueDateTV;
	@InjectView
	(R.id.measureCreationDateTV) TextView mMeasureCreationDateTV;

	@InjectView
	(R.id.measureResponsibleSubjectImage) ImageView mMeasureResponsibleSubjectImage;
	@InjectView
	(R.id.measureResponsibleSubjectTV) TextView mMeasureResponsibleSubjectTV;

	@InjectView
	(R.id.measureStatusTV) TextView mMeasureStatusTV;

	@InjectView
	(R.id.measureEvaluationSection) LinearLayout mMeasureEvaluationSection;
	
	@InjectView
	(R.id.measureEntryDateTV) TextView mMeasureEntryDateTV;

	@InjectView
	(R.id.measureEvaluationTV) TextView mMeasureEvaluationTV;
	
	@InjectView
	(R.id.measureRatingBar) RatingBar mMeasureRatingBar;

	@InjectView
	(R.id.measureDescriptionTV) TextView mMeasureDescriptionTV;

	@InjectView
	(R.id.measurePriorityTV) TextView mMeasurePriorityTV;

	@InjectView
	(R.id.measureImageSection) LinearLayout mMeasureImageSection;

	@InjectView
	(R.id.measureImage) ImageView mMeasureImage;

	@InjectView
	(R.id.attachedDocTable) TableLayout mAttachedDocTable;


	@InjectView
	(R.id.measureAttachedDocsSection) LinearLayout mMeasureAttachedDocsSection;

	private Activity mContext;
	private MimeTypeMap mMimeMapper;
	private OnClickListener mImageOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) 
		{
			Intent showImageDetails = new Intent(getActivity(), ImageDetailActivity.class);
			Bundle args = new Bundle();
			args.putInt("MeasureId", mMeasure.getInternalId());
			showImageDetails.putExtras(args);
			startActivity(showImageDetails);
		}
	};

	private RepositoryChangedListener mRepositoryChangedListener = new RepositoryChangedListener() 
	{
		public void handleRepositoryChange(EventObject e) 
		{
			if (mMeasure != null) 
			{
				if (mRepository.mMeasures.size() > 0) 
				{
					getMeasureInfo(mMeasureId);
				}
			}
		}
	};
	
	private DownloadCompleteListener mDownloadCompleteListener = new DownloadCompleteListener() 
	{
		public void handleDownloadComplete(EventObject e) 
		{
			openFile();
		}
	};

	private OnClickListener mTableRowOnClickListener = new OnClickListener() 
	{
		@Override
		public void onClick(View v) 
		{
			Document d = (Document) v.getTag();
			mRepository.downloadAttachedDocumentSource(d.getId());	
		}
	};

	private Measure mMeasure;
	private int mMeasureId;

	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);

		Bundle args = getArguments();
		if (args != null)
			mMeasureId = args.getInt("MeasureId");
		getMeasureInfo(mMeasureId);
	}


	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) 
	{
		mContext = getActivity();
		mMimeMapper = MimeTypeMap.getSingleton();
		mRepository.addEventListener(mDownloadCompleteListener);
		mRepository.addEventListener(mRepositoryChangedListener);
		//Inflate the layout for this fragment
		return inflater.inflate(R.layout.measure_info, container, false);
	}

	@SuppressWarnings("deprecation")
	protected void openFile() 
	{
		FileOutputStream fos;
		try {
			fos = mContext.openFileOutput(mRepository.mDocument.getName() , Context.MODE_WORLD_READABLE);
			fos.write((byte[]) mRepository.mDocument.getDocumentSource().getBinarySource());
			fos.close();
			Uri uri = Uri.fromFile(new File(mContext.getFilesDir(), mRepository.mDocument.getName()));
			String mimeType = getMimeTypeForExtension(getExtensionForFilename(mRepository.mDocument.getName()));

			if (mimeType == null)
				mimeType = "*/*";

			Intent fileOpenIntent = new Intent(Intent.ACTION_VIEW);
			fileOpenIntent.setDataAndType(uri, mimeType);
			startActivity(fileOpenIntent);

		} catch (Exception e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) 
	{
		super.onViewCreated(view, savedInstanceState);
		mMeasureImage.setOnClickListener(mImageOnClickListener);
		
	}

	protected void getMeasureInfo(int measureId) 
	{
		mMeasureId = measureId;
		SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);

		mMeasure = mRepository.getMeasureById(measureId);
		mMeasureNameTV.setText(mMeasure.getName());
		mMeasureCreationDateTV.setText("vom " + formatter.format(mMeasure.getCreationDate()));

		mMeasureDueDateTV.setText(formatter.format(mMeasure.getDueDate()));

		if (mMeasure.getImageSource() != null) 
		{
			mMeasureImageSection.setVisibility(View.VISIBLE);
			byte[] byteArray = Base64.decode(mMeasure.getImageSource().getBinarySource(), 0);
			Bitmap bMap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
			mMeasureImage.setImageBitmap(bMap);
		}
		else
		{
			mMeasureImageSection.setVisibility(View.GONE);
		}

		if (mMeasure.getResponsibleSubject() != null) 
		{
			if (mMeasure.getResponsibleSubject().getClass() == Employee.class) 
			{
				Employee e = (Employee)mMeasure.getResponsibleSubject();
				mMeasureResponsibleSubjectImage.setBackgroundResource(R.drawable.social_person);
				mMeasureResponsibleSubjectTV.setText(e.getFirstName() + " " + e.getLastName());
			}
			else
			{
				EmployeeGroup g = (EmployeeGroup)mMeasure.getResponsibleSubject();
				mMeasureResponsibleSubjectImage.setBackgroundResource(R.drawable.social_group);
				mMeasureResponsibleSubjectTV.setText(g.getName());
			}
		}
		else
		{
			mMeasureResponsibleSubjectImage.setBackgroundResource(R.drawable.social_person);
			mMeasureResponsibleSubjectTV.setText("nicht angegeben");
		}

		mMeasureStatusTV.setText(Status.values()[mMeasure.getStatus()].toString());
		
		if (mMeasure.getEntryDate() != null) 
		{
			mMeasureEvaluationSection.setVisibility(View.VISIBLE);
			mMeasureEntryDateTV.setText(formatter.format(mMeasure.getEntryDate()));
			mMeasureEvaluationTV.setText(mMeasure.getEvalutation());
			mMeasureRatingBar.setRating((float)mMeasure.getEvaluationRating()*5);
		}
		else 
		{
			mMeasureEvaluationSection.setVisibility(View.GONE);
		}
		
		
		mMeasureDescriptionTV.setText(mMeasure.getDescription());

		mMeasurePriorityTV.setText(Priority.values()[mMeasure.getPriority()].toString());

		if (mMeasure.getAttachedDocuments() != null) 
		{
			if (mMeasure.getAttachedDocuments().size() > 0) {
				mMeasureAttachedDocsSection.setVisibility(View.VISIBLE);
				mAttachedDocTable.removeAllViews();
				int color = Color.rgb(70, 70, 70);
				for (Document d : mMeasure.getAttachedDocuments()) 
				{

					TableRow row = (TableRow)LayoutInflater.from(mContext).inflate(R.layout.attached_doc_table_row, null);
					row.setTag(d);
					row.setOnClickListener(mTableRowOnClickListener);
					String extension = getExtensionForFilename(d.getName());
					String mimeType = getMimeTypeForExtension(extension);

					if (mimeType.equals("application/pdf"))
					{
						color = Color.rgb(255, 0, 0);
					}
					else if (mimeType.equals("application/msword") || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) 
					{
						color = Color.rgb(70, 100, 255);
					}
					else if (mimeType.equals("application/vnd.ms-excel") || mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
					{
						color = Color.rgb(0, 170, 0);
					}
					else if (mimeType.equals("application/vnd.ms-powerpoint") || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
					{
						color = Color.rgb(255, 145, 0);
					}
					else if (mimeType.equals("application/x-7z-compressed") || mimeType.equals("application/zip") ||  mimeType.equals("application/x-rar-compressed")) 
					{
						color = Color.rgb(255, 217, 0);
					}

					TextView tv1 = (TextView)row.findViewById(R.id.textItem1);
					tv1.setBackgroundColor(color);
					tv1.setText(extension);

					TextView tv2 = (TextView)row.findViewById(R.id.textItem2);
					tv2.setText(d.getName());

					mAttachedDocTable.addView(row);
				}
			}
			else 
			{
				mMeasureAttachedDocsSection.setVisibility(View.GONE);
			}
		}
		else 
		{
			mMeasureAttachedDocsSection.setVisibility(View.GONE);
		}

	}

	public String getMimeTypeForExtension(String filename) 
	{
		return mMimeMapper.getMimeTypeFromExtension(getExtensionForFilename(filename));
	}

	public String getExtensionForFilename(String filename) 
	{
		return filename.substring(filename.lastIndexOf('.')+1, filename.length());
	}

	public void updateMeasureInfo(int measureId) 
	{
		getMeasureInfo(measureId);
	}



}
