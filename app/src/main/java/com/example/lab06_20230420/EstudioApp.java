package com.example.lab06_20230420;
import android.app.Application;
import android.app.LocaleManager;
import android.os.LocaleList;
public class EstudioApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags("es"));
    }
}
