package com.habitsoft.kiyaa.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.ProfilingCallback;
import com.habitsoft.kiyaa.util.RetryController;
import com.habitsoft.kiyaa.util.ServiceProfilingAdapter;

public class ServiceProfilingAdapterGenerator extends BaseGenerator {

    @Override
    protected GeneratorInstance createGeneratorInstance() {
        return new RetryGeneratorInstance();
    }

    class RetryGeneratorInstance extends GeneratorInstance {
        @Override
        protected void addImports() {
            super.addImports();
            addImport(RetryController.class.getName());
            addImport(ProfilingCallback.class.getName());
            addImport(AsyncCallback.class.getName());
        }
        
        @Override
        protected void generateClassBody() throws UnableToCompleteException {
            // Find the interface to implement
            JClassType[] implementedInterfaces = baseType.getImplementedInterfaces();
            for(JClassType iface : implementedInterfaces) {
                JParameterizedType parameterizedType = iface.isParameterized();
                if(parameterizedType == null)
                    continue;
                if(iface.getErasedType().getQualifiedSourceName().equals(ServiceProfilingAdapter.class.getName())) {
                    JClassType[] typeArgs = parameterizedType.getTypeArgs();
                    if(typeArgs.length != 1) {
                        logger.log(TreeLogger.ERROR, "ServiceProfilingAdapter interface must be parameterized to the type of object to clone", null);
                        throw new UnableToCompleteException();
                    }
                    generateInterface(typeArgs[0]);
                    return;
                }
            }
            logger.log(TreeLogger.ERROR, "Failed to find interface ServiceProfilingAdapter; why was this generator even invoked, then?");
        }

        private void generateInterface(JClassType classType) {
            sw.println("public "+classType.getParameterizedQualifiedSourceName()+" getProxy("+classType.getParameterizedQualifiedSourceName()+" delegate) { return new InterfaceImpl(delegate); }");            
            sw.println("class InterfaceImpl "+(classType.isInterface()!=null?"implements":"extends")+" "+classType.getParameterizedQualifiedSourceName()+" {");
            sw.indent();
            sw.println("private final "+classType.getParameterizedQualifiedSourceName()+" delegate;");
            sw.println("InterfaceImpl("+classType.getParameterizedQualifiedSourceName()+" delegate) { this.delegate = delegate; }");
            generateMethods(classType);
            sw.outdent();
            sw.println("}");
            
        }

        private void generateMethods(JClassType classType) {
            if(classType.getSuperclass() != null) {
                generateMethods(classType.getSuperclass());
            }
            
            for(JMethod method : classType.getMethods()) {
                if(method.isAbstract()) {
                    generateMethod(method);
                }
            }
            
        }

        private void generateMethod(JMethod method) {
            if(method.getParameters().length == 0 
                || !method.getReturnType().equals(JPrimitiveType.VOID)
                || !method.getParameters()[method.getParameters().length-1].getType().getErasedType().getQualifiedSourceName().equals(AsyncCallback.class.getName())) {
                passthroughMethod(method);
                return;
            }
            
            generateDelegateMethodDecl(method);
            sw.indent();
            JParameter callbackParam = method.getParameters()[method.getParameters().length-1];
            sw.println(callbackParam.getName()+" = new ProfilingCallback("+callbackParam.getName()+", \""+method.getEnclosingType().getSimpleSourceName()+"."+method.getName()+"\");");
            generateDelegateCall(method);
            sw.outdent();
            sw.println("}");
        }

        private void generateDelegateMethodDecl(JMethod method) {
            sw.print("public "+method.getReturnType()+" "+method.getName()+"(");
            boolean first=true;
            for(JParameter arg : method.getParameters()) {
                if(first) first = false;
                else sw.print(", ");
                sw.print(arg.getType().getParameterizedQualifiedSourceName());
                sw.print(" ");
                sw.print(arg.getName());
            }
            sw.println(") {");
        }
        
        private void generateDelegateCall(JMethod method) {
            sw.print("delegate."+method.getName()+"(");
            boolean first=true;
            for(JParameter arg : method.getParameters()) {
                if(first) first = false;
                else sw.print(", ");
                sw.print(arg.getName());
            }
            sw.println(");");
        }

        private void passthroughMethod(JMethod method) {
            sw.println(method.getReadableDeclaration(false, true, true, true, true)+" {");
            generateDelegateCall(method);
            sw.println("}");
        }
    }
}
