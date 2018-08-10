package com.example.administrator.wifidemo;

import io.reactivex.disposables.CompositeDisposable;

/**
 * BasePresenterImpl
 *
 * @author 贾博瑄
 */

public abstract class BasePresenterImpl implements BasePresenter {

    protected CompositeDisposable mCompositeDisposable;

    public BasePresenterImpl() {
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
        mCompositeDisposable.dispose();
    }
}
