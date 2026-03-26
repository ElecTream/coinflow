package com.leeam.cryptowidget;

import androidx.hilt.work.HiltWorkerFactory;
import com.leeam.cryptowidget.worker.WorkScheduler;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class CryptoWidgetApplication_MembersInjector implements MembersInjector<CryptoWidgetApplication> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  private final Provider<WorkScheduler> workSchedulerProvider;

  public CryptoWidgetApplication_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<WorkScheduler> workSchedulerProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
    this.workSchedulerProvider = workSchedulerProvider;
  }

  public static MembersInjector<CryptoWidgetApplication> create(
      Provider<HiltWorkerFactory> workerFactoryProvider,
      Provider<WorkScheduler> workSchedulerProvider) {
    return new CryptoWidgetApplication_MembersInjector(workerFactoryProvider, workSchedulerProvider);
  }

  @Override
  public void injectMembers(CryptoWidgetApplication instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
    injectWorkScheduler(instance, workSchedulerProvider.get());
  }

  @InjectedFieldSignature("com.leeam.cryptowidget.CryptoWidgetApplication.workerFactory")
  public static void injectWorkerFactory(CryptoWidgetApplication instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }

  @InjectedFieldSignature("com.leeam.cryptowidget.CryptoWidgetApplication.workScheduler")
  public static void injectWorkScheduler(CryptoWidgetApplication instance,
      WorkScheduler workScheduler) {
    instance.workScheduler = workScheduler;
  }
}
