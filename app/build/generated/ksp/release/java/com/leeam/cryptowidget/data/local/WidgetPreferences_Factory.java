package com.leeam.cryptowidget.data.local;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "deprecation"
})
public final class WidgetPreferences_Factory implements Factory<WidgetPreferences> {
  private final Provider<Context> contextProvider;

  public WidgetPreferences_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WidgetPreferences get() {
    return newInstance(contextProvider.get());
  }

  public static WidgetPreferences_Factory create(Provider<Context> contextProvider) {
    return new WidgetPreferences_Factory(contextProvider);
  }

  public static WidgetPreferences newInstance(Context context) {
    return new WidgetPreferences(context);
  }
}
