package com.vinsys.instrumentation.agent;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class IntrumentationTransformer implements ClassFileTransformer {

	private static Logger LOGGER = LoggerFactory.getLogger(IntrumentationTransformer.class);

	private static final String METHOD = InstrumentationAgent.properties
			.getProperty(InstrumentationAgent.INSTRUMENTATION_METHOD_KEY);

	private String targetClassName;

	public IntrumentationTransformer(String targetClassName, ClassLoader targetClassLoader) {
		this.targetClassName = targetClassName;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		byte[] byteCode = classfileBuffer;
		String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); // replace . with /
		if (!className.equals(finalTargetClassName)) {
			return byteCode;
		}
		System.out.print(finalTargetClassName);
		System.out.println("[Agent] Transforming class " + className);
		try {
			ClassPool cp = ClassPool.getDefault();
			CtClass cc = cp.get(targetClassName);
			CtMethod m = cc.getDeclaredMethod(METHOD);
			m.addLocalVariable("startTime", CtClass.longType);
			m.insertBefore("startTime = System.currentTimeMillis();");
			StringBuilder endBlock = new StringBuilder();
			m.addLocalVariable("endTime", CtClass.longType);
			m.addLocalVariable("opTime", CtClass.longType);
			endBlock.append("endTime = System.currentTimeMillis();");
			endBlock.append("opTime = (endTime-startTime)/1000;");

			endBlock.append("System.out.println(\"[Application] " + m.getMethodInfo().getName()
					+ " operation completed in:\" + opTime + \" seconds!\");");

			m.insertAfter(endBlock.toString());

			byteCode = cc.toBytecode();
			cc.detach();
		} catch (NotFoundException | CannotCompileException | IOException e) {
			LOGGER.error("Exception", e);
		}
		// }
		return byteCode;
	}
}