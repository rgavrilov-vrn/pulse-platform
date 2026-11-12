package ru.agroeco.pulse.trassir.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class TrassirAsyncConfig {

    /**
     * FIX #40: bounded executor для Trassir @Async. Раньше @Async без явного
     * executor использовал SimpleAsyncTaskExecutor (unbounded threads) —
     * при пиковой нагрузке (несколько серверов агентов) лавина потоков и OOM.
     *
     * <p>core=4, max=8, queue=100. CallerRunsPolicy: при переполнении очередь
     * обрабатывается в вызывающем потоке. Для AFTER_COMMIT-listener'а это
     * безопасно (транзакция уже завершена), замедляет публикацию ingest-ответа,
     * но events не теряются. AbortPolicy → TaskRejectedException → events
     * останутся PENDING → подхватятся stale-sweep'ом через 5 минут.</p>
     */
    @Bean(name = "trassirAsyncExecutor")
    public ThreadPoolTaskExecutor trassirAsyncExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("trassir-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}