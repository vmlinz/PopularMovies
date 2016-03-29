package me.zaicheng.app.popularmovies.data;

import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import me.zaicheng.app.popularmovies.data.local.PreferenceHelper;
import me.zaicheng.app.popularmovies.data.model.Movie;
import me.zaicheng.app.popularmovies.data.model.Movie_Table;
import me.zaicheng.app.popularmovies.data.remote.MovieService;
import me.zaicheng.app.popularmovies.data.remote.MoviesResponse;
import me.zaicheng.app.popularmovies.rxbus.RxBus;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;

/**
 * Created by vmlinz on 3/23/16.
 */
@Singleton
public class DataManager {
    private final MovieService mMovieService;
    private final RxBus mRxBus;
    private final PreferenceHelper mPreferenceHelper;

    @Inject
    public DataManager(MovieService movieService,
                       RxBus rxBus,
                       PreferenceHelper mPreferenceHelper) {
        this.mMovieService = movieService;
        this.mRxBus = rxBus;
        this.mPreferenceHelper = mPreferenceHelper;
    }

    public Observable<List<Movie>> getPopularMovies() {
        return mMovieService.getPopularMoviesObservable()
                .map(new Func1<MoviesResponse, List<Movie>>() {
                    @Override
                    public List<Movie> call(MoviesResponse moviesResponse) {
                        return moviesResponse.results;
                    }
                });
    }

    public Observable<List<Movie>> getTopRatedMovies() {
        return mMovieService.getTopRatedMoviesObservable()
                .map(new Func1<MoviesResponse, List<Movie>>() {
                    @Override
                    public List<Movie> call(MoviesResponse moviesResponse) {
                        return moviesResponse.results;
                    }
                });
    }

    public Observable<Movie> syncMovies() {
        if (mPreferenceHelper.getSharedPreference()
                .getString(PreferenceHelper.PREF_KEY_MOVIE_ORDER,
                        PreferenceHelper.PREF_VALUE_MOVIE_ORDER_POPULAR)
                .equals(PreferenceHelper.PREF_VALUE_MOVIE_ORDER_POPULAR)) {
            // get popular movies
            return this.getPopularMovies()
                    .concatMap(new Func1<List<Movie>, Observable<? extends Movie>>() {
                        @Override
                        public Observable<? extends Movie> call(List<Movie> movies) {
                            return setMoviesInDb(movies);
                        }
                    });
        } else if (mPreferenceHelper.getSharedPreference()
                .getString(PreferenceHelper.PREF_KEY_MOVIE_ORDER,
                        PreferenceHelper.PREF_VALUE_MOVIE_ORDER_POPULAR)
                .equals(PreferenceHelper.PREF_VALUE_MOVIE_ORDER_TOP_RATED)) {
            // get top rated movies
            return this.getTopRatedMovies()
                    .concatMap(new Func1<List<Movie>, Observable<? extends Movie>>() {
                        @Override
                        public Observable<? extends Movie> call(List<Movie> movies) {
                            return setMoviesInDb(movies);
                        }
                    });
        } else {
            // TODO: 3/29/16 Implement fetch favorite movies
            return Observable.empty();
        }
    }

    public Observable<Void> clearMoviesInDb() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    Delete.table(Movie.class);
                    subscriber.onCompleted();
                }
            }
        });
    }

    public Observable<Movie> setMoviesInDb(final List<Movie> movies) {
        return Observable.create(new Observable.OnSubscribe<Movie>() {
            @Override
            public void call(Subscriber<? super Movie> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    Delete.table(Movie.class);

                    // try to save movies synchronously as a transaction
                    // TODO: 3/23/16 Make sure synchronous saving works as expected
                    FlowContentObserver contentObserver = new FlowContentObserver();
                    contentObserver.beginTransaction();
                    for (Movie movie : movies) {
                        movie.save();
                        subscriber.onNext(movie);
                    }
                    contentObserver.endTransactionAndNotify();
                    subscriber.onCompleted();
                }
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                // send notification that movies are updated
                mRxBus.send(new BusEvent.MoviesSaved());
            }
        });
    }

    public Observable<List<Movie>> getMoviesFromDb() {
        return Observable.create(new Observable.OnSubscribe<List<Movie>>() {
            @Override
            public void call(Subscriber<? super List<Movie>> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(new Select().from(Movie.class).queryList());
                    subscriber.onCompleted();
                }
            }
        });
    }

    public Observable<Movie> getMovieById(final long id) {
        return Observable.create(new Observable.OnSubscribe<Movie>() {
            @Override
            public void call(Subscriber<? super Movie> subscriber) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(new Select().from(Movie.class).where(Movie_Table.tmdb_id.is(id)).querySingle());
                    subscriber.onCompleted();
                }
            }
        });
    }
}