package com.hzt.system.listener;

import com.hzt.file.server.config.FileServerConfig;
import com.hzt.file.server.server.FileServer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartedEventListener implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        FileServerConfig.fileSeverInit();
        FileServer.getInstance().start();
    }
}