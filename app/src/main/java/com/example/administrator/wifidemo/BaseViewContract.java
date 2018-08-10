package com.example.administrator.wifidemo;

/**
 * BaseViewContract
 *
 * @author 贾博瑄
 */

public interface BaseViewContract {

    /**
     * 显示提示
     *
     * @param string 提示语
     */
    void showToast(String string);

    /**
     * 显示提示通过资源Id
     *
     * @param resId 资源Id
     */
    void showToastById(int resId);

    /**
     * 显示进度框
     */
    void showProgressDialog();

    /**
     * 解除进度框
     */
    void dismissProgressDialog();
}
