package co.joebirch.braillebox.data;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import co.joebirch.braillebox.data.model.ArticleModel;
import co.joebirch.braillebox.data.model.NewsResponse;
import co.joebirch.braillebox.data.remote.BrailleBoxService;
import co.joebirch.braillebox.util.BrailleMapper;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class DataManager {

    private final BrailleBoxService brailleBoxService;
    private final BrailleMapper brailleMapper;

    @Inject
    DataManager(BrailleBoxService brailleBoxService,
                BrailleMapper brailleMapper) {
        this.brailleBoxService = brailleBoxService;
        this.brailleMapper = brailleMapper;
    }

    public Observable<String> getArticle(final int delay) {
        return brailleBoxService.getArticles(Source.BBC_NEWS.getId(), SortBy.TOP.getLabel())
                .filter(new Predicate<NewsResponse>() {
                    @Override
                    public boolean test(NewsResponse newsResponse) throws Exception {
                        return newsResponse.articles != null &&
                                !newsResponse.articles.isEmpty();
                    }
                })
                .flatMap(new Function<NewsResponse, ObservableSource<ArticleModel>>() {
                    @Override
                    public ObservableSource<ArticleModel> apply(NewsResponse newsResponse)
                            throws Exception {
                        return Observable.just(newsResponse.articles.get(0));
                    }
                })
                .flatMap(new Function<ArticleModel, ObservableSource<List<String>>>() {
                    @Override
                    public ObservableSource<List<String>> apply(ArticleModel articleModel)
                            throws Exception {
                        return Observable.just(brailleMapper.mapFromWords(
                                articleModel.title, articleModel.description));
                    }
                })
                .flatMap(new Function<List<String>, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(List<String> strings) throws Exception {
                        return Observable.fromIterable(strings);
                    }
                })
                .zipWith(Observable.interval(delay, TimeUnit.MILLISECONDS),
                        new BiFunction<String, Long, String>() {
                            @Override
                            public String apply(String sequence, Long aLong) throws Exception {
                                return sequence;
                            }
                        });
    }

}