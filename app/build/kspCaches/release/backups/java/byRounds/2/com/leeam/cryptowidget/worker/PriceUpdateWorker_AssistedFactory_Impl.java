package com.leeam.cryptowidget.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PriceUpdateWorker_AssistedFactory_Impl implements PriceUpdateWorker_AssistedFactory {
  private final PriceUpdateWorker_Factory delegateFactory;

  PriceUpdateWorker_AssistedFactory_Impl(PriceUpdateWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public PriceUpdateWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<PriceUpdateWorker_AssistedFactory> create(
      PriceUpdateWorker_Factory delegateFactory) {
    return InstanceFactory.create(new PriceUpdateWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<PriceUpdateWorker_AssistedFactory> createFactoryProvider(
      PriceUpdateWorker_Factory delegateFactory) {
    return InstanceFactory.create(new PriceUpdateWorker_AssistedFactory_Impl(delegateFactory));
  }
}
