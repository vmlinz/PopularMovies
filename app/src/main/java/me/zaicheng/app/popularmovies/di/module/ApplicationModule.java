package me.zaicheng.app.popularmovies.di.module;

import android.app.Application;
import android.content.Context;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raizlabs.android.dbflow.structure.ModelAdapter;
import com.squareup.leakcanary.RefWatcher;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.zaicheng.app.popularmovies.MovieApplication;
import me.zaicheng.app.popularmovies.data.remote.MovieService;
import me.zaicheng.app.popularmovies.di.ApplicationContext;
import me.zaicheng.app.popularmovies.rxbus.RxBus;

/**
 * Created by vmlinz on 3/17/16.
 */

@Module
public class ApplicationModule {
    protected final Application mApplication;


    public ApplicationModule(Application mApplication) {
        this.mApplication = mApplication;
    }

    @Provides
    Application provideApplication() {
        return mApplication;
    }

    @Provides
    @ApplicationContext
    Context provideContext() {
        return mApplication;
    }

    @Provides
    @Singleton
    RxBus provideEventBus() {
        return RxBus.newInstance();
    }

    @Provides
    @Singleton
    MovieService provideMovieService() {
        return MovieService.Creator.newMovieService();
    }

    @Provides
    @Singleton
    RefWatcher provideRefWatcher() {
        return MovieApplication.getRefWatcher(mApplication);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaredClass().equals(ModelAdapter.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }
}
