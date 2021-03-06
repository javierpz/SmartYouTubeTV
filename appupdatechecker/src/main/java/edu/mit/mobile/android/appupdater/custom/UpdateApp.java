package edu.mit.mobile.android.appupdater.custom;

import android.content.*;
import android.net.*;
import android.os.*;
import android.os.Build.VERSION;
import android.support.v4.content.FileProvider;
import android.util.*;
import android.webkit.*;
import android.widget.Toast;
import edu.mit.mobile.android.appupdater.R;

import java.io.*;
import java.net.*;

/**
 * Usage:
 * <pre>
 *   atualizaApp = new UpdateApp(ctx);
 *   atualizaApp.execute("http://serverurl/appfile.apk");
 * </pre>
 */
public class UpdateApp extends AsyncTask<String,Void,Void> {
    private Context mContext;

    public UpdateApp(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... arg0) {
        String packagePath = arg0[0];
        if (URLUtil.isValidUrl(packagePath)) {
            packagePath = downloadPackage(packagePath);
        }
        installPackage(packagePath);
        return null;
    }

    private String downloadPackage(String uri) {
        // NOTE: Android 6.0 fix
        File cacheDir = mContext.getExternalCacheDir();
        if (cacheDir == null) { // try to use SDCard
            cacheDir = Environment.getExternalStorageDirectory();
        }
        File outputFile = new File(cacheDir, "update.apk");
        try {
            URL url = new URL(uri);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(false);
            c.connect();

            if (outputFile.exists()) {
                outputFile.delete();
            }
            FileOutputStream fos = new FileOutputStream(outputFile);

            InputStream is = c.getInputStream();

            byte[] buffer = new byte[1024];
            int len1 = 0;
            while ((len1 = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len1);
            }
            fos.close();
            is.close();

        } catch (IOException e) {
            Log.e("UpdateAPP", e.toString());
            throw new IllegalStateException(e);
        }
        return outputFile.getAbsolutePath();
    }

    private void noExternalStorageError() {
        Toast.makeText(mContext, R.string.no_external_storage_error, Toast.LENGTH_LONG).show();
    }

    // NOTE: as of Oreo you must also add the REQUEST_INSTALL_PACKAGES permission to your manifest. Otherwise it just silently fails
    private void installPackage(String packagePath) {
        if (packagePath == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri file = getFileUri(packagePath);
        intent.setDataAndType(file, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION); // without this flag android returned a intent error!
        mContext.startActivity(intent);
    }

    private Uri getFileUri(String packagePath) {
        // if your targetSdkVersion is 24 or higher, we have to use FileProvider class
        // https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
        if (VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".update_provider", new File(packagePath));
        } else {
            return Uri.fromFile(new File(packagePath));
        }
    }
}
