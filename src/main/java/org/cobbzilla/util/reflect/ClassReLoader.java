package org.cobbzilla.util.reflect;

import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.lang.Class.forName;

// adapted from https://stackoverflow.com/a/9192126/1251543
public class ClassReLoader extends URLClassLoader {

    private Set<String> reload = new HashSet<>();
    public void doReloadFor(String classOrPackage) { reload.add(classOrPackage); }

    public ClassReLoader(Collection<String> toReload) {
        super(((URLClassLoader) getSystemClassLoader()).getURLs());
        reload.addAll(toReload);
    }

    @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("java.")) return forName(name);
        if (!reload.contains(name)) {
            String find = name;
            while (find.contains(".")) {
                find = find.substring(0, find.lastIndexOf("."));
                if (reload.contains(find)) {
                    return super.loadClass(name);
                }
            }
            return forName(name);
        }
        return super.loadClass(name);
    }
}
