package no.fintlabs;

import no.fintlabs.model.EgrunnervervSimpleInstance;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class EgrunnervervSimpleInstanceRepository {
    private final Map<String, EgrunnervervSimpleInstance> instances = new HashMap<>();

    public void put(EgrunnervervSimpleInstance egrunnervervSimpleInstance) {
        instances.put(egrunnervervSimpleInstance.getSysId(), egrunnervervSimpleInstance);
    }

    public Optional<EgrunnervervSimpleInstance> get(String sysId) {
        return Optional.ofNullable(instances.get(sysId));
    }

    public void remove(String sysId) {
        instances.remove(sysId);
    }
}
