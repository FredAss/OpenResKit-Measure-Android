package htw.bui.openreskit.measure.odata;


import htw.bui.openreskit.domain.measure.Catalog;
import htw.bui.openreskit.domain.organisation.Document;
import htw.bui.openreskit.domain.measure.MeasureImageSource;
import htw.bui.openreskit.domain.measure.Measure;
import htw.bui.openreskit.domain.organisation.ResponsibleSubject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;



import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MeasureRepository
{
	private Activity mContext;
	public List<Catalog> mCatalogs;
	public List<Measure> mMeasures;
	public List<MeasureImageSource> mImages;
	public Document mDocument;
	public List<ResponsibleSubject> mResponsibleSubjects;
	private ProgressDialog mProgressDialog;
	private List<RepositoryChangedListener> mListeners = new ArrayList<RepositoryChangedListener>();
	private List<DownloadCompleteListener> mDlListeners = new ArrayList<DownloadCompleteListener>();
	private static ObjectMapper mObjectMapper;
	private static SimpleDateFormat mOdataDataFormatter;

	@Inject
	public MeasureRepository(Activity ctx)
	{
		mObjectMapper = new ObjectMapper();
		mContext = ctx;
		mCatalogs = getCatalogsFromFile();
		mResponsibleSubjects = getResponsibleSubjectsFromFile();
		mMeasures = getAllMeasures();
		mImages = getAllImages();
		mOdataDataFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.GERMANY);
	}

	public Catalog getCatalogById(long catalogId) 
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean personalMode = prefs.getBoolean("personalMode", false);
		String responsibleSubjectId = prefs.getString("default_responsibleSubject", "none");

		for (Catalog c : mCatalogs) 
		{
			try {
				if (c.getInternalId() == catalogId) 
				{

					Catalog tempCatalog = (Catalog) c.clone();

					if (personalMode == false) 
					{ 
						return tempCatalog;
					}
					else 
					{
						if (responsibleSubjectId != "none") 
						{
							if (tempCatalog.getMeasures() != null) 
							{
								List<Measure> measures = new ArrayList<Measure>();
								for (Measure m : tempCatalog.getMeasures()) 
								{
									if (m.getResponsibleSubject() != null) 
									{
										if (m.getResponsibleSubject().getId() == Long.parseLong(responsibleSubjectId)) 
										{
											measures.add(m);
										}
									}
								}
								tempCatalog.setMeasures(measures);
								return tempCatalog;
							}
						}
					}
				}
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public Measure getMeasureById(long measureId) 
	{
		for (Measure m : mMeasures) 
		{
			if (m.getInternalId() == measureId) {
				return m;
			}
		}
		return null;
	}

	public void addCatalog(Catalog catalog) 
	{
		mCatalogs.add(catalog);
		fireRepositoryUpdate();
	}

	public long getMaxCatalogId() 
	{
		long maxId = 0;
		for (Catalog c : mCatalogs) 
		{
			if (c.getInternalId() > maxId) 
			{
				maxId = c.getInternalId(); 
			}
		}
		return maxId;
	}

	public long getMaxMeasureId() 
	{
		long maxId = 0;
		for (Measure m : mMeasures) 
		{
			if (m.getInternalId() > maxId) 
			{
				maxId = m.getInternalId(); 
			}
		}
		return maxId;
	}

	public long getMaxImageId() 
	{
		long maxId = 0;
		for (MeasureImageSource i : mImages) 
		{
			if (i.getInternalId() > maxId) 
			{
				maxId = i.getInternalId(); 
			}
		}
		return maxId;
	}

	public ResponsibleSubject getResponsibleSubjectById(long responsibleSubjectId) 
	{
		for (ResponsibleSubject rs : mResponsibleSubjects) 
		{
			if (rs.getId() == responsibleSubjectId) {
				return rs;
			}
		}
		return null;
	}

	public synchronized List<Measure> getAllMeasures() 
	{
		List<Measure> allMeasures = new ArrayList<Measure>();

		for (Catalog c : mCatalogs) 
		{
			if (c.getMeasures() != null) 
			{
				for (Measure m : c.getMeasures()) 
				{
					allMeasures.add(m);
				}
			}
		}
		return allMeasures;
	}

	public synchronized List<MeasureImageSource> getAllImages() 
	{
		List<MeasureImageSource> allImages = new ArrayList<MeasureImageSource>();

		for (Measure m : mMeasures) 
		{
			if (m.getImageSource() != null) 
			{
				allImages.add(m.getImageSource());
			}
		}
		return allImages;
	}

	public void addMeasureToCatalog(Measure m, long catalogId) 
	{
		for (Catalog c : mCatalogs) 
		{
			if (c.getInternalId() == catalogId) 
			{
				if (c.getMeasures() == null)
				{
					c.setMeasures(new ArrayList<Measure>());
				}
				c.getMeasures().add(m);
				c.setManipulated(true);
				mMeasures = getAllMeasures();
				fireRepositoryUpdate();
				break;
			}
		}
	}

	public void setCatalogManipulated(long catalogId) 
	{
		for (Catalog c : mCatalogs) 
		{
			if (c.getInternalId() == catalogId) 
			{
				c.setManipulated(true);
				break;
			}
		}
	}

	public void fireUpdate() 
	{
		fireRepositoryUpdate();
	}

	public synchronized void addEventListener(RepositoryChangedListener listener)  
	{
		mListeners.add(listener);
	}

	public synchronized void removeEventListener(RepositoryChangedListener listener)   
	{
		mListeners.remove(listener);
	}

	public synchronized void addEventListener(DownloadCompleteListener listener)  
	{
		mDlListeners.add(listener);
	}

	public synchronized void removeEventListener(DownloadCompleteListener listener)   
	{
		mDlListeners.remove(listener);
	}

	private synchronized void fireRepositoryUpdate() 
	{
		RepositoryChangedEvent event = new RepositoryChangedEvent(this);
		Iterator<RepositoryChangedListener> i = mListeners.iterator();
		while(i.hasNext())  
		{
			((RepositoryChangedListener) i.next()).handleRepositoryChange(event);
		}
	}

	private synchronized void fireDownloadComplete() 
	{
		DownloadCompleteEvent event = new DownloadCompleteEvent(this);
		Iterator<DownloadCompleteListener> i = mDlListeners.iterator();
		while(i.hasNext())  
		{
			((DownloadCompleteListener) i.next()).handleDownloadComplete(event);
		}
	}

	private List<Catalog> getCatalogsFromFile() {

		ArrayList<Catalog> catalogs = new ArrayList<Catalog>();
		String catalogsJSON = loadFromExternal("catalogs.json");

		if (catalogsJSON != null) 
		{
			try {
				JSONArray catalogsJSONArray = new JSONArray(catalogsJSON);

				for (int i = 0; i < catalogsJSONArray.length(); i++) 
				{
					JSONObject catalogJSON = catalogsJSONArray.getJSONObject(i);
					Catalog c = mObjectMapper.readValue(catalogJSON.toString(), Catalog.class);
					c.setInternalId(c.getId());
					catalogs.add(c);
					if (c.getMeasures() != null) 
					{
						if (c.getMeasures().size() > 0) 
						{
							for (Measure m : c.getMeasures()) 
							{
								m.setInternalId(m.getId());
								if (m.getImageSource() != null) 
								{
									m.getImageSource().setInternalId(m.getImageSource().getId());
								}
							}
						}
					}
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return catalogs;
	}

	private List<ResponsibleSubject> getResponsibleSubjectsFromFile() {

		ArrayList<ResponsibleSubject> responsibleSubjects = new ArrayList<ResponsibleSubject>();
		String responsibleSubjectsJSON = loadFromExternal("responsibleSubjects.json");

		if (responsibleSubjectsJSON != null) 
		{
			try {
				JSONArray responsibleSubjectsJSONArray = new JSONArray(responsibleSubjectsJSON);

				for (int i = 0; i < responsibleSubjectsJSONArray.length(); i++) 
				{
					JSONObject responsibleSubjectJSON = responsibleSubjectsJSONArray.getJSONObject(i);
					ResponsibleSubject rs = mObjectMapper.readValue(responsibleSubjectJSON.toString(), ResponsibleSubject.class);
					responsibleSubjects.add(rs);
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return responsibleSubjects;
	}

	private void saveResponsibleSubjectToFile(List<ResponsibleSubject> responsibleSubjects) 
	{
		saveToExternal(serializeResponsibleSubjects(responsibleSubjects), "responsibleSubjects.json");
	}

	private class SaveCatalogsToFileTask extends AsyncTask<List<Catalog>, Void, Void>
	{
		protected Void doInBackground(List<Catalog>... params)
		{

			saveToExternal(serializeCatalogs(params[0]), "catalogs.json");
			return null;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();

		}


		@Override
		protected void onPostExecute(Void results)
		{

		}

	}
	
	public void persistCatalogs() 
	{
		new SaveCatalogsToFileTask().execute((mCatalogs));	
	}
	
	

	public void getDataFromOdataService(Activity _start)
	{
		if (isOnline())
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");
			if (defaultIP == "none" || port == "none" || username == "none" || password == "none") 
			{
				Toast.makeText(mContext, "Bitte geben sie in den Einstellungen zuerst die Verbingungsparamenter an", Toast.LENGTH_SHORT).show();
			}
			else
			{
				new GetData().execute((Void[]) null);	
			}
		} 
		else
		{
			Toast.makeText(mContext, "Keine Verbindung!", Toast.LENGTH_SHORT).show();
		}
	}

	public void downloadAttachedDocumentSource(int documentSourceId) {

		if (isOnline())
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");
			if (defaultIP == "none" || port == "none" || username == "none" || password == "none") 
			{
				Toast.makeText(mContext, "Bitte geben sie in den Einstellungen zuerst die Verbingungsparamenter an", Toast.LENGTH_SHORT).show();
			}
			else
			{
				new GetDocumentByIdAndOpen().execute(documentSourceId);	
			}
		} 
		else
		{
			Toast.makeText(mContext, "Es besteht keine Verbindung!", Toast.LENGTH_SHORT).show();
		}
	}

	public void writeDataToOdataService(Activity _start)
	{
		if (isOnline())
		{
			new WriteData().execute((Void[]) null);
		} 
		else
		{
			Toast.makeText(mContext, "Keine Verbindung!", Toast.LENGTH_SHORT).show();
		}

	}
	private boolean isOnline()
	{
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting())
		{
			return true;
		}
		return false;
	}

	private class GetDocumentByIdAndOpen extends AsyncTask<Integer, Void, Void>
	{
		protected Void doInBackground(Integer... params)
		{

			int documentId = params[0];

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");
			try 
			{
				//DS
				String expandDocumentSource = "DocumentSource";
				JSONObject documentJSONObject = getJSONObjectFromOdata(defaultIP, port, username, password, "OpenResKitHub", "Documents("+documentId+")", expandDocumentSource);
				mDocument = mObjectMapper.readValue(documentJSONObject.toString(), Document.class);

			} 
			catch (final Exception e) 
			{
				mContext.runOnUiThread(new Runnable() {

					public void run() 
					{
						Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
					}
				});
				e.printStackTrace();
			}
			return null;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage("Lade Dokument herunter...");
			mProgressDialog.show();
		}


		@Override
		protected void onPostExecute(Void results)
		{
			mProgressDialog.dismiss();
			fireDownloadComplete();
		}

	}

	private class GetData extends AsyncTask<Void, Void, Integer>
	{
		protected Integer doInBackground(Void... params)
		{
			mCatalogs = new ArrayList<Catalog>();
			mResponsibleSubjects = new ArrayList<ResponsibleSubject>();

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");

			int counter = 0;
			try 
			{
				//Catalogs
				String expandCatalogs = "Measures/MeasureImageSource,Measures/AttachedDocuments,Measures/ResponsibleSubject";
				JSONArray catalogJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "Catalogs", expandCatalogs, null);
				if (catalogJSONArray != null) 
				{
					for (int i = 0; i < catalogJSONArray.length(); i++) 
					{
						JSONObject catalogJSON = catalogJSONArray.getJSONObject(i);
						Catalog c = mObjectMapper.readValue(catalogJSON.toString(), Catalog.class);
						c.setInternalId(c.getId());
						mCatalogs.add(c);
						counter++;
						if (c.getMeasures() != null) 
						{
							if (c.getMeasures().size() > 0) 
							{
								for (Measure m : c.getMeasures()) 
								{
									m.setInternalId(m.getId());
									if (m.getImageSource() != null) 
									{
										m.getImageSource().setInternalId(m.getImageSource().getId());
									}
								}
							}
						}
					}
				}
				else 
				{
					mContext.runOnUiThread(new Runnable() {

						public void run() {
							Toast.makeText(mContext, "Auf dem Server befinden sich keine Daten.", Toast.LENGTH_SHORT).show();

						}
					});
				}
			} 
			catch (final Exception e) 
			{
				mContext.runOnUiThread(new Runnable() {

					public void run() {
						Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

					}
				});
				e.printStackTrace();
			}

			//ResponsibleSubjects

			try 
			{
				String expandResponsibleSubjects = "OpenResKit.DomainModel.Employee/Groups";
				JSONArray responsibleSubjectsJSONArray = getJSONArrayFromOdata(defaultIP, port, username, password, "OpenResKitHub", "ResponsibleSubjects", expandResponsibleSubjects, null);

				for (int i = 0; i < responsibleSubjectsJSONArray.length(); i++) 
				{
					JSONObject responsibleSubjectsJSON = responsibleSubjectsJSONArray.getJSONObject(i);
					ResponsibleSubject rs = mObjectMapper.readValue(responsibleSubjectsJSON.toString(), ResponsibleSubject.class);
					mResponsibleSubjects.add(rs);

				}
			} 
			catch ( Exception e) 
			{

				e.printStackTrace();
			}

			return counter;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage("Aktualisiere Daten");
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			new SaveCatalogsToFileTask().execute((mCatalogs));	
			saveResponsibleSubjectToFile(mResponsibleSubjects);
			mMeasures = getAllMeasures();
			mImages = getAllImages();
			mProgressDialog.dismiss();
			Toast.makeText(mContext, "Es wurden " + result + " Datensätze vom Server geladen.", Toast.LENGTH_SHORT).show();
			fireRepositoryUpdate();

		}
	}


	private class WriteData extends AsyncTask<Void, Void, Integer>
	{
		protected Integer doInBackground(Void... params)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String defaultIP = prefs.getString("default_url", "none");
			String port = prefs.getString("default_port", "none");
			String username = prefs.getString("auth_user", "none");
			String password = prefs.getString("auth_password", "none");

			int counter = 0;
			for (Catalog c : mCatalogs) 
			{
//				if (c.isManipulated()) 
//				{
					try 
					{
						writeChangesInCatalogToOdata(defaultIP, port, username, password, "OpenResKitHub", "Catalogs", c);
					} 
					catch (final Exception e) 
					{
						mContext.runOnUiThread(new Runnable() 
						{

							public void run() {
								Toast.makeText(mContext, "Es ist ein Fehler aufgetreten. " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();

							}
						});
						e.printStackTrace();
					}
					c.setManipulated(false);
					counter++;
//				}
			}
			return counter;

		}

		protected void onPreExecute()
		{
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage("Schreibe Daten");
			mProgressDialog.show();
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			mProgressDialog.dismiss();
			Toast.makeText(mContext, result + " Kataloge wurden aktualisiert.", Toast.LENGTH_SHORT).show();
		}

	}
	private static String serializeCatalogs(List<Catalog> catalogs) 
	{
		String str = null;
		try 
		{
			str = mObjectMapper.writerWithType(new TypeReference<List<Catalog>>(){}).writeValueAsString(catalogs);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}

	private static String serializeResponsibleSubjects(List<ResponsibleSubject> responsibleSubjects) 
	{
		String str = null;
		try 
		{
			str = mObjectMapper.writerWithType(new TypeReference<List<ResponsibleSubject>>(){}).writeValueAsString(responsibleSubjects);
		} 
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
		return str;
	}

	private JSONObject getJSONObjectFromOdata(String ip, String port, String username, String password, String endpoint, String collection, String expand) throws Exception
	{
		JSONObject returnJSONObject = null;
		String jsonText = null;
		String uriString = null;
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
			httpParams.setBooleanParameter("http.protocol.expect-continue", false);
			HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
			if (expand == null) 
			{
				uriString = "http://"+ip+":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json";
			}
			else 
			{
				uriString = "http://"+ip+":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$expand=" + expand;
			}

			HttpGet request = new HttpGet(uriString);
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
			HttpClient httpClient = new DefaultHttpClient(httpParams);

			HttpResponse response = httpClient.execute(request);
			if(response.getStatusLine().getStatusCode() == 200){
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream instream = entity.getContent();
					jsonText = convertStreamToString(instream);
					instream.close();
				}
				returnJSONObject  = new JSONObject(jsonText);
			}
			else if (response.getStatusLine().getStatusCode() == 403) 
			{
				Exception e1 = new AuthenticationException("Der Benutzername oder das Passwort für die Authentifizierung am OData Service sind nicht korrekt");
				throw e1; 
			}
		}
		catch (Exception e) 
		{
			throw e;
		}
		return returnJSONObject;
	}

	private JSONArray getJSONArrayFromOdata(String ip, String port, String username, String password, String endpoint, String collection, String expand, String filter) throws Exception
	{
		JSONArray returnJSONArray = null;
		String jsonText = null;
		String uriString = null;
		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
			httpParams.setBooleanParameter("http.protocol.expect-continue", false);
			HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
			if (filter == null) 
			{
				if (expand == null) 
				{
					uriString = "http://"+ip+":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json";
				}
				else 
				{
					uriString = "http://"+ip+":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$expand=" + expand;
				}
			} 
			else
			{
				if (expand == null) 
				{
					uriString = "http://"+ ip +":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$filter="+ filter;
				}
				else
				{
					uriString = "http://"+ ip +":"+ port +"/" + endpoint +"/"+ collection +"/?$format=json&$expand=" + expand + "&$filter="+ filter;
				}
			}
			HttpGet request = new HttpGet(uriString);
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");
			request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
			HttpClient httpClient = new DefaultHttpClient(httpParams);

			HttpResponse response = httpClient.execute(request);
			if(response.getStatusLine().getStatusCode() == 200){
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream instream = entity.getContent();
					jsonText = convertStreamToString(instream);
					instream.close();
				}
				returnJSONArray  = new JSONObject(jsonText).getJSONArray("value");
			}
			else if (response.getStatusLine().getStatusCode() == 403) 
			{
				Exception e1 = new AuthenticationException("Der Benutzername oder das Passwort für die Authentifizierung am OData Service sind nicht korrekt");
				throw e1; 
			}
		}
		catch (Exception e) 
		{
			throw e;
		}
		return returnJSONArray;
	}


	private static void writeChangesInCatalogToOdata(String ip, String port, String username, String password, String endpoint, String collection, Catalog catalog) throws Exception 
	{


		JSONArray catalogMeasures = new JSONArray();
		if (catalog.getMeasures() != null)
		{
			for (Measure m : catalog.getMeasures()) 
			{
				if (m.isManipulated()) 
				{
					writeChangesInMeasureToOdata(ip, port, username, password, "OpenResKitHub", "Measures", m);			
				}
			}
		}

		if (catalog.getId() == 0) 
		{
			try 
			{
				HttpResponse response;
				HttpParams httpParams = new BasicHttpParams();
				HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
				httpParams.setBooleanParameter("http.protocol.expect-continue", false);
				DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
				if (catalog.getMeasures() != null) 
				{
					for (Measure m : catalog.getMeasures()) 
					{
						JSONObject measureNavProp = new JSONObject();
						measureNavProp.put("uri", "http://" + ip + ":" + port + "/" + endpoint + "/Measures("+  m.getInternalId() +")");
						JSONObject measureNavPropMetadata = new JSONObject();
						measureNavPropMetadata.put("__metadata", measureNavProp);
						catalogMeasures.put(measureNavPropMetadata);
					}
				}

				JSONObject catalogJO = new JSONObject();
				catalogJO.put("odata.type", "OpenResKit.DomainModel.Catalog");
				catalogJO.put("Name", catalog.getName());
				catalogJO.put("Description", catalog.getDescription());
				catalogJO.put("Measures", catalogMeasures);

				StringEntity stringEntity = new StringEntity(catalogJO.toString(),HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + collection);
				request.setHeader("X-HTTP-Method-Override", "PUT");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();
				if(responseEntity != null) {
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}
		//existing catalog
		else
		{
			try 
			{
				HttpResponse response;
				HttpParams httpParams = new BasicHttpParams();
				HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
				httpParams.setBooleanParameter("http.protocol.expect-continue", false);
				DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
				if (catalog.getMeasures() != null || catalog.getMeasures().size() > 0) 
				{
					for (Measure m : catalog.getMeasures()) 
					{

						JSONObject measureNavProp = new JSONObject();
						measureNavProp.put("uri", "http://" + ip + ":" + port + "/" + endpoint + "/Measures("+  m.getInternalId() +")");
						JSONObject measureNavPropMetadata = new JSONObject();
						measureNavPropMetadata.put("__metadata", measureNavProp);
						catalogMeasures.put(measureNavPropMetadata);
					}
				}

				JSONObject catalogJO = new JSONObject();
				catalogJO.put("odata.type", "OpenResKit.DomainModel.Catalog");
				catalogJO.put("Id", catalog.getInternalId());
				catalogJO.put("Measures", catalogMeasures);

				StringEntity stringEntity = new StringEntity(catalogJO.toString(),HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + collection+"(" + catalog.getInternalId() +")");
				request.setHeader("X-HTTP-Method", "MERGE");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();
				if(responseEntity != null) {
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}
	}

	private static void writeChangesInMeasureToOdata(String ip, String port, String username, String password, String endpoint, String collection, Measure measure) throws Exception 
	{

		MeasureImageSource newImageSource = null;
		Measure newMeasure = null;
		//if new image
		if (measure.getImageSource() != null && measure.getId() == 0) 
		{
			try 
			{
				HttpResponse response;
				HttpParams httpParams = new BasicHttpParams();
				HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
				httpParams.setBooleanParameter("http.protocol.expect-continue", false);
				DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

				JSONObject imageJO = new JSONObject();
				imageJO.put("odata.type", "OpenResKit.DomainModel.ImageSource");
				imageJO.put("BinarySource", measure.getImageSource().getBinarySource());
				StringEntity stringEntity = new StringEntity(imageJO.toString(),HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/MeasureImageSources");
				request.setHeader("X-HTTP-Method-Override", "PUT");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();


				if(responseEntity != null) 
				{
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
					newImageSource = mObjectMapper.readValue(answer.toString(), MeasureImageSource.class);
					measure.getImageSource().setInternalId(newImageSource.getId());
					measure.getImageSource().setId(newImageSource.getId());
				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}


		//if new measure
		if (measure.getId() == 0) 
		{
			try 
			{
				HttpResponse response;
				HttpParams httpParams = new BasicHttpParams();
				HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
				httpParams.setBooleanParameter("http.protocol.expect-continue", false);
				DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

				JSONObject measureJO = new JSONObject();
				measureJO.put("odata.type", "OpenResKit.DomainModel.Measure");
				measureJO.put("Name", measure.getName());
				measureJO.put("Description", measure.getDescription());
				measureJO.put("Evaluation", measure.getEvalutation());
				measureJO.put("EvaluationRating", measure.getEvaluationRating());
				if (measure.getEntryDate() != null) 
				{
					measureJO.put("EntryDate", mOdataDataFormatter.format(measure.getEntryDate()));
				}
				if (measure.getDueDate() != null) 
				{
					measureJO.put("DueDate", mOdataDataFormatter.format(measure.getDueDate()));
				}
				if (measure.getCreationDate() != null) 
				{
					measureJO.put("CreationDate", mOdataDataFormatter.format(measure.getCreationDate()));
				}

				JSONObject respSubjNavProp = new JSONObject();
				respSubjNavProp.put("uri", "http://" + ip + ":" + port + "/" + endpoint + "/ResponsibleSubjects("+ measure.getResponsibleSubject().getId() +")");
				JSONObject respSubjNavPropMetadata = new JSONObject();
				respSubjNavPropMetadata.put("__metadata", respSubjNavProp);
				measureJO.put("ResponsibleSubject", respSubjNavPropMetadata);
				measureJO.put("Status", measure.getStatus());
				measureJO.put("Priority", measure.getPriority());
				if (newImageSource != null) 
				{

					JSONObject imageSourceNavProp = new JSONObject();
					imageSourceNavProp.put("uri", "http://" + ip + ":" + port + "/" + endpoint + "/MeasureImageSources("+ measure.getImageSource().getId() +")");
					JSONObject imageSourceNavPropMetadata = new JSONObject();
					imageSourceNavPropMetadata.put("__metadata", imageSourceNavProp);
					measureJO.put("MeasureImageSource", imageSourceNavPropMetadata);
				}
				StringEntity stringEntity = new StringEntity(measureJO.toString(),HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + collection);
				request.setHeader("X-HTTP-Method-Override", "PUT");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();


				if(responseEntity != null) 
				{
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
					newMeasure = mObjectMapper.readValue(answer.toString(), Measure.class);
					measure.setInternalId(newMeasure.getId());
					measure.setId(newMeasure.getId());

				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}
		//manipulated measure
		else
		{
			try 
			{
				HttpResponse response;
				HttpParams httpParams = new BasicHttpParams();
				HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
				HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
				httpParams.setBooleanParameter("http.protocol.expect-continue", false);
				DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);

				JSONObject measureJO = new JSONObject();
				measureJO.put("odata.type", "OpenResKit.DomainModel.Measure");
				measureJO.put("Id", measure.getId());
				measureJO.put("Evaluation", measure.getEvalutation());
				measureJO.put("EvaluationRating", measure.getEvaluationRating());
				measureJO.put("EntryDate", mOdataDataFormatter.format(measure.getEntryDate()));
				measureJO.put("Status", measure.getStatus());

				StringEntity stringEntity = new StringEntity(measureJO.toString(),HTTP.UTF_8);
				stringEntity.setContentType("application/json");

				HttpPost request = null;
				request = new HttpPost("http://" + ip + ":" + port + "/" + endpoint + "/" + collection + "("+ measure.getInternalId()+ ")");
				request.setHeader("X-HTTP-Method", "MERGE");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json;odata=verbose");
				request.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(username, password), "UTF-8", false));
				request.setEntity(stringEntity);

				response = httpClient.execute(request);
				HttpEntity responseEntity = response.getEntity();

				if(responseEntity != null) 
				{
					String jsonText = EntityUtils.toString(responseEntity, HTTP.UTF_8);
					JSONObject answer = new JSONObject(jsonText);
					System.out.print(answer);
				}
			}
			catch (Exception e)
			{
				throw e;
			}
		}
		measure.setManipulated(false);
	}

	public void deleteLocalData()
	{
		mCatalogs = new ArrayList<Catalog>();
		new SaveCatalogsToFileTask().execute((mCatalogs));	
		mResponsibleSubjects = new ArrayList<ResponsibleSubject>();
		saveResponsibleSubjectToFile(mResponsibleSubjects);
		mMeasures = new ArrayList<Measure>();
		mImages = new ArrayList<MeasureImageSource>();
		fireRepositoryUpdate();
	}

	private void saveToExternal(String content, String fileName) {
		FileOutputStream fos = null;
		Writer out = null;
		try {
			File file = new File(getAppRootDir(), fileName);
			fos = new FileOutputStream(file);
			out = new OutputStreamWriter(fos, "UTF-8");

			out.write(content);
			out.flush();
		} catch (Throwable e){
			e.printStackTrace();
		} finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException ignored) {}
			}
			if(out!= null){
				try {
					out.close();
				} catch (IOException ignored) {}
			}
		}
	}

	private String loadFromExternal(String fileName) {
		String res = null;
		File file = new File(getAppRootDir(), fileName);
		if(!file.exists()){
			Log.e("", "file " +file.getAbsolutePath()+ " not found");
			return null;
		}
		FileInputStream fis = null;
		BufferedReader inputReader = null;
		try {
			fis = new FileInputStream(file);
			inputReader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			StringBuilder strBuilder = new StringBuilder();
			String line;
			while ((line = inputReader.readLine()) != null) {
				strBuilder.append(line + "\n");
			}
			res = strBuilder.toString();
		} catch(Throwable e){
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException ignored) {}
			}
			if(inputReader!= null){
				try {
					inputReader.close();
				} catch (IOException ignored) {}
			}
		}
		return res;
	}

	public File getAppRootDir() {
		File appRootDir;
		boolean externalStorageAvailable;
		boolean externalStorageWriteable;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			externalStorageAvailable = externalStorageWriteable = false;
		}
		if (externalStorageAvailable && externalStorageWriteable) {

			appRootDir = mContext.getExternalFilesDir(null);
		} else {
			appRootDir = mContext.getDir("appRootDir", Context.MODE_PRIVATE);
		}
		if (!appRootDir.exists()) {
			appRootDir.mkdir();
		}
		return appRootDir;
	}

	private static String convertStreamToString(InputStream is) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
}
