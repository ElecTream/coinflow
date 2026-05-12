package com.leeam.cryptowidget.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.leeam.cryptowidget.data.local.AlertRepository;
import com.leeam.cryptowidget.data.local.DebugLog;
import com.leeam.cryptowidget.data.local.WidgetPreferences;
import com.leeam.cryptowidget.data.repository.CoinRepository;
import com.leeam.cryptowidget.data.repository.CryptoRepository;
import com.leeam.cryptowidget.notifications.AlertNotifier;
import dagger.internal.DaggerGenerated;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class PriceUpdateWorker_Factory {
  private final Provider<CryptoRepository> cryptoRepositoryProvider;

  private final Provider<CoinRepository> coinRepositoryProvider;

  private final Provider<WidgetPreferences> widgetPreferencesProvider;

  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<AlertNotifier> alertNotifierProvider;

  private final Provider<DebugLog> debugLogProvider;

  private PriceUpdateWorker_Factory(Provider<CryptoRepository> cryptoRepositoryProvider,
      Provider<CoinRepository> coinRepositoryProvider,
      Provider<WidgetPreferences> widgetPreferencesProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<AlertNotifier> alertNotifierProvider, Provider<DebugLog> debugLogProvider) {
    this.cryptoRepositoryProvider = cryptoRepositoryProvider;
    this.coinRepositoryProvider = coinRepositoryProvider;
    this.widgetPreferencesProvider = widgetPreferencesProvider;
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.alertNotifierProvider = alertNotifierProvider;
    this.debugLogProvider = debugLogProvider;
  }

  public PriceUpdateWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, cryptoRepositoryProvider.get(), coinRepositoryProvider.get(), widgetPreferencesProvider.get(), alertRepositoryProvider.get(), alertNotifierProvider.get(), debugLogProvider.get());
  }

  public static PriceUpdateWorker_Factory create(
      Provider<CryptoRepository> cryptoRepositoryProvider,
      Provider<CoinRepository> coinRepositoryProvider,
      Provider<WidgetPreferences> widgetPreferencesProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<AlertNotifier> alertNotifierProvider, Provider<DebugLog> debugLogProvider) {
    return new PriceUpdateWorker_Factory(cryptoRepositoryProvider, coinRepositoryProvider, widgetPreferencesProvider, alertRepositoryProvider, alertNotifierProvider, debugLogProvider);
  }

  public static PriceUpdateWorker newInstance(Context context, WorkerParameters workerParams,
      CryptoRepository cryptoRepository, CoinRepository coinRepository,
      WidgetPreferences widgetPreferences, AlertRepository alertRepository,
      AlertNotifier alertNotifier, DebugLog debugLog) {
    return new PriceUpdateWorker(context, workerParams, cryptoRepository, coinRepository, widgetPreferences, alertRepository, alertNotifier, debugLog);
  }
}
