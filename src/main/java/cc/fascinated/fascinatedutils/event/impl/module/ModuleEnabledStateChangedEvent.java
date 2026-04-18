package cc.fascinated.fascinatedutils.event.impl.module;

import cc.fascinated.fascinatedutils.systems.modules.Module;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public record ModuleEnabledStateChangedEvent(Module module, boolean enabled) {}
