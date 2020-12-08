update behandling
set resultat = vilkaarsvurdering.samlet_resultat
from behandling inner join vilkaarsvurdering on behandling.id = vilkaarsvurdering.fk_behandling_id and vilkaarsvurdering.aktiv = true;