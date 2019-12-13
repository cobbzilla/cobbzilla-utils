package org.cobbzilla.util.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

public class OnlyGetters {

    public static <T> T wrap(T thing) {
        return (T) Proxy.newProxyInstance(thing.getClass().getClassLoader(), new Class[]{thing.getClass()}, OnlyGettersInvocationHandler.instance);
    }

    private static class OnlyGettersInvocationHandler implements InvocationHandler {
        public static InvocationHandler instance;
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String name = method.getName();
            if ((args == null || args.length == 0)
                    && (name.startsWith("get") || name.startsWith("is")
                    && !method.getReturnType().equals(Void.class))) {
                return notSupported("immutable object: " + proxy + ", cannot call " + name);
            }
            return method.invoke(proxy, args);
        }
    }

}
