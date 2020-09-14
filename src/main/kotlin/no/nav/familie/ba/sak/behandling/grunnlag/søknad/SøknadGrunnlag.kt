package no.nav.familie.ba.sak.behandling.grunnlag.søknad

import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "GR_SOKNAD")
data class SøknadGrunnlag(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gr_soknad_seq_generator")
        @SequenceGenerator(name = "gr_soknad_seq_generator", sequenceName = "gr_soknad_seq", allocationSize = 50)
        val id: Long = 0,

        @Column(name = "opprettet_av", nullable = false, updatable = false)
        val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),

        @Column(name = "opprettet_tid", nullable = false, updatable = false)
        val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

        @Column(name = "fk_behandling_id", updatable = false, nullable = false)
        val behandlingId: Long,

        @Column(name = "soknad", nullable = false, columnDefinition = "text")
        var søknad: String,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true
) {

    fun hentSøknadDto(): SøknadDTO {
        return objectMapper.readValue(this.søknad, SøknadDTO::class.java)
    }
}