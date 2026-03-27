package com.leeam.cryptowidget.data.repository;

import com.leeam.cryptowidget.data.remote.KrakenService;
import com.leeam.cryptowidget.data.remote.XrplService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class CryptoRepositoryImpl_Factory implements Factory<CryptoRepositoryImpl> {
  private final Provider<KrakenService> krakenServiceProvider;

  private final Provider<XrplService> xrplServiceProvider;

  private CryptoRepositoryImpl_Factory(Provider<KrakenService> krakenServiceProvider,
      Provider<XrplService> xrplServiceProvider) {
    this.krakenServiceProvider = krakenServiceProvider;
    this.xrplServiceProvider = xrplServiceProvider;
  }

  @Override
  public CryptoRepositoryImpl get() {
    return newInstance(krakenServiceProvider.get(), xrplServiceProvider.get());
  }

  public static CryptoRepositoryImpl_Factory create(Provider<KrakenService> krakenServiceProvider,
      Provider<XrplService> xrplServiceProvider) {
    return new CryptoRepositoryImpl_Factory(krakenServiceProvider, xrplServiceProvider);
  }

  public static CryptoRepositoryImpl newInstance(KrakenService krakenService,
      XrplService xrplService) {
    return new CryptoRepositoryImpl(krakenService, xrplService);
  }
}
