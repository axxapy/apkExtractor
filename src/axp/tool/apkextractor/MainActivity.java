package axp.tool.apkextractor;

import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import java.util.List;

public class MainActivity extends ActionBarActivity {
	ApkList adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

		RecyclerView listView = (RecyclerView)findViewById(android.R.id.list);

		adapter = new ApkList(getPackageManager());
		listView.setLayoutManager(new LinearLayoutManager(this));
		listView.setAdapter(adapter);

		new Loader(this).execute();
	}

	public void addItem(ApplicationInfo item) {
		adapter.addItem(item);
	}

	class Loader extends AsyncTask<Void, ApplicationInfo, Void> {
		ProgressDialog dialog;
		MainActivity   mainActivity;

		public Loader(MainActivity a) {
			dialog = ProgressDialog.show(a, "Loading", "Loading list of installed applications...");
			mainActivity = a;
		}

		@Override
		protected Void doInBackground(Void... params) {
			final PackageManager pm = getPackageManager();

			List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

			for (ApplicationInfo packageInfo : packages) {
				//Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
				//Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
				publishProgress(packageInfo);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(ApplicationInfo... values) {
			super.onProgressUpdate(values);
			mainActivity.addItem(values[0]);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			dialog.dismiss();
		}
	}
}
