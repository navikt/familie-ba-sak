package no.nav.familie.ba.sak.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@Profile("prod", "preprod", "e2e")
@EnableScheduling
class SchedulingConfig