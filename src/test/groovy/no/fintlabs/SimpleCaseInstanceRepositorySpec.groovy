import no.fintlabs.model.SimpleCaseInstance
import no.fintlabs.SimpleCaseInstanceRepository
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class SimpleCaseInstanceRepositorySpec extends Specification {
    SimpleCaseInstanceRepository repository = new SimpleCaseInstanceRepository()

    def "put should add an instance to the repository"() {
        given:
        SimpleCaseInstance instance = new SimpleCaseInstance(sysId: "1")

        when:
        repository.put(instance)

        then:
        repository.get("1").get() == instance
    }

    def "get should return an instance by its system ID"() {
        given:
        SimpleCaseInstance instance1 = new SimpleCaseInstance(sysId: "1")
        SimpleCaseInstance instance2 = new SimpleCaseInstance(sysId: "2")
        repository.put(instance1)
        repository.put(instance2)

        when:
        Optional<SimpleCaseInstance> result = repository.get("2")

        then:
        result.isPresent()
        result.get() == instance2
    }

    def "get should return an empty optional if the instance does not exist"() {
        when:
        Optional<SimpleCaseInstance> result = repository.get("1")

        then:
        !result.isPresent()
    }

    def "remove should remove an instance from the repository"() {
        given:
        SimpleCaseInstance instance = new SimpleCaseInstance(sysId: "1")
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
        SimpleCaseInstance instance1 = new SimpleCaseInstance(sysId: "1")
        SimpleCaseInstance instance2 = new SimpleCaseInstance(sysId: "2")
        repository.put(instance1)
        repository.put(instance2)

        when:
        Optional<SimpleCaseInstance> result = repository.get(sysId)

        then:
        result.isPresent() == expected
        result.orElse(null) == expectedInstance

        where:
        sysId        | expected | expectedInstance
        "1"          | true     | instance1
        "2"          | true     | instance2
        "non-existent" | false    | null
    }
}
