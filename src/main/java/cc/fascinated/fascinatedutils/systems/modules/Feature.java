package cc.fascinated.fascinatedutils.systems.modules;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Feature<M> {

    private final M module;
}
