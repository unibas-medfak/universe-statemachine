# Universe Statemachine

Universe Statemachine provides a common infrastructure to work with state
machine concepts in Spring applications. It is a fork of
[Spring Statemachine](https://spring.io/projects/spring-statemachine),
repackaged under `ch.unibas.medizin.universe.statemachine` and modernized to
run on Spring Boot 4 and Java 21.

## Modules

| Module | Description |
| --- | --- |
| `universe-statemachine-core` | The core, reactive state machine API and implementation. |
| `universe-statemachine-autoconfigure` | Spring Boot auto-configuration and actuator integration. |
| `universe-statemachine-starter` | Spring Boot starter that pulls in core and auto-configuration. |

## Requirements

- Java 21
- Spring Boot 4.1
- Project Reactor (the state machine API is reactive)

## Getting started

Add the starter to a Spring Boot application:

```xml
<dependency>
    <groupId>ch.unibas.medizin.universe.statemachine</groupId>
    <artifactId>universe-statemachine-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

Define the states and events:

```java
public enum States {
    STATE1, STATE2
}

public enum Events {
    EVENT1, EVENT2
}
```

Configure the machine:

```java
@Configuration
@EnableStateMachine
public class Config extends EnumStateMachineConfigurerAdapter<States, Events> {

    @Override
    public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
        states
            .withStates()
                .initial(States.STATE1)
                .states(EnumSet.allOf(States.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
        transitions
            .withExternal()
                .source(States.STATE1).target(States.STATE2)
                .event(Events.EVENT1)
                .and()
            .withExternal()
                .source(States.STATE2).target(States.STATE1)
                .event(Events.EVENT2);
    }
}
```

Inject the machine and send events:

```java
@Autowired
StateMachine<States, Events> stateMachine;

void doSignals() {
    stateMachine
        .sendEvent(Mono.just(MessageBuilder.withPayload(Events.EVENT1).build()))
        .subscribe();
}
```

## Building

The project builds with the Maven wrapper (Maven 4):

```bash
./mvnw clean verify
```

Publish to Maven Central:

```bash
./mvnw -Prelease deploy
```

## License

Universe Statemachine is Open Source software released under the
[Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).
