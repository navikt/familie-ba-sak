package no.nav.familie.ba.sak.integrasjoner

import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import java.io.IOException
import java.time.Duration

fun retryVedException(delayInMs: Long) =
    RetryTemplate(
        RetryPolicy
            .builder()
            .includes(Exception::class.java)
            .maxRetries(3)
            .delay(Duration.ofMillis(delayInMs))
            .build(),
    )

fun retryVedIOException(delayInMs: Long) =
    RetryTemplate(
        RetryPolicy
            .builder()
            .includes(IOException::class.java)
            .maxRetries(3)
            .delay(Duration.ofMillis(delayInMs))
            .build(),
    )
