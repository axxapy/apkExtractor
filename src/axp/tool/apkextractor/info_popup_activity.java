package axp.tool.apkextractor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class info_popup_activity extends AppCompatActivity {

    TextView about_app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_popup);
        PackageInfo info = getIntent().getExtras().getParcelable("package_info");


//        TextView packageName = findViewById(R.id.packageName);
//        packageName.setText("package name: " + info.packageName);
        about_app = findViewById(R.id.about_app);
//        about_app.setText("package name: " + info.packageName + "\n" + "version name: " + info.versionName + "\n" + "version code: " + info.versionCode);
//        about_app.append("\n");
        addToDescription("package name: " + info.packageName);
        addToDescription("version name: " + info.versionName);
        addToDescription("version code: " + info.versionCode);

    }
    public void addToDescription(String text) {
        about_app.append(text);
        about_app.append("\n");
    }
}