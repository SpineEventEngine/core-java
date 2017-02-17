/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.tools.javadoc;

import com.sun.javadoc.Doc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;
import org.spine3.Internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@linkplain Standard} doclet, which excludes {@linkplain Internal}-annotated components.
 * Based on <a href="http://sixlegs.com/blog/java/exclude-javadoc-tag.html">this</a> topic.
 *
 * @author Dmytro Grankin
 */
@SuppressWarnings("ExtendsUtilityClass")
public class ExcludeInternalDoclet extends Standard {

    private final ExcludePrinciple excludePrinciple;

    private ExcludeInternalDoclet(ExcludePrinciple excludePrinciple) {
        super();
        this.excludePrinciple = excludePrinciple;
    }

    /**
     * Entry point for the Javadoc tool.
     *
     * @param args the command-line parameters
     */
    public static void main(String[] args) {
        final String name = ExcludeInternalDoclet.class.getName();
        Main.execute(name, name, args);
    }

    /**
     * The "start" method as required by Javadoc.
     *
     * @param root the root of the documentation tree.
     * @return {@code true} if the doclet ran without encountering any errors.
     */
    @SuppressWarnings("unused") // called by com.sun.tools.javadoc.Main
    public static boolean start(RootDoc root) {
        final ExcludePrinciple excludePrinciple = new ExcludeInternalPrinciple(root);
        final ExcludeInternalDoclet doclet = new ExcludeInternalDoclet(excludePrinciple);
        return Standard.start((RootDoc) doclet.process(root, RootDoc.class));
    }

    private Object process(Object obj, Class expect) {
        if (obj == null) {
            return null;
        }

        if (obj.getClass().getName().startsWith("com.sun.")) {
            final Class cls = obj.getClass();
            return Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), new ExcludeHandler(obj));
        } else if (obj instanceof Object[] && expect.getComponentType() != null) {
            final Class componentType = expect.getComponentType();
            final Object[] array = (Object[]) obj;
            final List<Object> list = new ArrayList<>();
            for (Object entry : array) {
                if (!(entry instanceof Doc && excludePrinciple.exclude((Doc) entry))) {
                    list.add(process(entry, componentType));
                }
            }
            return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
        } else {
            return obj;
        }
    }

    private class ExcludeHandler implements InvocationHandler {

        private final Object target;

        private ExcludeHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null) {
                String methodName = method.getName();
                if ("compareTo".equals(methodName)
                        || "equals".equals(methodName)
                        || "overrides".equals(methodName)
                        || "subclassOf".equals(methodName)) {
                    args[0] = unwrap(args[0]);
                }
            }
            try {
                return process(method.invoke(target, args), method.getReturnType());
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private Object unwrap(Object proxy) {
            if (proxy instanceof Proxy) {
                return ((ExcludeHandler) Proxy.getInvocationHandler(proxy)).target;
            }
            return proxy;
        }
    }
}
