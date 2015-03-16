package axp.tool.apkextractor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Extractor {
	private Activity mActivity;

	public Extractor(Activity activity) {
		this.mActivity = activity;
	}

	public void extract(final ApplicationInfo info) {
		final File src = new File(info.sourceDir);

		try {
			File dst = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "apk/" + src.getName());
			copy(src, dst);
			Toast.makeText(mActivity, String.format(mActivity.getString(R.string.toast_extracted), dst.getAbsolutePath()), Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
			new AlertDialog.Builder(mActivity)
				.setTitle(R.string.alert_root_title)
				.setMessage(R.string.alert_root_body)
				.setPositiveButton(R.string.alert_root_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							String path = System.getenv("EXTERNAL_STORAGE") + "/Download/apk/" + info.packageName + ".apk";
							new File(path).getParentFile().mkdirs();
							//Log.d("AXP", "su -c cat " + src.getAbsolutePath() + " > " + path);
							Process p = Runtime.getRuntime().exec("su -c cat " + src.getAbsolutePath() + " > " + path);
							try {
								p.waitFor();
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
							if (p.exitValue() == 0) {
								Toast.makeText(mActivity, String.format(mActivity.getString(R.string.toast_extracted), path), Toast.LENGTH_SHORT).show();
								return;
							}

							/*BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
							String line = "";
							while ((line = reader.readLine())!= null) {
								Log.d("AXP", line);
								//output.append(line + "\n");
							}*/
						} catch (IOException e) {
							e.printStackTrace();
						}
						Toast.makeText(mActivity, R.string.toast_failed, Toast.LENGTH_SHORT).show();
					}
				}).setNegativeButton(R.string.alert_root_no, null).show();
		}
	}

	private void copy(File src, File dst) throws IOException {
		dst.getParentFile().mkdirs();
		FileInputStream inStream = new FileInputStream(src);
		FileOutputStream outStream = new FileOutputStream(dst);
		FileChannel inChannel = inStream.getChannel();
		FileChannel outChannel = outStream.getChannel();
		inChannel.transferTo(0, inChannel.size(), outChannel);
		inStream.close();
		outStream.close();
	}
}
