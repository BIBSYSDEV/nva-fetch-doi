package no.unit.nva.metadata;

import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.domain.Metadata;

public interface PublicationConverter {

    CreatePublicationRequest getCreatePublicationRequest(Metadata metadata);

}
