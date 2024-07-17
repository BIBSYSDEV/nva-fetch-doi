package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Identity {
    private URI id;
    private String name;
    private String nameType;
    private String orcId;

    public Identity() {
    }

    public Identity(URI id, String name, String nameType, String orcId) {
        this.id = id;
        this.name = name;
        this.nameType = nameType;
        this.orcId = orcId;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameType() {
        return nameType;
    }

    public void setNameType(String nameType) {
        this.nameType = nameType;
    }

    public String getOrcId() {
        return orcId;
    }

    public void setOrcId(String orcId) {
        this.orcId = orcId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Identity identity = (Identity) o;
        return Objects.equals(getId(), identity.getId())
               && Objects.equals(getName(), identity.getName())
               && Objects.equals(getNameType(), identity.getNameType())
               && Objects.equals(getOrcId(), identity.getOrcId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getNameType(), getOrcId());
    }
}
