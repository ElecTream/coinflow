package com.leeam.cryptowidget;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.leeam.cryptowidget.data.local.AlertDao;
import com.leeam.cryptowidget.data.local.AlertDatabase;
import com.leeam.cryptowidget.data.local.AlertRepository;
import com.leeam.cryptowidget.data.local.WidgetPreferences;
import com.leeam.cryptowidget.data.remote.KrakenService;
import com.leeam.cryptowidget.data.remote.XrplService;
import com.leeam.cryptowidget.data.repository.CryptoRepository;
import com.leeam.cryptowidget.data.repository.CryptoRepositoryImpl;
import com.leeam.cryptowidget.di.DatabaseModule_ProvideAlertDaoFactory;
import com.leeam.cryptowidget.di.DatabaseModule_ProvideAlertDatabaseFactory;
import com.leeam.cryptowidget.di.NetworkModule_ProvideKrakenRetrofitFactory;
import com.leeam.cryptowidget.di.NetworkModule_ProvideKrakenServiceFactory;
import com.leeam.cryptowidget.di.NetworkModule_ProvideOkHttpClientFactory;
import com.leeam.cryptowidget.di.NetworkModule_ProvideXrplRetrofitFactory;
import com.leeam.cryptowidget.di.NetworkModule_ProvideXrplServiceFactory;
import com.leeam.cryptowidget.notifications.AlertNotifier;
import com.leeam.cryptowidget.ui.chart.ChartDetailActivity;
import com.leeam.cryptowidget.ui.chart.ChartDetailActivity_MembersInjector;
import com.leeam.cryptowidget.ui.chart.ChartDetailViewModel;
import com.leeam.cryptowidget.ui.chart.ChartDetailViewModel_HiltModules;
import com.leeam.cryptowidget.ui.chart.ChartDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.leeam.cryptowidget.ui.chart.ChartDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.leeam.cryptowidget.ui.settings.SettingsActivity;
import com.leeam.cryptowidget.ui.settings.SettingsViewModel;
import com.leeam.cryptowidget.ui.settings.SettingsViewModel_HiltModules;
import com.leeam.cryptowidget.ui.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.leeam.cryptowidget.ui.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.leeam.cryptowidget.ui.settings.WidgetConfigActivity;
import com.leeam.cryptowidget.worker.PriceUpdateWorker;
import com.leeam.cryptowidget.worker.PriceUpdateWorker_AssistedFactory;
import com.leeam.cryptowidget.worker.WorkScheduler;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerCryptoWidgetApplication_HiltComponents_SingletonC {
  private DaggerCryptoWidgetApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public CryptoWidgetApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements CryptoWidgetApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public CryptoWidgetApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements CryptoWidgetApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public CryptoWidgetApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements CryptoWidgetApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public CryptoWidgetApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements CryptoWidgetApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CryptoWidgetApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements CryptoWidgetApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CryptoWidgetApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements CryptoWidgetApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public CryptoWidgetApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements CryptoWidgetApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public CryptoWidgetApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends CryptoWidgetApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends CryptoWidgetApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    FragmentCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends CryptoWidgetApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends CryptoWidgetApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    ActivityCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    Map keySetMapOfClassOfAndBooleanBuilder() {
      MapBuilder mapBuilder = MapBuilder.<String, Boolean>newMapBuilder(2);
      mapBuilder.put(ChartDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ChartDetailViewModel_HiltModules.KeyModule.provide());
      mapBuilder.put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide());
      return mapBuilder.build();
    }

    @Override
    public void injectChartDetailActivity(ChartDetailActivity chartDetailActivity) {
      injectChartDetailActivity2(chartDetailActivity);
    }

    @Override
    public void injectSettingsActivity(SettingsActivity settingsActivity) {
    }

    @Override
    public void injectWidgetConfigActivity(WidgetConfigActivity widgetConfigActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(keySetMapOfClassOfAndBooleanBuilder());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @CanIgnoreReturnValue
    private ChartDetailActivity injectChartDetailActivity2(ChartDetailActivity instance) {
      ChartDetailActivity_MembersInjector.injectPrefs(instance, singletonCImpl.widgetPreferencesProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends CryptoWidgetApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    Provider<ChartDetailViewModel> chartDetailViewModelProvider;

    Provider<SettingsViewModel> settingsViewModelProvider;

    ViewModelCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        SavedStateHandle savedStateHandleParam, ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    Map hiltViewModelMapMapOfClassOfAndProviderOfViewModelBuilder() {
      MapBuilder mapBuilder = MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(2);
      mapBuilder.put(ChartDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (chartDetailViewModelProvider)));
      mapBuilder.put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (settingsViewModelProvider)));
      return mapBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.chartDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(hiltViewModelMapMapOfClassOfAndProviderOfViewModelBuilder());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T get() {
        switch (id) {
          case 0: // com.leeam.cryptowidget.ui.chart.ChartDetailViewModel
          return (T) new ChartDetailViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.widgetPreferencesProvider.get());

          case 1: // com.leeam.cryptowidget.ui.settings.SettingsViewModel
          return (T) new SettingsViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.widgetPreferencesProvider.get(), singletonCImpl.cryptoRepositoryImplProvider.get(), singletonCImpl.alertRepositoryProvider.get(), singletonCImpl.workSchedulerProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends CryptoWidgetApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends CryptoWidgetApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends CryptoWidgetApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    Provider<OkHttpClient> provideOkHttpClientProvider;

    Provider<Retrofit> provideKrakenRetrofitProvider;

    Provider<KrakenService> provideKrakenServiceProvider;

    Provider<Retrofit> provideXrplRetrofitProvider;

    Provider<XrplService> provideXrplServiceProvider;

    Provider<CryptoRepositoryImpl> cryptoRepositoryImplProvider;

    Provider<WidgetPreferences> widgetPreferencesProvider;

    Provider<AlertDatabase> provideAlertDatabaseProvider;

    Provider<AlertRepository> alertRepositoryProvider;

    Provider<AlertNotifier> alertNotifierProvider;

    Provider<PriceUpdateWorker_AssistedFactory> priceUpdateWorker_AssistedFactoryProvider;

    Provider<WorkScheduler> workSchedulerProvider;

    SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    AlertDao alertDao() {
      return DatabaseModule_ProvideAlertDaoFactory.provideAlertDao(provideAlertDatabaseProvider.get());
    }

    Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return Collections.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>singletonMap("com.leeam.cryptowidget.worker.PriceUpdateWorker", ((Provider) (priceUpdateWorker_AssistedFactoryProvider)));
    }

    HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 4));
      this.provideKrakenRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 3));
      this.provideKrakenServiceProvider = DoubleCheck.provider(new SwitchingProvider<KrakenService>(singletonCImpl, 2));
      this.provideXrplRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 6));
      this.provideXrplServiceProvider = DoubleCheck.provider(new SwitchingProvider<XrplService>(singletonCImpl, 5));
      this.cryptoRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<CryptoRepositoryImpl>(singletonCImpl, 1));
      this.widgetPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<WidgetPreferences>(singletonCImpl, 7));
      this.provideAlertDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AlertDatabase>(singletonCImpl, 9));
      this.alertRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AlertRepository>(singletonCImpl, 8));
      this.alertNotifierProvider = DoubleCheck.provider(new SwitchingProvider<AlertNotifier>(singletonCImpl, 10));
      this.priceUpdateWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<PriceUpdateWorker_AssistedFactory>(singletonCImpl, 0));
      this.workSchedulerProvider = DoubleCheck.provider(new SwitchingProvider<WorkScheduler>(singletonCImpl, 11));
    }

    @Override
    public void injectCryptoWidgetApplication(CryptoWidgetApplication cryptoWidgetApplication) {
      injectCryptoWidgetApplication2(cryptoWidgetApplication);
    }

    @Override
    public WidgetPreferences widgetPreferences() {
      return widgetPreferencesProvider.get();
    }

    @Override
    public CryptoRepository cryptoRepository() {
      return cryptoRepositoryImplProvider.get();
    }

    @Override
    public AlertRepository alertRepository() {
      return alertRepositoryProvider.get();
    }

    @Override
    public AlertNotifier alertNotifier() {
      return alertNotifierProvider.get();
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private CryptoWidgetApplication injectCryptoWidgetApplication2(
        CryptoWidgetApplication instance) {
      CryptoWidgetApplication_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      CryptoWidgetApplication_MembersInjector.injectWorkScheduler(instance, workSchedulerProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T get() {
        switch (id) {
          case 0: // com.leeam.cryptowidget.worker.PriceUpdateWorker_AssistedFactory
          return (T) new PriceUpdateWorker_AssistedFactory() {
            @Override
            public PriceUpdateWorker create(Context context, WorkerParameters workerParams) {
              return new PriceUpdateWorker(context, workerParams, singletonCImpl.cryptoRepositoryImplProvider.get(), singletonCImpl.widgetPreferencesProvider.get(), singletonCImpl.alertRepositoryProvider.get(), singletonCImpl.alertNotifierProvider.get());
            }
          };

          case 1: // com.leeam.cryptowidget.data.repository.CryptoRepositoryImpl
          return (T) new CryptoRepositoryImpl(singletonCImpl.provideKrakenServiceProvider.get(), singletonCImpl.provideXrplServiceProvider.get());

          case 2: // com.leeam.cryptowidget.data.remote.KrakenService
          return (T) NetworkModule_ProvideKrakenServiceFactory.provideKrakenService(singletonCImpl.provideKrakenRetrofitProvider.get());

          case 3: // @javax.inject.Named("kraken") retrofit2.Retrofit
          return (T) NetworkModule_ProvideKrakenRetrofitFactory.provideKrakenRetrofit(singletonCImpl.provideOkHttpClientProvider.get());

          case 4: // okhttp3.OkHttpClient
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 5: // com.leeam.cryptowidget.data.remote.XrplService
          return (T) NetworkModule_ProvideXrplServiceFactory.provideXrplService(singletonCImpl.provideXrplRetrofitProvider.get());

          case 6: // @javax.inject.Named("xrpl") retrofit2.Retrofit
          return (T) NetworkModule_ProvideXrplRetrofitFactory.provideXrplRetrofit(singletonCImpl.provideOkHttpClientProvider.get());

          case 7: // com.leeam.cryptowidget.data.local.WidgetPreferences
          return (T) new WidgetPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 8: // com.leeam.cryptowidget.data.local.AlertRepository
          return (T) new AlertRepository(singletonCImpl.alertDao());

          case 9: // com.leeam.cryptowidget.data.local.AlertDatabase
          return (T) DatabaseModule_ProvideAlertDatabaseFactory.provideAlertDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 10: // com.leeam.cryptowidget.notifications.AlertNotifier
          return (T) new AlertNotifier(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // com.leeam.cryptowidget.worker.WorkScheduler
          return (T) new WorkScheduler(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
