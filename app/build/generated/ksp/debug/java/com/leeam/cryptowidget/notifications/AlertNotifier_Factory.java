package com.leeam.cryptowidget.notifications;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AlertNotifier_Factory implements Factory<AlertNotifier> {
  private final Provider<Context> contextProvider;

  private AlertNotifier_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AlertNotifier get() {
    return newInstance(contextProvider.get());
  }

  public static AlertNotifier_Factory create(Provider<Context> contextProvider) {
    return new AlertNotifier_Factory(contextProvider);
  }

  public static AlertNotifier newInstance(Context context) {
    return new AlertNotifier(context);
  }
}
