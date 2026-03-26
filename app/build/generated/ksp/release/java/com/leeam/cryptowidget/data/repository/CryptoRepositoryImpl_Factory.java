package com.leeam.cryptowidget.data.repository;

import com.leeam.cryptowidget.data.remote.CoinGeckoService;
import com.leeam.cryptowidget.data.remote.XrpScanService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "deprecation"
})
public final class CryptoRepositoryImpl_Factory implements Factory<CryptoRepositoryImpl> {
  private final Provider<CoinGeckoService> coinGeckoServiceProvider;

  private final Provider<XrpScanService> xrpScanServiceProvider;

  public CryptoRepositoryImpl_Factory(Provider<CoinGeckoService> coinGeckoServiceProvider,
      Provider<XrpScanService> xrpScanServiceProvider) {
    this.coinGeckoServiceProvider = coinGeckoServiceProvider;
    this.xrpScanServiceProvider = xrpScanServiceProvider;
  }

  @Override
  public CryptoRepositoryImpl get() {
    return newInstance(coinGeckoServiceProvider.get(), xrpScanServiceProvider.get());
  }

  public static CryptoRepositoryImpl_Factory create(
      Provider<CoinGeckoService> coinGeckoServiceProvider,
      Provider<XrpScanService> xrpScanServiceProvider) {
    return new CryptoRepositoryImpl_Factory(coinGeckoServiceProvider, xrpScanServiceProvider);
  }

  public static CryptoRepositoryImpl newInstance(CoinGeckoService coinGeckoService,
      XrpScanService xrpScanService) {
    return new CryptoRepositoryImpl(coinGeckoService, xrpScanService);
  }
}
