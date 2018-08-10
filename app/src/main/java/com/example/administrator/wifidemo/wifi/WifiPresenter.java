package com.example.administrator.wifidemo.wifi;


import com.example.administrator.wifidemo.BasePresenterImpl;
import com.example.administrator.wifidemo.wifi.source.WifiDataSource;

/**
 * WifiPresenter
 *
 * @author 贾博瑄
 */

public class WifiPresenter extends BasePresenterImpl implements WifiContract.Presenter {

    private final WifiContract.View mView;

    private final WifiDataSource mDataSource;

    public WifiPresenter(WifiContract.View view, WifiDataSource dataSource) {
        mView = view;
        mDataSource = dataSource;
    }
}
