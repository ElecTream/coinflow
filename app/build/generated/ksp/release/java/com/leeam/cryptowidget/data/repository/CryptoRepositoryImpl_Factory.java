package com.leeam.cryptowidget.data.repository;

import com.leeam.cryptowidget.data.remote.BitcoinService;
import com.leeam.cryptowidget.data.remote.EthereumService;
import com.leeam.cryptowidget.data.remote.GenericRestService;
import com.leeam.cryptowidget.data.remote.KrakenService;
import com.leeam.cryptowidget.data.remote.SolanaService;
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

  private final Provider<BitcoinService> bitcoinServiceProvider;

  private final Provider<EthereumService> ethereumServiceProvider;

  private final Provider<SolanaService> solanaServiceProvider;

  private final Provider<GenericRestService> genericRestServiceProvider;

  private final Provider<CoinRepository> coinRepositoryProvider;

  private CryptoRepositoryImpl_Factory(Provider<KrakenService> krakenServiceProvider,
      Provider<XrplService> xrplServiceProvider, Provider<BitcoinService> bitcoinServiceProvider,
      Provider<EthereumService> ethereumServiceProvider,
      Provider<SolanaService> solanaServiceProvider,
      Provider<GenericRestService> genericRestServiceProvider,
      Provider<CoinRepository> coinRepositoryProvider) {
    this.krakenServiceProvider = krakenServiceProvider;
    this.xrplServiceProvider = xrplServiceProvider;
    this.bitcoinServiceProvider = bitcoinServiceProvider;
    this.ethereumServiceProvider = ethereumServiceProvider;
    this.solanaServiceProvider = solanaServiceProvider;
    this.genericRestServiceProvider = genericRestServiceProvider;
    this.coinRepositoryProvider = coinRepositoryProvider;
  }

  @Override
  public CryptoRepositoryImpl get() {
    return newInstance(krakenServiceProvider.get(), xrplServiceProvider.get(), bitcoinServiceProvider.get(), ethereumServiceProvider.get(), solanaServiceProvider.get(), genericRestServiceProvider.get(), coinRepositoryProvider.get());
  }

  public static CryptoRepositoryImpl_Factory create(Provider<KrakenService> krakenServiceProvider,
      Provider<XrplService> xrplServiceProvider, Provider<BitcoinService> bitcoinServiceProvider,
      Provider<EthereumService> ethereumServiceProvider,
      Provider<SolanaService> solanaServiceProvider,
      Provider<GenericRestService> genericRestServiceProvider,
      Provider<CoinRepository> coinRepositoryProvider) {
    return new CryptoRepositoryImpl_Factory(krakenServiceProvider, xrplServiceProvider, bitcoinServiceProvider, ethereumServiceProvider, solanaServiceProvider, genericRestServiceProvider, coinRepositoryProvider);
  }

  public static CryptoRepositoryImpl newInstance(KrakenService krakenService,
      XrplService xrplService, BitcoinService bitcoinService, EthereumService ethereumService,
      SolanaService solanaService, GenericRestService genericRestService,
      CoinRepository coinRepository) {
    return new CryptoRepositoryImpl(krakenService, xrplService, bitcoinService, ethereumService, solanaService, genericRestService, coinRepository);
  }
}
