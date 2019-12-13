package org.cobbzilla.util.reflect;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

public class PoisonProxy {

    public interface PoisonProxyThrow { Throwable getThrowable(Object proxy, Method method, Object[] args); }

    public static <T> T wrap(Class<T> clazz) { return wrap(clazz, null); }

    public static <T> T wrap(Class<T> clazz, PoisonProxyThrow thrower) { return wrap(new Class[]{clazz}, thrower); }

    /**
     * Create a proxy object for a class where calling any methods on the object will result in it throwing an exception.
     * @param clazzes The classes to create a proxy for
     * @param thrower An object implementing the PoisonProxyThrow interface, which produces objects to throw
     * @param <T> The class to create a proxy for
     * @return A proxy to the class that will throw an exception if any methods are called on it
     */
    public static <T> T wrap(Class[] clazzes, PoisonProxyThrow thrower) {
        return (T) Proxy.newProxyInstance(clazzes[0].getClassLoader(), clazzes, thrower == null ? PoisonedInvocationHandler.instance : new PoisonedInvocationHandler(thrower));
    }

    @NoArgsConstructor @AllArgsConstructor
    private static class PoisonedInvocationHandler implements InvocationHandler {
        public static PoisonedInvocationHandler instance = new PoisonedInvocationHandler();
        private PoisonProxyThrow thrower = null;
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (thrower == null) {
                return notSupported("method not supported by poisonProxy: " + method.getName() + " (in fact, NO methods will work on this object)");
            } else {
                throw thrower.getThrowable(proxy, method, args);
            }
        }
    }

}
