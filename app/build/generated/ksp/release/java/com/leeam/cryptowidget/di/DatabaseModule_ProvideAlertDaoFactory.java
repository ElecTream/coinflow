package com.leeam.cryptowidget.di;

import com.leeam.cryptowidget.data.local.AlertDao;
import com.leeam.cryptowidget.data.local.AlertDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideAlertDaoFactory implements Factory<AlertDao> {
  private final Provider<AlertDatabase> dbProvider;

  private DatabaseModule_ProvideAlertDaoFactory(Provider<AlertDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public AlertDao get() {
    return provideAlertDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideAlertDaoFactory create(Provider<AlertDatabase> dbProvider) {
    return new DatabaseModule_ProvideAlertDaoFactory(dbProvider);
  }

  public static AlertDao provideAlertDao(AlertDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAlertDao(db));
  }
}
