package com.academy.trafficviolationsystem.core.config;

import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.Mqttv5ClientManager;
import org.springframework.integration.mqtt.inbound.Mqttv5PahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.Mqttv5PahoMessageHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 *  MQTT Configuration for data transport from live cameras/radars and Traffic Violation System
 *
 *  Configuration provides setup for MQTTManager and I/O channels for communications between
 *  Camera and Server with InputChannel, Server and Broker with OutputChannel.
 */

@Configuration
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${app.mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${app.mqtt.username:}")
    private String username;

    @Value("${app.mqtt.password:}")
    private String password;

    @Value("${app.mqtt.client-id:traffic-system-server}")
    private String clientId;

    @Value("${app.mqtt.topic.cameras:cameras/#}")
    private String cameraTopic;

    // ── client manager ────────────────────────────────────────────────────

    @Bean
    public Mqttv5ClientManager mqttClientManager() {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{ brokerUrl });
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);
        options.setCleanStart(false);

        if (!username.isBlank()) {
            options.setUserName(username);
            options.setPassword(password.getBytes());
        }

        Mqttv5ClientManager manager = new Mqttv5ClientManager(options, clientId);
        log.info("MQTT v5 client manager configured for broker: {}", brokerUrl);
        return manager;
    }

    // ── inbound (camera → server) ─────────────────────────────────────────

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public Mqttv5PahoMessageDrivenChannelAdapter mqttInboundAdapter() {
        Mqttv5PahoMessageDrivenChannelAdapter adapter =
                new Mqttv5PahoMessageDrivenChannelAdapter(mqttClientManager(), cameraTopic);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    // ── outbound (server → broker) ────────────────────────────────────────

    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler mqttOutboundHandler() {
        Mqttv5PahoMessageHandler handler =
                new Mqttv5PahoMessageHandler(mqttClientManager());
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }
}