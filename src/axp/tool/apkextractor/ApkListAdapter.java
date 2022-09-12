package axp.tool.apkextractor;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ApkListAdapter extends RecyclerView.Adapter<ApkListAdapter.ViewHolder> {
    public final PackageManager packageManager;
    public MainActivity mActivity;
    int names_to_load = 0;
    private ThreadFactory tFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    };
    private ArrayList<PackageInfo> list = new ArrayList<PackageInfo>();
    private ArrayList<PackageInfo> list_original = new ArrayList<PackageInfo>();
    private ExecutorService executorServiceNames = Executors.newFixedThreadPool(3, tFactory);
    private ExecutorService executorServiceIcons = Executors.newFixedThreadPool(3, tFactory);
    private Handler handler = new Handler();
    private Map<String, String> cache_appName = Collections.synchronizedMap(new LinkedHashMap<String, String>(10, 1.5f, true));
    private Map<String, Drawable> cache_appIcon = Collections.synchronizedMap(new LinkedHashMap<String, Drawable>(10, 1.5f, true));

    private String search_pattern;

    public ApkListAdapter(MainActivity activity) {
        this.packageManager = activity.getPackageManager();
        mActivity = activity;


    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false), this);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int i) {
        PackageInfo item = list.get(i);
        holder.setPackageName(item.packageName, search_pattern);
        if (cache_appIcon.containsKey(item.packageName) && cache_appName.containsKey(item.packageName)) {
            holder.setAppName(cache_appName.get(item.packageName), search_pattern);
            holder.imgIcon.setImageDrawable(cache_appIcon.get(item.packageName));
        } else {
            holder.setAppName(item.packageName, search_pattern);
            holder.imgIcon.setImageDrawable(null);
            executorServiceIcons.submit(new GuiLoader(holder, item));
        }
    }

    public PackageInfo getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void addItem(PackageInfo item) {
        names_to_load++;
        executorServiceNames.submit(new AppNameLoader(item));
        list_original.add(item);
        filterListByPattern();
        notifyDataSetChanged();
    }

    public void setSearchPattern(String pattern) {
        search_pattern = pattern.toLowerCase();
        filterListByPattern();
        this.notifyDataSetChanged();
    }

    private void filterListByPattern() {
        list.clear();
        for (PackageInfo info : list_original) {
            boolean add = true;
            do {
                if (search_pattern == null || search_pattern.isEmpty()) {
                    break;// empty search pattern: add everything
                }
                if (info.packageName.toLowerCase().contains(search_pattern)) {
                    break;// search in package name
                }
                if (cache_appName.containsKey(info.packageName) && cache_appName.get(info.packageName).toLowerCase().contains(search_pattern)) {
                    break;// search in application name
                }
                add = false;
            } while (false);

            if (add) list.add(info);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener {
        public ImageView imgIcon;
        TextView about_app;
        Button extractButton;
        BottomSheetDialog bottomSheetDialog;
        View bottomSheetView;
        private ApkListAdapter adapter;
        private TextView txtPackageName;
        private TextView txtAppName;

        public ViewHolder(View v, ApkListAdapter adapter) {
            super(v);
            this.adapter = adapter;
            txtPackageName = (TextView) v.findViewById(R.id.txtPackageName);
            imgIcon = (ImageView) v.findViewById(R.id.imgIcon);
            txtAppName = (TextView) v.findViewById(R.id.txtAppName);
            v.setOnClickListener(this);

            bottomSheetDialog = new BottomSheetDialog(
                    v.getContext());
            bottomSheetView = LayoutInflater.from(v.getContext().getApplicationContext())
                    .inflate(R.layout.bottom_sheet, v.findViewById(R.id.bottomSheet));
        }

        @Override
        public void onClick(View v) {
            PackageInfo info = adapter.getItem(getAdapterPosition());
            //adapter.mActivity.doExctract(info);
            //Intent i = new Intent(v.getContext(), info_popup_activity.class);
            //i.putExtra("package_info",info);
            //v.getContext().startActivity(i);



            about_app = bottomSheetView.findViewById(R.id.about_app);
            TextView appNameText = bottomSheetView.findViewById(R.id.appName);

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy, hh:mm:ss");
            about_app.setText("");
            appNameText.setText(info.applicationInfo.loadLabel(adapter.packageManager));
            addToDescription("package name: " + info.packageName);
            addToDescription("version name: " + info.versionName);
            addToDescription("first installed: " + formatter.format(new Date(info.firstInstallTime)));
            addToDescription("last updated: " + formatter.format(new Date(info.lastUpdateTime)));


            extractButton = bottomSheetView.findViewById(R.id.extractButton);
            extractButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.mActivity.doExctract(info);
                }
            });


            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();

        }

        public void addToDescription(String text) {
            about_app.append(text);
            about_app.append("\n");
        }

        public void setAppName(String name, String highlight) {
            setAndHighlight(txtAppName, name, highlight);
        }

        public void setPackageName(String name, String highlight) {
            setAndHighlight(txtPackageName, name, highlight);
        }

        private void setAndHighlight(TextView view, String value, String pattern) {
            view.setText(value);
            if (pattern == null || pattern.isEmpty()) return;// nothing to highlight

            value = value.toLowerCase();
            for (int offset = 0, index = value.indexOf(pattern, offset); index >= 0 && offset < value.length(); index = value.indexOf(pattern, offset)) {
                Spannable span = new SpannableString(view.getText());
                span.setSpan(new ForegroundColorSpan(Color.BLUE), index, index + pattern.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                view.setText(span);
                offset += index + pattern.length();
            }
        }
    }

    class AppNameLoader implements Runnable {
        private PackageInfo package_info;

        public AppNameLoader(PackageInfo info) {
            package_info = info;
        }

        @Override
        public void run() {
            cache_appName.put(package_info.packageName, (String) package_info.applicationInfo.loadLabel(packageManager));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    names_to_load--;
                    if (names_to_load == 0) {
                        mActivity.hideProgressBar();
                        executorServiceNames.shutdown();
                    }
                }
            });
        }
    }

    class GuiLoader implements Runnable {
        private ViewHolder viewHolder;
        private PackageInfo package_info;

        public GuiLoader(ViewHolder h, PackageInfo info) {
            viewHolder = h;
            package_info = info;
        }

        @Override
        public void run() {
            boolean first = true;
            do {
                try {
                    final String appName = cache_appName.containsKey(package_info.packageName)
                            ? cache_appName.get(package_info.packageName)
                            : (String) package_info.applicationInfo.loadLabel(packageManager);
                    final Drawable icon = package_info.applicationInfo.loadIcon(packageManager);
                    cache_appName.put(package_info.packageName, appName);
                    cache_appIcon.put(package_info.packageName, icon);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            viewHolder.setAppName(appName, search_pattern);
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
}