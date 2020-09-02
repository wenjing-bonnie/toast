package com.android.toast;

/**
 * Created by wenjing.liu on 2020-08-11 .
 * {@link ITransientNotification}
 *
 * @author wenjing.liu
 */
public interface ITransientPowerfulToast {

    /**
     * schedule handleHide into the right thread
     */
    void hide();

    /**
     * schedule handleShow into the right thread
     */
    void show();
}
