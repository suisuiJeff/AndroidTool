package com.njking.tool;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * App详情信息页
 * 功能：
 * 1. 分享应用
 * 2. 查看签名
 * 属性：
 * 安装时间
 * 版本号
 * 版本名称
 * 应用名称
 * 包名
 *
 * @author
 * @date 2018/10/29 11:48
 */
public class AppDetailActivity extends AppCompatActivity implements View.OnClickListener {

    private String appName;

    private String packageName;

    private TextView tvAppName;
    private TextView tvPackageName;
    private TextView tvVersionName;
    private TextView tvVersionCode;
    private TextView tvFirstInstalledTime;
    private TextView tvLastUpdateTime;
    private TextView tvAppSize;
    private TextView tvAppLocation;
    private TextView tvPermissions;

    private TextView tvMD5;

    private TextView tvSHA1;

    private Context mContext;

    public static Intent createIntent(Context context, String packageName, String appName) {
        Intent intent = new Intent(context, AppDetailActivity.class);
        intent.putExtra("packageName", packageName);
        intent.putExtra("appName", appName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);
        mContext = this;

        packageName = getIntent().getStringExtra("packageName");
        appName = getIntent().getStringExtra("appName");

        getSupportActionBar().setTitle(appName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        tvAppName = findViewById(R.id.tvAppName);
        tvPackageName = findViewById(R.id.tvPackageName);
        tvVersionName = findViewById(R.id.tvVersionName);
        tvVersionCode = findViewById(R.id.tvVersionCode);
        tvFirstInstalledTime = findViewById(R.id.tvFirstInstalledTime);
        tvLastUpdateTime = findViewById(R.id.tvLastUpdateTime);
        tvAppSize = findViewById(R.id.tvAppSize);
        tvAppLocation = findViewById(R.id.tvAppLocation);
        tvPermissions = findViewById(R.id.tvPermissions);
        tvMD5 = findViewById(R.id.tvMD5);
        tvSHA1 = findViewById(R.id.tvSHA1);
        String MD5signature = PackageUtils.getAppMD5Signature(mContext, packageName);
        tvMD5.setText("应用M D 5：" + MD5signature);
        tvMD5.setTag(MD5signature);
        String sha1Signature = PackageUtils.getAppSHA1Signature(mContext, packageName);
        tvSHA1.setText("应用SHA1：" + sha1Signature);
        tvSHA1.setTag(sha1Signature);

        findViewById(R.id.btnCopyMD5).setOnClickListener(this);
        findViewById(R.id.btnCopyMD5Without).setOnClickListener(this);
        findViewById(R.id.btnCopySHA1).setOnClickListener(this);
        findViewById(R.id.btnCopySHA1Without).setOnClickListener(this);

        tvPackageName.setText("应用包名：" + packageName);
        HashMap<String, Object> appInfo = PackageUtils.getAppInfo(mContext, packageName, false);
        if (appInfo != null) {
            tvAppName.setText("应用名称：" + appInfo.get(PackageUtils.APP_S_NAME));
            tvVersionName.setText("版本名称：" + appInfo.get(PackageUtils.APP_S_VERSION_NAME));
            tvVersionCode.setText("版本号：" + appInfo.get(PackageUtils.APP_I_VERSION_CODE));
            tvFirstInstalledTime.setText("首次安装时间：" + formatTime((long) appInfo.get(PackageUtils.APP_L_FIRST_INSTALLED_TIME), "yyyy-MM-dd HH:mm"));
            tvLastUpdateTime.setText("最近更新时间：" + formatTime((long) appInfo.get(PackageUtils.APP_L_LAST_UPDATE_TIME), "yyyy-MM-dd HH:mm"));
            tvAppLocation.setText("应用位置：" + appInfo.get(PackageUtils.APP_S_STORAGE));
            apkFile = new File((String) appInfo.get(PackageUtils.APP_S_STORAGE));
            tvAppSize.setText("应用大小：" + Formatter.formatFileSize(mContext, (long) appInfo.get(PackageUtils.APP_L_SIZE)));
            tvPermissions.setText(getPermissionString(packageName));
        } else {
            Toast.makeText(mContext, "无法获取应用信息", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private File apkFile;

    public String formatTime(long millisecond, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(new Date(millisecond));
    }

    private String getPermissionString(String pkgName) {
        StringBuffer sb = new StringBuffer();
        String[] perms = PackageUtils.getAppPermission(mContext,pkgName);
        if (perms != null) {
            for (String permName : perms) {
                sb.append(permName).append('\n');
            }
        }
        if (TextUtils.isEmpty(sb.toString())) {
            sb.append("没有相关权限");
        }
        return sb.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.share:
                final ProgressDialog progressDialog = new ProgressDialog(mContext);
                progressDialog.setMessage("正在复制文件...");
                progressDialog.show();
                Observable.create(new ObservableOnSubscribe<File>() {
                    @Override
                    public void subscribe(ObservableEmitter<File> emitter) throws Exception {
                        File copyApkFile = new File(getExternalFilesDir(null), "apk/" + appName + ".apk");
                        if (copyApkFile.exists()) {
                            copyApkFile.delete();
                        }
                        if (!copyApkFile.getParentFile().exists()) {
                            copyApkFile.getParentFile().mkdirs();
                        }
                        FileUtils.copyFileUsingStream(apkFile, copyApkFile);
                        emitter.onNext(copyApkFile);
                        emitter.onComplete();
                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        progressDialog.dismiss();
                        if (file != null && file.exists()) {
                            Uri contentUri = FileProvider.getUriForFile(mContext,
                                    BuildConfig.APPLICATION_ID + ".myprovider", file);
                            if (!IntentUtils.shareApkFile(mContext, contentUri)) {
                                Toast.makeText(mContext, "无可提供分享功能的应用", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
                break;
            case R.id.openApp:
                if (!IntentUtils.openApp(mContext, packageName)) {
                    Toast.makeText(mContext, "无法打开该应用程序", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_detail, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCopyMD5: {
                String md5Tag = (String) tvMD5.getTag();
                Toast.makeText(mContext, "复制成功", Toast.LENGTH_SHORT).show();
                ClipboardUtils.copyToClipboard(mContext, md5Tag);
            }
            break;
            case R.id.btnCopyMD5Without: {
                String md5Tag = (String) tvMD5.getTag();
                md5Tag = md5Tag.replace(":", "");
                Toast.makeText(mContext, "复制成功", Toast.LENGTH_SHORT).show();
                ClipboardUtils.copyToClipboard(mContext, md5Tag);
            }
            break;
            case R.id.btnCopySHA1: {
                String sha1Tag = (String) tvSHA1.getTag();
                Toast.makeText(mContext, "复制成功", Toast.LENGTH_SHORT).show();
                ClipboardUtils.copyToClipboard(mContext, sha1Tag);
            }
            break;
            case R.id.btnCopySHA1Without: {
                String sha1Tag = (String) tvSHA1.getTag();
                sha1Tag = sha1Tag.replace(":", "");

                Toast.makeText(mContext, "复制成功", Toast.LENGTH_SHORT).show();
                ClipboardUtils.copyToClipboard(mContext, sha1Tag);
            }
            break;
        }
    }


}
