package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.getCacheOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("integrasjonstest")
class PreprodControllerIntegrasjonstest(
    @Autowired
    val preprodController: PreprodController,
    @Autowired
    @Qualifier("shortCache")
    val cacheManager: CacheManager,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `tømPersonopplysningerCache tømmer cache`() {
        val cache = cacheManager.getCacheOrThrow("personopplysninger")

        cache.put("personopplysninger", "verdi")
        assertThat(cache.get("personopplysninger")?.get()).isEqualTo("verdi")

        val response = preprodController.tømPersonopplysningerCache()
        assertThat(response.body).isEqualTo("Personopplysninger-cache er tømt")
        assertThat(cache.get("personopplysninger")).isNull()
    }
}
