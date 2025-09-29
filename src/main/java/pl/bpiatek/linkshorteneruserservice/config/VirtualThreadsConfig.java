package pl.bpiatek.linkshorteneruserservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

@Configuration
class VirtualThreadsConfig {

    @Bean
    TaskExecutor undertowVirtualThreadExecutor() {
        var virtualThreadExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("undertow-vt-", 0).factory()
        );
        return new TaskExecutorAdapter(virtualThreadExecutor);
    }
}
