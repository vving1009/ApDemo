package com.example.administrator.wifidemo.wifi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.administrator.wifidemo.R;


/**
 * WifiActivity
 *
 * @author 贾博瑄
 */

public class WifiActivity extends AppCompatActivity {

    private static final String TAG = "liwei";

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, WifiActivity.class);
        context.startActivity(intent);
    }

    private ViewPager mViewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "WifiActivity onCreate: ");
        setContentView(R.layout.wifi_activity);
        mViewPager = findViewById(R.id.view_pager);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:// 左
                        return WifiFragment.newInstance();
                }
                return null;
            }
        });
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                /*MainFragment mainFragment = (MainFragment) getSupportFragmentManager()
                        .findFragmentByTag("android:switcher:" + R.id.view_pager + ":" + 1);
                switch (position) {
                    case 0:
                        mainFragment.pause();
                        break;
                    case 1:
                        mainFragment.resume();
                        break;
                }*/
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
}
