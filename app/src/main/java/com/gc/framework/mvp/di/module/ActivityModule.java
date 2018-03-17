/*
 * Copyright (C) 2017 MINDORKS NEXTGEN PRIVATE LIMITED
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://mindorks.com/license/apache-v2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.gc.framework.mvp.di.module;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;

import com.gc.framework.mvp.data.network.model.OpenSourceResponse;
import com.gc.bussiness.feed.FeedPagerAdapter;
import com.gc.framework.mvp.data.network.model.BlogResponse;
import com.gc.framework.mvp.di.ActivityContext;
import com.gc.framework.mvp.di.PerActivity;
import com.gc.bussiness.about.AboutMvpPresenter;
import com.gc.bussiness.about.AboutMvpView;
import com.gc.bussiness.about.AboutPresenter;
import com.gc.bussiness.feed.FeedMvpPresenter;
import com.gc.bussiness.feed.FeedMvpView;
import com.gc.bussiness.feed.FeedPresenter;
import com.gc.bussiness.feed.blogs.BlogAdapter;
import com.gc.bussiness.feed.blogs.BlogMvpPresenter;
import com.gc.bussiness.feed.blogs.BlogMvpView;
import com.gc.bussiness.feed.blogs.BlogPresenter;
import com.gc.bussiness.feed.opensource.OpenSourceAdapter;
import com.gc.bussiness.feed.opensource.OpenSourceMvpPresenter;
import com.gc.bussiness.feed.opensource.OpenSourceMvpView;
import com.gc.bussiness.feed.opensource.OpenSourcePresenter;
import com.gc.bussiness.login.LoginMvpPresenter;
import com.gc.bussiness.login.LoginMvpView;
import com.gc.bussiness.login.LoginPresenter;
import com.gc.bussiness.main.MainMvpPresenter;
import com.gc.bussiness.main.MainMvpView;
import com.gc.bussiness.main.MainPresenter;
import com.gc.bussiness.main.rating.RatingDialogMvpPresenter;
import com.gc.bussiness.main.rating.RatingDialogMvpView;
import com.gc.bussiness.main.rating.RatingDialogPresenter;
import com.gc.bussiness.splash.SplashMvpPresenter;
import com.gc.bussiness.splash.SplashMvpView;
import com.gc.bussiness.splash.SplashPresenter;
import com.gc.framework.mvp.utils.rx.AppSchedulerProvider;
import com.gc.framework.mvp.utils.rx.SchedulerProvider;

import java.util.ArrayList;

import dagger.Module;
import dagger.Provides;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by janisharali on 27/01/17.
 */

@Module
public class ActivityModule {

    private AppCompatActivity mActivity;

    public ActivityModule(AppCompatActivity activity) {
        this.mActivity = activity;
    }

    @Provides
    @ActivityContext
    Context provideContext() {
        return mActivity;
    }

    @Provides
    AppCompatActivity provideActivity() {
        return mActivity;
    }

    @Provides
    CompositeDisposable provideCompositeDisposable() {
        return new CompositeDisposable();
    }

    @Provides
    SchedulerProvider provideSchedulerProvider() {
        return new AppSchedulerProvider();
    }

    @Provides
    @PerActivity
    SplashMvpPresenter<SplashMvpView> provideSplashPresenter(
            SplashPresenter<SplashMvpView> presenter) {
        return presenter;
    }

    @Provides
    AboutMvpPresenter<AboutMvpView> provideAboutPresenter(
            AboutPresenter<AboutMvpView> presenter) {
        return presenter;
    }

    @Provides
    @PerActivity
    LoginMvpPresenter<LoginMvpView> provideLoginPresenter(
            LoginPresenter<LoginMvpView> presenter) {
        return presenter;
    }

    @Provides
    @PerActivity
    MainMvpPresenter<MainMvpView> provideMainPresenter(
            MainPresenter<MainMvpView> presenter) {
        return presenter;
    }

    @Provides
    RatingDialogMvpPresenter<RatingDialogMvpView> provideRateUsPresenter(
            RatingDialogPresenter<RatingDialogMvpView> presenter) {
        return presenter;
    }

    @Provides
    FeedMvpPresenter<FeedMvpView> provideFeedPresenter(
            FeedPresenter<FeedMvpView> presenter) {
        return presenter;
    }

    @Provides
    OpenSourceMvpPresenter<OpenSourceMvpView> provideOpenSourcePresenter(
            OpenSourcePresenter<OpenSourceMvpView> presenter) {
        return presenter;
    }

    @Provides
    BlogMvpPresenter<BlogMvpView> provideBlogMvpPresenter(
            BlogPresenter<BlogMvpView> presenter) {
        return presenter;
    }

    @Provides
    FeedPagerAdapter provideFeedPagerAdapter(AppCompatActivity activity) {
        return new FeedPagerAdapter(activity.getSupportFragmentManager());
    }

    @Provides
    OpenSourceAdapter provideOpenSourceAdapter() {
        return new OpenSourceAdapter(new ArrayList<OpenSourceResponse.Repo>());
    }

    @Provides
    BlogAdapter provideBlogAdapter() {
        return new BlogAdapter(new ArrayList<BlogResponse.Blog>());
    }

    @Provides
    LinearLayoutManager provideLinearLayoutManager(AppCompatActivity activity) {
        return new LinearLayoutManager(activity);
    }
}
