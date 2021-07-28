package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(
        "postgres",
        "mock-pdl",
        "mock-localdate-service",
        "mock-oauth",
        "mock-arbeidsfordeling",
        "mock-tilbakekreving-klient",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
)
abstract class AbstractVerdikjedetest : WebSpringAuthTestRunner()