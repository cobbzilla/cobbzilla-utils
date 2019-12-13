package org.cobbzilla.util.reflect;

import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@AllArgsConstructor
public class Immutable<T> implements InvocationHandler {

    private final T obj;

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {

        final String mName = m.getName();
        if (!(mName.startsWith("get")
                || mName.startsWith("is")
                || m.getParameterTypes().length > 0
                || Void.class.isAssignableFrom(m.getReturnType())
        )) die("invoke("+obj.getClass().getSimpleName()+"."+mName+"): not a zero-arg getter or returns void: "+mName);

        return m.invoke(obj, args);
    }

    public static <T> T wrap(T thing) {
        final ClassLoader loader = thing.getClass().getClassLoader();
        final Class[] classes = thing.getClass().getInterfaces();
        return (T) Proxy.newProxyInstance(loader, classes, new Immutable(thing));
    }

}
