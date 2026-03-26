package com.leeam.cryptowidget.di;

import com.leeam.cryptowidget.data.remote.XrpScanService;
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
public final class NetworkModule_ProvideXrpScanServiceFactory implements Factory<XrpScanService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideXrpScanServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public XrpScanService get() {
    return provideXrpScanService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideXrpScanServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideXrpScanServiceFactory(retrofitProvider);
  }

  public static XrpScanService provideXrpScanService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideXrpScanService(retrofit));
  }
}
