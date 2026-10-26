package com.tdb.vazhipadu.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tdb.vazhipadu.constant.TimeZones;

@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(TimeZones.IST);
    }
}
