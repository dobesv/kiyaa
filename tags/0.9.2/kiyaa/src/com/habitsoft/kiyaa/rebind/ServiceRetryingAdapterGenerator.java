package com.habitsoft.kiyaa.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.RetryController;
import com.habitsoft.kiyaa.util.RetryingOperation;
import com.habitsoft.kiyaa.util.ServiceRetryingAdapter;
import com.habitsoft.kiyaa.util.SimpleRetryController;

public class ServiceRetryingAdapterGenerator extends BaseGenerator {

    @Override
    protected GeneratorInstance createGeneratorInstance() {
        return new RetryGeneratorInstance();
    }

    class RetryGeneratorInstance extends GeneratorInstance {
        JClassType asyncCallbackType;
        
        @Override
        protected void addImports() {
            super.addImports();
            addImport(RetryController.class.getName());
            addImport(RetryingOperation.class.getName());
            addImport(AsyncCallback.class.getName());
        }
        
        @Override
        protected void generateClassBody() throws UnableToCompleteException {
            asyncCallbackType = getType(AsyncCallback.class.getName());
            // Find the interface to implement
            JClassType[] implementedInterfaces = baseType.getImplementedInterfaces();
            for(JClassType iface : implementedInterfaces) {
                JParameterizedType parameterizedType = iface.isParameterized();
                if(parameterizedType == null)
                    continue;
                if(iface.getErasedType().getQualifiedSourceName().equals(ServiceRetryingAdapter.class.getName())) {
                    JClassType[] typeArgs = parameterizedType.getTypeArgs();
                    if(typeArgs.length != 1) {
                        logger.log(TreeLogger.ERROR, "ServiceRetryingAdapter interface must be parameterized to the type of object to clone", null);
                        throw new UnableToCompleteException();
                    }
                    generateInterface(typeArgs[0]);
                    return;
                }
            }
            logger.log(TreeLogger.ERROR, "Failed to find interface ServiceRetryingAdapter; why was this generator even invoked, then?");
        }

        private void generateInterface(JClassType classType) {
            sw.println("protected RetryController controller = new "+SimpleRetryController.class.getName()+"();");
            sw.println("public void setController(RetryController controller) { this.controller = controller; }");
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
            sw.println("new RetryingOperation("+callbackParam.getName()+", controller) {");            
            sw.indent();
            sw.println("public void perform() {");
            sw.indent();
            sw.println("AsyncCallback "+callbackParam.getName()+" = this; // Replace callback parameter with ourself");
            generateDelegateCall(method);
            sw.outdent();
            sw.println("}");
            sw.outdent();
            sw.println("}.perform();");
            sw.outdent();
            sw.println("}");
        }

        private void generateDelegateMethodDecl(JMethod method) {
            sw.print("public "+method.getReturnType()+" "+method.getName()+"(");
            boolean first=true;
            for(JParameter arg : method.getParameters()) {
                if(first) first = false;
                else sw.print(", ");
                sw.print("final ");
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
