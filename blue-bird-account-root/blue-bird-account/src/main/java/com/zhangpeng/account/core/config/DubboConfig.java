package com.zhangpeng.account.core.config;

import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableDubboConfig
@PropertySource("classpath:dubbo/dubbo.properties")
@ImportResource({"classpath*:dubbo/*.xml"})
public class DubboConfig {

    @Value("${dubbo.registry.protocol}")
    private String protocol;
    @Value("${dubbo.registry.address}")
    private String address;

 /*   @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("blue-bird-fission");
        return applicationConfig;
    }*/

    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setProtocol(protocol);
        registryConfig.setAddress(address);
        return registryConfig;
    }

    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20890);
        return protocolConfig;
    }

    @Bean
    public MonitorConfig monitorConfig() {
        MonitorConfig monitorConfig = new MonitorConfig();
        monitorConfig.setProtocol("registry");
        return monitorConfig;
    }


    @Bean
    public ProviderConfig providerConfig() {
        ProviderConfig providerConfig = new ProviderConfig();
        providerConfig.setTimeout(60000);
        providerConfig.setThreads(500);
        providerConfig.setAccepts(1000);
        providerConfig.setThreadpool("fixed");
        providerConfig.setId("com.alibaba.dubbo.config.ProviderConfig");
        return providerConfig;
    }

    //	<dubbo:provider timeout="1000"></dubbo:provider>

    //	<dubbo:monitor address="127.0.0.1:7070"></dubbo:monitor>


}
