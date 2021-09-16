package no.nav.familie.ba.sak.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling

@Profile("!mock-scheduling")
@Configuration
@EnableScheduling
class SchedulingConfig