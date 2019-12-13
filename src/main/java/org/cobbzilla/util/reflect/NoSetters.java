package org.cobbzilla.util.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

public class NoSetters {

    public static <T> T wrap(T thing) {
        return (T) Proxy.newProxyInstance(thing.getClass().getClassLoader(), new Class[]{thing.getClass()}, NoSettersInvocationHandler.instance);
    }

    private static class NoSettersInvocationHandler implements InvocationHandler {
        public static InvocationHandler instance;
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().startsWith("set")) return notSupported("immutable object: " + proxy + ", cannot call " + method.getName());
            return method.invoke(proxy, args);
        }
    }

}
