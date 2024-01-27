/*
 * Copyright 2023-2024 the Ken.Zhang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.org.byc.base.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.asm.*;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
public class ReflectionUtils {
    private static Map<Class<?>, Map<Method, String[]>> declaredMethodsParameterNamesCache = new HashMap<>();

    private static Map<Class<?>, List<Method>> methodsInDeclaringOrderCache = new HashMap<>();
    private static Map<Class<?>, List<Method>> declaredMethodsInDeclaringOrderCache = new HashMap<>();
    private static Map<Method, String> methodSignatureCache = new HashMap<>();
    public static List<Method> getMethodsInDeclaringOrder(Class<?> clazz) {
        if (!methodsInDeclaringOrderCache.containsKey(clazz)){
            synchronized (methodsInDeclaringOrderCache) {
                if (!methodsInDeclaringOrderCache.containsKey(clazz)){
                    // todo: 待完善实际获取方法 暂定名称
                    List<Method> result = null;
                    methodsInDeclaringOrderCache.put(clazz,result);
                    return result;
                }
            }
        }
        return methodsInDeclaringOrderCache.get(clazz);
    }

    public static List<Method> getDeclaredMethodsInDeclaringOrder(Class<?> clazz) {
        if (!declaredMethodsParameterNamesCache.containsKey(clazz)){
            synchronized (declaredMethodsParameterNamesCache){
                if (!declaredMethodsInDeclaringOrderCache.containsKey(clazz)) {
                    List<Method> result = doGetDeclaredMethodsInDeclaringOrder(clazz);
                    declaredMethodsInDeclaringOrderCache.put(clazz,result);
                    return result;
                }
            }
        }
        return declaredMethodsInDeclaringOrderCache.get(clazz);
    }

    public static String getMethodSignature(Method method) {
        if (!methodSignatureCache.containsKey(method)){
            synchronized (methodSignatureCache){
                if (!methodSignatureCache.containsKey(method)){
                    String result = doGetMethodSignature(method);
                    methodSignatureCache.put(method,result);
                    return result;
                }
            }
        }
        return methodSignatureCache.get(method);
    }

    private static String doGetMethodSignature(Method method) {
        if (log.isDebugEnabled()){
            log.debug("getMethodSignature {}", method);
        }

        StringBuilder result = new StringBuilder();
        result.append(method.getName());
        result.append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length>0){
            for (Class<?> parameterType : parameterTypes) {
                result.append(ClassUtils.getQualifiedName(parameterType));
                result.append(",");
            }
            result.setLength(result.length()-2);
        }
        result.append(")");
        return result.toString();
    }

    private static List<Method> doGetMethodsInDeclaringOrder(Class<?> clazz) {
        if (log.isDebugEnabled()){
            log.debug("getMethodsInDeclaringOrder {}", clazz);
        }

        List<Method> result = new ArrayList<>();
        Set<String> methodSignatures = new HashSet<>();
        Class<?> tempClass = clazz;
        while (tempClass != null) {
            List<Method> methods = getDeclaredMethodsInDeclaringOrder(tempClass);
            List<Method> temp = new ArrayList<>();
            for (Method method : methods) {
                String methodSignature = getMethodSignature(method);
                if (!methodSignatures.contains(methodSignature)){
                    methodSignatures.add(methodSignature);
                    temp.add(method);
                }
            }
            result.addAll(0, temp);
            tempClass = tempClass.getSuperclass();
        }
        return result;
    }

    private static List<Method> doGetDeclaredMethodsInDeclaringOrder(Class<?> clazz) {
        if (log.isDebugEnabled()){
            log.debug("getDeclaredMethodsInDeclaringOrder {}", clazz);
        }

        final List<Method> result = new ArrayList<>();
        ClassReader classReader;

        final ClassLoader cl = ClassUtils.getDefaultClassLoader();
        try {
            String classResourceName = "/" + clazz.getName().replace(".","/")+".class";
            InputStream classResourceInputSteam = ReflectionUtils.class.getResourceAsStream(classResourceName);
            classReader = new ClassReader(classResourceInputSteam);
        }catch (IOException e){
            throw new RuntimeException("Error read {}" + clazz, e);
        }

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ("<init>".equals(name) || "<clinit>".equals(name)){
                    return null;
                }

                try {
                    Type[] types = Type.getArgumentTypes(descriptor);
                    Class<?>[] parameterTypes = new Class<?>[types.length];
                    for (int i = 0; i < types.length; i++) {
                        parameterTypes[i] = ClassUtils.forName(types[i].getClassName(), cl);
                    }

                    Method method = clazz.getDeclaredMethod(name, parameterTypes);
                    result.add(method);
                    return null;
                }catch (ClassNotFoundException | LinkageError e){
                    log.error("get class name error",e);
                    throw new RuntimeException(e);
                }catch (NoSuchMethodException | SecurityException e){
                    log.error("get deckared method error", e);
                    throw new RuntimeException(e);
                }
            }
        };
        classReader.accept(classVisitor,ClassReader.EXPAND_FRAMES);
        return result;
    }
}
