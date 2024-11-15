package com.hothand.extension;

import com.p6spy.engine.spy.P6DataSource;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.matcher.ElementMatchers;

import javax.sql.DataSource;

import static net.bytebuddy.jar.asm.Opcodes.*;
import static net.bytebuddy.jar.asm.TypeReference.NEW;
import static net.bytebuddy.matcher.ElementMatchers.named;


public class ClassRedefine {
    static {
        //only call once
        ByteBuddyAgent.install();
    }

    public static void redefine() {
        new ByteBuddy()
                .redefine(org.springframework.jdbc.datasource.DelegatingDataSource.class)
                .visit(Advice.to(Decorator.class).on(ElementMatchers.named("setTargetDataSource")))
                .make()
                .load(Thread.currentThread().getContextClassLoader(), ClassReloadingStrategy.fromInstalledAgent())
                .getLoaded();
    }

    static class Decorator {

        @Advice.OnMethodEnter
        public static void enter(@Advice.Argument(value = 0, readOnly = false) DataSource dataSource) {
            dataSource = new P6DataSource(dataSource);
        }
    }


    public static void redefine2() {
        ByteBuddyAgent.install();
        new ByteBuddy()
                .redefine(org.springframework.jdbc.datasource.DelegatingDataSource.class)
                //重写DelegatingDataSource#setTargetDataSource方法
                .method(named("setTargetDataSource"))
                .intercept(MyImplementation.INSTANCE)
                .make()
                .load(Thread.currentThread().getContextClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

    }

    private enum MyAppender implements ByteCodeAppender {

        INSTANCE; // singleton

        /**
         * 修改字节码
         */
        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Implementation.Context implementationContext,
                          MethodDescription instrumentedMethod) {
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(70, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitTypeInsn(NEW, "com/p6spy/engine/spy/P6DataSource");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "com/p6spy/engine/spy/P6DataSource", "<init>", "(Ljavax/sql/DataSource;)V", false);
            methodVisitor.visitFieldInsn(PUTFIELD, "org/springframework/jdbc/datasource/DelegatingDataSource", "targetDataSource", "Ljavax/sql/DataSource;");
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLineNumber(71, label1);
            methodVisitor.visitInsn(RETURN);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLocalVariable("this", "Lorg/springframework/jdbc/datasource/DelegatingDataSource;", null, label0, label2, 0);
            methodVisitor.visitLocalVariable("targetDataSource", "Ljavax/sql/DataSource;", null, label0, label2, 1);
            methodVisitor.visitMaxs(4, 2);
            return new Size(4, 2);
        }
    }


    private enum MyImplementation implements Implementation {

        INSTANCE; // singleton

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return MyAppender.INSTANCE;
        }

    }
}
