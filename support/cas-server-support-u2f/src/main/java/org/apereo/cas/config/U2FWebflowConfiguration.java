package org.apereo.cas.config;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.adaptors.u2f.storage.U2FDeviceRepository;
import org.apereo.cas.adaptors.u2f.web.flow.U2FAccountCheckRegistrationAction;
import org.apereo.cas.adaptors.u2f.web.flow.U2FAccountSaveRegistrationAction;
import org.apereo.cas.adaptors.u2f.web.flow.U2FAuthenticationWebflowAction;
import org.apereo.cas.adaptors.u2f.web.flow.U2FAuthenticationWebflowEventResolver;
import org.apereo.cas.adaptors.u2f.web.flow.U2FMultifactorWebflowConfigurer;
import org.apereo.cas.adaptors.u2f.web.flow.U2FStartAuthenticationAction;
import org.apereo.cas.adaptors.u2f.web.flow.U2FStartRegistrationAction;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.web.cookie.CasCookieBuilder;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowExecutionPlan;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.authentication.RankedMultifactorAuthenticationProviderSelector;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;

import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.webflow.config.FlowDefinitionRegistryBuilder;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link U2FWebflowConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Configuration("u2FWebflowConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class U2FWebflowConfiguration implements CasWebflowExecutionPlanConfigurer {

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("loginFlowRegistry")
    private ObjectProvider<FlowDefinitionRegistry> loginFlowDefinitionRegistry;

    @Autowired
    private FlowBuilderServices flowBuilderServices;

    @Autowired
    @Qualifier("u2fDeviceRepository")
    private ObjectProvider<U2FDeviceRepository> u2fDeviceRepository;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    @Qualifier("authenticationServiceSelectionPlan")
    private ObjectProvider<AuthenticationServiceSelectionPlan> authenticationRequestServiceSelectionStrategies;

    @Autowired
    @Qualifier("centralAuthenticationService")
    private ObjectProvider<CentralAuthenticationService> centralAuthenticationService;

    @Autowired
    @Qualifier("defaultAuthenticationSystemSupport")
    private ObjectProvider<AuthenticationSystemSupport> authenticationSystemSupport;

    @Autowired
    @Qualifier("defaultTicketRegistrySupport")
    private ObjectProvider<TicketRegistrySupport> ticketRegistrySupport;

    @Autowired
    @Qualifier("servicesManager")
    private ObjectProvider<ServicesManager> servicesManager;

    @Autowired
    @Qualifier("multifactorAuthenticationProviderSelector")
    private ObjectProvider<MultifactorAuthenticationProviderSelector> multifactorAuthenticationProviderSelector;

    @Autowired
    @Qualifier("warnCookieGenerator")
    private ObjectProvider<CasCookieBuilder> warnCookieGenerator;


    @Bean
    public FlowDefinitionRegistry u2fFlowRegistry() {
        val builder = new FlowDefinitionRegistryBuilder(this.applicationContext, this.flowBuilderServices);
        builder.setBasePath("classpath*:/webflow");
        builder.addFlowLocationPattern("/mfa-u2f/*-webflow.xml");
        return builder.build();
    }

    @ConditionalOnMissingBean(name = "u2fAuthenticationWebflowAction")
    @Bean
    public Action u2fAuthenticationWebflowAction() {
        return new U2FAuthenticationWebflowAction(u2fAuthenticationWebflowEventResolver());
    }

    @ConditionalOnMissingBean(name = "u2fMultifactorWebflowConfigurer")
    @Bean
    @DependsOn("defaultWebflowConfigurer")
    public CasWebflowConfigurer u2fMultifactorWebflowConfigurer() {
        return new U2FMultifactorWebflowConfigurer(flowBuilderServices,
            loginFlowDefinitionRegistry.getIfAvailable(), u2fFlowRegistry(), applicationContext, casProperties);
    }

    @ConditionalOnMissingBean(name = "u2fStartAuthenticationAction")
    @Bean
    public Action u2fStartAuthenticationAction() {
        return new U2FStartAuthenticationAction(casProperties.getServer().getName(), u2fDeviceRepository.getIfAvailable());
    }

    @ConditionalOnMissingBean(name = "u2fStartRegistrationAction")
    @Bean
    public Action u2fStartRegistrationAction() {
        return new U2FStartRegistrationAction(casProperties.getServer().getName(), u2fDeviceRepository.getIfAvailable());
    }

    @ConditionalOnMissingBean(name = "u2fCheckAccountRegistrationAction")
    @Bean
    public Action u2fCheckAccountRegistrationAction() {
        return new U2FAccountCheckRegistrationAction(u2fDeviceRepository.getIfAvailable());
    }

    @ConditionalOnMissingBean(name = "u2fSaveAccountRegistrationAction")
    @Bean
    public Action u2fSaveAccountRegistrationAction() {
        return new U2FAccountSaveRegistrationAction(u2fDeviceRepository.getIfAvailable());
    }

    @ConditionalOnMissingBean(name = "u2fAuthenticationWebflowEventResolver")
    @Bean
    public CasWebflowEventResolver u2fAuthenticationWebflowEventResolver() {
        return new U2FAuthenticationWebflowEventResolver(authenticationSystemSupport.getIfAvailable(),
            centralAuthenticationService.getIfAvailable(),
            servicesManager.getIfAvailable(),
            ticketRegistrySupport.getIfAvailable(),
            warnCookieGenerator.getIfAvailable(),
            authenticationRequestServiceSelectionStrategies.getIfAvailable(),
            multifactorAuthenticationProviderSelector.getIfAvailable(RankedMultifactorAuthenticationProviderSelector::new),
            applicationEventPublisher,
            applicationContext);
    }

    @Override
    public void configureWebflowExecutionPlan(final CasWebflowExecutionPlan plan) {
        plan.registerWebflowConfigurer(u2fMultifactorWebflowConfigurer());
    }
}
