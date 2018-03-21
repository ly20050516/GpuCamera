package com.gc.bussiness.gcamera;

import com.gc.framework.mvp.di.PerActivity;
import com.gc.framework.mvp.ui.base.MvpPresenter;
import com.gc.framework.mvp.ui.base.MvpView;

/**
 * @author：ly on 2018/3/21 22:21
 * @mail：liuyan@zhimei.ai
 */
@PerActivity
public interface GpuCameraMvpPresenter<V extends MvpView> extends MvpPresenter<V>{
}
