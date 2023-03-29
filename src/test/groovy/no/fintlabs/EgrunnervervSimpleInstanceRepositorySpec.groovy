package no.fintlabs

import no.fintlabs.model.EgrunnervervSimpleInstance
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class EgrunnervervSimpleInstanceRepositorySpec extends Specification {
    EgrunnervervSimpleInstanceRepository repository = new EgrunnervervSimpleInstanceRepository()

    def "put should add an instance to the repository"() {
        given:
        EgrunnervervSimpleInstance instance = EgrunnervervSimpleInstance.builder().sysId("1").build()

        when:
        repository.put(instance)

        then:
        repository.get("1").get() == instance
    }

    def "get should return an instance by its system ID"() {
        given:
        EgrunnervervSimpleInstance instance1 = EgrunnervervSimpleInstance.builder().sysId("1").build()
        EgrunnervervSimpleInstance instance2 = EgrunnervervSimpleInstance.builder().sysId("2").build()
        repository.put(instance1)
        repository.put(instance2)

        when:
        Optional<EgrunnervervSimpleInstance> result = repository.get("2")

        then:
        result.isPresent()
        result.get() == instance2
    }

    def "get should return an empty optional if the instance does not exist"() {
        when:
        Optional<EgrunnervervSimpleInstance> result = repository.get("1")

        then:
        !result.isPresent()
    }

    def "remove should remove an instance from the repository"() {
        given:
        EgrunnervervSimpleInstance instance = EgrunnervervSimpleInstance.builder().sysId("1").build()
        repository.put(instance)

        when:
        repository.remove("1")

        then:
        !repository.get("1").isPresent()
    }

    @Unroll
    @Ignore
    def "get should return #result for system ID #sysId"() {
        given:
        EgrunnervervSimpleInstance instance1 = EgrunnervervSimpleInstance.builder().sysId("1").build()
        EgrunnervervSimpleInstance instance2 = EgrunnervervSimpleInstance.builder().sysId("2").build()
        repository.put(instance1)
        repository.put(instance2)

        when:
        Optional<EgrunnervervSimpleInstance> result = repository.get(sysId)

        then:
        result.isPresent() == expected
        result.orElse(null) == expectedInstance

        where:
        sysId          | expected | expectedInstance
        "1"            | true     | instance1
        "2"            | true     | instance2
        "non-existent" | false    | null
    }
}
