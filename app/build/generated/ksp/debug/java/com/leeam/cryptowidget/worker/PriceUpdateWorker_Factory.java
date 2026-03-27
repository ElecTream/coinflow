package com.leeam.cryptowidget.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.leeam.cryptowidget.data.local.AlertRepository;
import com.leeam.cryptowidget.data.local.WidgetPreferences;
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

  private final Provider<WidgetPreferences> widgetPreferencesProvider;

  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<AlertNotifier> alertNotifierProvider;

  private PriceUpdateWorker_Factory(Provider<CryptoRepository> cryptoRepositoryProvider,
      Provider<WidgetPreferences> widgetPreferencesProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<AlertNotifier> alertNotifierProvider) {
    this.cryptoRepositoryProvider = cryptoRepositoryProvider;
    this.widgetPreferencesProvider = widgetPreferencesProvider;
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.alertNotifierProvider = alertNotifierProvider;
  }

  public PriceUpdateWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, cryptoRepositoryProvider.get(), widgetPreferencesProvider.get(), alertRepositoryProvider.get(), alertNotifierProvider.get());
  }

  public static PriceUpdateWorker_Factory create(
      Provider<CryptoRepository> cryptoRepositoryProvider,
      Provider<WidgetPreferences> widgetPreferencesProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<AlertNotifier> alertNotifierProvider) {
    return new PriceUpdateWorker_Factory(cryptoRepositoryProvider, widgetPreferencesProvider, alertRepositoryProvider, alertNotifierProvider);
  }

  public static PriceUpdateWorker newInstance(Context context, WorkerParameters workerParams,
      CryptoRepository cryptoRepository, WidgetPreferences widgetPreferences,
      AlertRepository alertRepository, AlertNotifier alertNotifier) {
    return new PriceUpdateWorker(context, workerParams, cryptoRepository, widgetPreferences, alertRepository, alertNotifier);
  }
}
