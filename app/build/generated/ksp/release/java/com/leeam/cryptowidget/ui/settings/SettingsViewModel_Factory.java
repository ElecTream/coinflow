package com.leeam.cryptowidget.ui.settings;

import android.content.Context;
import com.leeam.cryptowidget.data.local.AlertRepository;
import com.leeam.cryptowidget.data.local.WidgetPreferences;
import com.leeam.cryptowidget.data.repository.CryptoRepository;
import com.leeam.cryptowidget.worker.WorkScheduler;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<WidgetPreferences> widgetPrefsProvider;

  private final Provider<CryptoRepository> cryptoRepositoryProvider;

  private final Provider<AlertRepository> alertRepositoryProvider;

  private final Provider<WorkScheduler> workSchedulerProvider;

  private SettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<WidgetPreferences> widgetPrefsProvider,
      Provider<CryptoRepository> cryptoRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<WorkScheduler> workSchedulerProvider) {
    this.contextProvider = contextProvider;
    this.widgetPrefsProvider = widgetPrefsProvider;
    this.cryptoRepositoryProvider = cryptoRepositoryProvider;
    this.alertRepositoryProvider = alertRepositoryProvider;
    this.workSchedulerProvider = workSchedulerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(contextProvider.get(), widgetPrefsProvider.get(), cryptoRepositoryProvider.get(), alertRepositoryProvider.get(), workSchedulerProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<WidgetPreferences> widgetPrefsProvider,
      Provider<CryptoRepository> cryptoRepositoryProvider,
      Provider<AlertRepository> alertRepositoryProvider,
      Provider<WorkScheduler> workSchedulerProvider) {
    return new SettingsViewModel_Factory(contextProvider, widgetPrefsProvider, cryptoRepositoryProvider, alertRepositoryProvider, workSchedulerProvider);
  }

  public static SettingsViewModel newInstance(Context context, WidgetPreferences widgetPrefs,
      CryptoRepository cryptoRepository, AlertRepository alertRepository,
      WorkScheduler workScheduler) {
    return new SettingsViewModel(context, widgetPrefs, cryptoRepository, alertRepository, workScheduler);
  }
}
