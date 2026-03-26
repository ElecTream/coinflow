package com.leeam.cryptowidget.data.local;

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
public final class AlertRepository_Factory implements Factory<AlertRepository> {
  private final Provider<AlertDao> daoProvider;

  public AlertRepository_Factory(Provider<AlertDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public AlertRepository get() {
    return newInstance(daoProvider.get());
  }

  public static AlertRepository_Factory create(Provider<AlertDao> daoProvider) {
    return new AlertRepository_Factory(daoProvider);
  }

  public static AlertRepository newInstance(AlertDao dao) {
    return new AlertRepository(dao);
  }
}
