package com.vinsys.instrumentation.agent;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Properties;

public class InstrumentationAgent {
	public static final String INSTRUMENTATION_CLASS_KEY = "intrumentationClass";
	public static final String INSTRUMENTATION_METHOD_KEY = "instrucmentationMethod";
	public static final Properties properties = new Properties();
	static {
		try {
			properties.load(new FileInputStream("d:/instrumentation.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("[Agent] In premain method");
		String className = properties.getProperty(INSTRUMENTATION_CLASS_KEY);
		transformClass(className, inst);
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		System.out.println("[Agent] In agentmain method");
		String className = properties.getProperty(INSTRUMENTATION_CLASS_KEY);
		transformClass(className, inst);
	}

	private static void transformClass(String className, Instrumentation instrumentation) {
		Class<?> targetCls = null;
		ClassLoader targetClassLoader = null;
		// see if we can get the class using forName
		try {
			targetCls = Class.forName(className);
			targetClassLoader = targetCls.getClassLoader();
			transform(targetCls, targetClassLoader, instrumentation);
			return;
		} catch (Exception ex) {
			System.out.println("Class [{}] not found with Class.forName");
		}
		// otherwise iterate all loaded classes and find what we want
		for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
			if (clazz.getName().equals(className)) {
				targetCls = clazz;
				targetClassLoader = targetCls.getClassLoader();
				transform(targetCls, targetClassLoader, instrumentation);
				return;
			}
		}
		throw new RuntimeException("Failed to find class [" + className + "]");
	}

	private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation) {
		IntrumentationTransformer dt = new IntrumentationTransformer(clazz.getName(), classLoader);
		instrumentation.addTransformer(dt, true);
		try {
			instrumentation.retransformClasses(clazz);
		} catch (Exception ex) {
			throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
		}
	}

}