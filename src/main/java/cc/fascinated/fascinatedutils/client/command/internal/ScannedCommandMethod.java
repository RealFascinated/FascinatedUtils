package cc.fascinated.fascinatedutils.client.command.internal;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;

public record ScannedCommandMethod(Method method, MethodHandle handle, List<String> literalPath, List<ArgBinding> args) {}