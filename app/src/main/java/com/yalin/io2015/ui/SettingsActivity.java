package com.yalin.io2015.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.yalin.io2015.R;

/**
 * Created by YaLin on 2015/12/23.
 */
public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        overridePendingTransition(0, 0);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_SETTINGS;
    }
}
