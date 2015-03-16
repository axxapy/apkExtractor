package axp.tool.apkextractor;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkList extends RecyclerView.Adapter<ApkList.ViewHolder> {
	private ArrayList<ApplicationInfo> list            = new ArrayList<ApplicationInfo>();
	private ExecutorService            executorService = Executors.newFixedThreadPool(5);
	private Handler                    handler         = new Handler();
	private final PackageManager pm;

	private Map<String, String>   cache_appName = Collections.synchronizedMap(new LinkedHashMap<String, String>(10, 1.5f, true));
	private Map<String, Drawable> cache_appIcon = Collections.synchronizedMap(new LinkedHashMap<String, Drawable>(10, 1.5f, true));

	public ApkList(final PackageManager pm) {
		this.pm = pm;
	}

	class InfoLoader implements Runnable {
		private       ViewHolder      viewHolder;
		private       ApplicationInfo applicationInfo;
		final private PackageManager  packageManager;

		public InfoLoader(final PackageManager pm, ViewHolder h, ApplicationInfo info) {
			packageManager = pm;
			viewHolder = h;
			applicationInfo = info;
		}

		@Override
		public void run() {
			boolean first = true;
			do {
				try {
					final String appName = (String)applicationInfo.loadLabel(packageManager);
					final Drawable icon = applicationInfo.loadIcon(packageManager);
					cache_appName.put(applicationInfo.packageName, appName);
					cache_appIcon.put(applicationInfo.packageName, icon);
					handler.post(new Runnable() {
						@Override
						public void run() {
							viewHolder.txtAppName.setText(appName);
							viewHolder.imgIcon.setImageDrawable(icon);
						}
					});


				} catch (OutOfMemoryError ex) {
					cache_appIcon.clear();
					cache_appName.clear();
					if (first) {
						first = false;
						continue;
					}
				}
				break;
			} while (true);
		}
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		public TextView  txtPackageName;
		public ImageView imgIcon;
		public TextView  txtAppName;

		public ViewHolder(View v) {
			super(v);
			txtPackageName = (TextView)v.findViewById(R.id.txtPackageName);
			imgIcon = (ImageView)v.findViewById(R.id.imgIcon);
			txtAppName = (TextView)v.findViewById(R.id.txtAppName);
		}
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
		return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false));
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int i) {
		ApplicationInfo item = list.get(i);
		holder.txtPackageName.setText(item.packageName);
		if (cache_appIcon.containsKey(item.packageName) && cache_appName.containsKey(item.packageName)) {
			holder.txtAppName.setText(cache_appName.get(item.packageName));
			holder.imgIcon.setImageDrawable(cache_appIcon.get(item.packageName));
		} else {
			holder.txtAppName.setText(item.packageName);
			holder.imgIcon.setImageDrawable(null);
			executorService.submit(new InfoLoader(pm, holder, item));
		}
	}

	@Override
	public int getItemCount() {
		return list.size();
	}

	public void addItem(ApplicationInfo item) {
		list.add(item);
		notifyDataSetChanged();
	}
}