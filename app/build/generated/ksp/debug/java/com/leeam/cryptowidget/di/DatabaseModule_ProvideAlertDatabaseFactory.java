package com.leeam.cryptowidget.di;

import android.content.Context;
import com.leeam.cryptowidget.data.local.AlertDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideAlertDatabaseFactory implements Factory<AlertDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideAlertDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AlertDatabase get() {
    return provideAlertDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideAlertDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideAlertDatabaseFactory(contextProvider);
  }

  public static AlertDatabase provideAlertDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAlertDatabase(context));
  }
}
