package com.leeam.cryptowidget.di;

import com.leeam.cryptowidget.data.remote.CoinGeckoService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class NetworkModule_ProvideCoinGeckoServiceFactory implements Factory<CoinGeckoService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideCoinGeckoServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public CoinGeckoService get() {
    return provideCoinGeckoService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideCoinGeckoServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideCoinGeckoServiceFactory(retrofitProvider);
  }

  public static CoinGeckoService provideCoinGeckoService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideCoinGeckoService(retrofit));
  }
}
