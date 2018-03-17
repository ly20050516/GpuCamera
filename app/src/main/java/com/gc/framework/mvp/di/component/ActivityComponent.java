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

package com.gc.framework.mvp.di.component;

import com.gc.framework.mvp.di.PerActivity;
import com.gc.framework.mvp.di.module.ActivityModule;
import com.gc.bussiness.about.AboutFragment;
import com.gc.bussiness.feed.FeedActivity;
import com.gc.bussiness.feed.blogs.BlogFragment;
import com.gc.bussiness.feed.opensource.OpenSourceFragment;
import com.gc.bussiness.login.LoginActivity;
import com.gc.bussiness.main.MainActivity;
import com.gc.bussiness.main.rating.RateUsDialog;
import com.gc.bussiness.splash.SplashActivity;

import dagger.Component;

/**
 * Created by janisharali on 27/01/17.
 */

@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(MainActivity activity);

    void inject(LoginActivity activity);

    void inject(SplashActivity activity);

    void inject(FeedActivity activity);

    void inject(AboutFragment fragment);

    void inject(OpenSourceFragment fragment);

    void inject(BlogFragment fragment);

    void inject(RateUsDialog dialog);

}
