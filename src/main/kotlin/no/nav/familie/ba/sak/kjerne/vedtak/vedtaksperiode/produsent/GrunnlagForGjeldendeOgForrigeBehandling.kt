package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent

data class GrunnlagForGjeldendeOgForrigeBehandling(
    val gjeldende: GrunnlagForPerson?,
    val forrige: GrunnlagForPerson?
)
