package no.fintlabs.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Jacksonized
@Builder(toBuilder = true)
public class EgrunnervervSimpleInstance {

    public enum Type {
        SAK, JOURNALPOST
    }

    private String sysId;
    private String tableName;
    private Type type;

}
