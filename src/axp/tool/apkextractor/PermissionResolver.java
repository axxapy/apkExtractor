package axp.tool.apkextractor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PermissionResolver {
	private static final int REQUEST_CODE = 100500;

	private static final String[] DEFAULT_PERMISSIONS = new String[] {
		"android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
		"android.permission.ACCESS_NETWORK_STATE",
		"android.permission.ACCESS_NOTIFICATION_POLICY",
		"android.permission.ACCESS_WIFI_STATE",
		"android.permission.ACCESS_WIMAX_STATE",
		"android.permission.BLUETOOTH",
		"android.permission.BLUETOOTH_ADMIN",
		"android.permission.BROADCAST_STICKY",
		"android.permission.CHANGE_NETWORK_STATE",
		"android.permission.CHANGE_WIFI_MULTICAST_STATE",
		"android.permission.CHANGE_WIFI_STATE",
		"android.permission.CHANGE_WIMAX_STATE",
		"android.permission.DISABLE_KEYGUARD",
		"android.permission.EXPAND_STATUS_BAR",
		"android.permission.FLASHLIGHT",
		"android.permission.GET_ACCOUNTS",
		"android.permission.GET_PACKAGE_SIZE",
		"android.permission.INTERNET",
		"android.permission.KILL_BACKGROUND_PROCESSES",
		"android.permission.MODIFY_AUDIO_SETTINGS",
		"android.permission.NFC",
		"android.permission.READ_SYNC_SETTINGS",
		"android.permission.READ_SYNC_STATS",
		"android.permission.RECEIVE_BOOT_COMPLETED",
		"android.permission.REORDER_TASKS",
		"android.permission.REQUEST_INSTALL_PACKAGES",
		"android.permission.SET_TIME_ZONE",
		"android.permission.SET_WALLPAPER",
		"android.permission.SET_WALLPAPER_HINTS",
		"android.permission.SUBSCRIBED_FEEDS_READ",
		"android.permission.TRANSMIT_IR",
		"android.permission.USE_FINGERPRINT",
		"android.permission.VIBRATE",
		"android.permission.WAKE_LOCK",
		"android.permission.WRITE_SYNC_SETTINGS",
		"com.android.alarm.permission.SET_ALARM",
		"com.android.launcher.permission.INSTALL_SHORTCUT",
		"com.android.launcher.permission.UNINSTALL_SHORTCUT",
		"android.permission.ACCESS_SUPERUSER"
	};

	private Activity activity;

	public PermissionResolver(Activity a) {
		this.activity = a;
	}

	public boolean resolve() {
		if (Build.VERSION.SDK_INT < 23) return true;

		String[] unmet_permissions = getUnmetPermissions();
		if (unmet_permissions.length < 1) return true;

		activity.requestPermissions(unmet_permissions, REQUEST_CODE);

		return false;
	}

	@SuppressLint("NewApi")
	private String[] getUnmetPermissions() {
		List<String> unmet_permissions = new LinkedList<String>();
		try {
			List<String> def  = Arrays.asList(DEFAULT_PERMISSIONS);
			PackageInfo  info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
			for (String perm : info.requestedPermissions) {
				if (def.contains(perm)) continue;
				if (activity.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) continue;
				unmet_permissions.add(perm);
			}
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			return new String[0];
		}

		if (unmet_permissions.size() < 1) return new String[0];

		String[] arr = new String[unmet_permissions.size()];
		unmet_permissions.toArray(arr);
		return arr;
	}

	public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode != REQUEST_CODE) return false;

		boolean granted = true;
		for (int result : grantResults) {
			if (result != PackageManager.PERMISSION_GRANTED) {
				granted = false;
				break;
			}
		}

		if (granted) return true;

		showPermissionDialog();

		return true;
	}

	private void showPermissionDialog() {
		new AlertDialog.Builder(activity)
			.setMessage(R.string.alert_perm_body)
			.setTitle(R.string.alert_perm_title)
			.setPositiveButton(R.string.alert_perm_btn, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int i) {
					activity.startActivity(new Intent()
						.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
						.addCategory(Intent.CATEGORY_DEFAULT)
						.setData(Uri.parse("package:" + activity.getApplicationContext().getPackageName()))
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
						.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
					);
				}
			})
			.create()
			.show();
	}
}
