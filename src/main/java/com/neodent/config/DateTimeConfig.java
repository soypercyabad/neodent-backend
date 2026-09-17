package com.neodent.config;

import com.neodent.shared.constants.AppConstants;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class DateTimeConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(AppConstants.Timezone.AMERICA_LIMA));
    }

    @Bean
    public ZoneId zoneId() {
        return ZoneId.of(AppConstants.Timezone.AMERICA_LIMA);
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(AppConstants.Timezone.AMERICA_LIMA));
    }
}
