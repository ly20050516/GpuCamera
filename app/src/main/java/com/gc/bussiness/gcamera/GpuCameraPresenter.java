package com.gc.bussiness.gcamera;

import com.gc.framework.mvp.data.DataManager;
import com.gc.framework.mvp.ui.base.BasePresenter;
import com.gc.framework.mvp.utils.rx.SchedulerProvider;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;

/**
 * @author：ly on 2018/3/21 22:23
 * @mail：liuyan@zhimei.ai
 */

public class GpuCameraPresenter<V extends GpuCameraMvpView> extends BasePresenter<V> implements GpuCameraMvpPresenter<V>{
    @Inject
    public GpuCameraPresenter(DataManager dataManager, SchedulerProvider schedulerProvider, CompositeDisposable compositeDisposable) {
        super(dataManager, schedulerProvider, compositeDisposable);
    }
}
