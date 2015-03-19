package axp.tool.apkextractor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends ActionBarActivity {
	private ApkListAdapter apkListAdapter;

	private ProgressBar progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

		RecyclerView listView = (RecyclerView)findViewById(android.R.id.list);

		apkListAdapter = new ApkListAdapter(this);
		listView.setLayoutManager(new LinearLayoutManager(this));
		listView.setAdapter(apkListAdapter);

		progressBar = (ProgressBar) findViewById(android.R.id.progress);
		progressBar.setVisibility(View.VISIBLE);

		new Loader(this).execute();
	}

	public void hideProgressBar() {
		progressBar.setVisibility(View.GONE);
	}

	public void addItem(ApplicationInfo item) {
		apkListAdapter.addItem(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
		final SearchView searchView = (SearchView)MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean queryTextFocused) {
				if (!queryTextFocused && searchView.getQuery().length() < 1) {
					getSupportActionBar().collapseActionView();
				}
			}
		});
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String s) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String s) {
				apkListAdapter.setSearchPattern(s);
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	public void doExctract(final ApplicationInfo info) {
		final Extractor extractor = new Extractor();
		try {
			String dst = extractor.extractWithoutRoot(info);
			Toast.makeText(this, String.format(this.getString(R.string.toast_extracted), dst), Toast.LENGTH_SHORT).show();
			return;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		new AlertDialog.Builder(this)
			.setTitle(R.string.alert_root_title)
			.setMessage(R.string.alert_root_body)
			.setPositiveButton(R.string.alert_root_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						String dst = extractor.extractWithRoot(info);
						Toast.makeText(MainActivity.this, String.format(MainActivity.this.getString(R.string.toast_extracted), dst), Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText(MainActivity.this, R.string.toast_failed, Toast.LENGTH_SHORT).show();
					}
				}
			}).setNegativeButton(R.string.alert_root_no, null)
			.show();
	}

	class Loader extends AsyncTask<Void, ApplicationInfo, Void> {
		ProgressDialog dialog;
		MainActivity   mainActivity;

		public Loader(MainActivity a) {
			dialog = ProgressDialog.show(a, getString(R.string.dlg_loading_title), getString(R.string.dlg_loading_body));
			mainActivity = a;
		}

		@Override
		protected Void doInBackground(Void... params) {
			List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
			for (ApplicationInfo packageInfo : packages) {
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
