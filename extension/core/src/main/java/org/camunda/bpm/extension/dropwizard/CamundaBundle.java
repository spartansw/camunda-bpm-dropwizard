package org.camunda.bpm.extension.dropwizard;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.delegate.DelegateInvocation;
import org.camunda.bpm.engine.impl.interceptor.DelegateInterceptor;
import org.camunda.bpm.extension.dropwizard.application.CamundaServletProcessApplication;

import org.camunda.bpm.extension.dropwizard.function.ActivateJobExecutor;
import org.camunda.bpm.extension.dropwizard.healthcheck.CamundaHealthChecks;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;
import com.google.inject.Injector;

/**
 * The bundle is the hook that a developer of a camunda dropwizard application has to add so the engine
 * and process application are configured correctly.
 */
public class CamundaBundle implements ConfiguredBundle<CamundaConfiguration> {

  private final Logger logger = getLogger(this.getClass());
  private final Injector injector;

  public CamundaBundle(Injector injector) {
      super();
      this.injector = injector;
  }

  public CamundaBundle() {
    this(null);
  }

  @Override
  public void run(final CamundaConfiguration configuration, final Environment environment) throws Exception {
    // @formatter:off
    final ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) configuration
      .overwriteHistoryLevel(environment)
      .setDelegateInterceptor(new DelegateInterceptor() {
        @Override
        public void handleInvocation(DelegateInvocation invocation) throws Exception {
          Object target = invocation.getTarget();
          if (target instanceof JavaDelegate && injector != null) {
              injector.injectMembers(target);
          }
          invocation.proceed();
        }
      })
      .buildProcessEngineConfiguration();
    // @formatter:on

    environment.lifecycle().manage(new ProcessEngineManager(processEngineConfiguration));

    if (configuration.getCamunda().isJobExecutorActivate()) {
      environment.lifecycle().addServerLifecycleListener(ActivateJobExecutor.serverLifecycleListener());
    }
    environment.servlets().addServletListeners(new CamundaServletProcessApplication());

    CamundaHealthChecks.processEngineIsRunning(environment);

    environment.admin().addTask(ActivateJobExecutor.task());
  }



  @Override
  public void initialize(final Bootstrap<?> bootstrap) {
    // nothing to do here, we need access to configuration and we won't have that until run() is executed.
  }

}
