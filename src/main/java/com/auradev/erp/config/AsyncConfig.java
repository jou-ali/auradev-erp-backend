package com.auradev.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async execution configuration.
 *
 * <p>Defines a shared {@link ThreadPoolTaskExecutor} used by all
 * {@code @Async} methods in the application. The pool is sized conservatively
 * to avoid overwhelming the database connection pool under high load.</p>
 *
 * <p>Note: {@code @EnableAsync} is retained here for explicitness even though
 * {@link com.auradev.erp.ErpApplication} also carries it — the annotation is
 * idempotent and having it on the config class makes the intent clear.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Thread-pool executor exposed as the default async executor bean.
     *
     * <ul>
     *   <li>Core pool size: 4 threads always alive</li>
     *   <li>Max pool size: 8 threads under burst load</li>
     *   <li>Queue capacity: 100 tasks buffered before thread creation above core</li>
     *   <li>Thread name prefix: {@code erp-async-} for easy log filtering</li>
     * </ul>
     *
     * @return configured {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("erp-async-");
        executor.initialize();
        return executor;
    }
}
