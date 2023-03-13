package no.fintlabs;

import no.fintlabs.model.SimpleCaseInstance;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class SimpleCaseInstanceRepository {
    private final Map<String, SimpleCaseInstance> instances = new HashMap<>();

    public void put(SimpleCaseInstance simpleCaseInstance) {
        instances.put(simpleCaseInstance.getSysId(), simpleCaseInstance);
    }

    public Optional<SimpleCaseInstance> get(String sysId) {
        return Optional.ofNullable(instances.get(sysId));
    }

    public void remove(String sysId) {
        instances.remove(sysId);
    }
}
