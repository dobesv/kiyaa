package com.habitsoft.kiyaa.rebind;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.Text;
import nu.xom.XPathContext;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.metamodel.Value;
import com.habitsoft.kiyaa.views.ModelView;
import com.habitsoft.kiyaa.views.View;
import com.habitsoft.kiyaa.views.ViewFactory;
import com.sun.facelets.util.Classpath;

/**
 * This generator creates a View implementation from an XML/XHTML template.
 * 
 * The template contains a mix of XHTML tags and special tags for the construction of subviews,
 * which come from a tag library.
 * 
 * The format/architecture is based loosely on facelets/JSF, but since we're generating widgets and
 * not text, some things are changed.
 * 
 * Any HTML element can be "converted" into a special tag by specifying kc="ns:tag" which means the
 * template parser treats the whole tag as if it were an <ns:tag ... > element.
 * 
 * Tags which are not part of HTML are converted into a View or Widget subclass,
 * and the attributes of that tag are used to initialize the new view object.
 * 
 * The view provides attributes in the form of setters;
 * 
 * - If a setter takes an Action object, the tag attribute value may be
 *   one or more assignments or method calls seperated by a semicolon.  The
 *   generated code constructs an Action object which executes those 
 *   statements.  The first statement to fail terminates the action sequence;
 *   asynchronous operations ARE supported.  It may also be an expression
 *   for an Action to copy to that place.
 * - If a setter takes a Value object, or a type which isn't automatically
 *   converted from a string by the generated (a non-primitive type), then
 *   the XML attribute value should be an EL-style expression, either
 *   "${...}" or "#{...}".  The expression is evaluated during the template's
 *   LOAD phase and the view's setter is called with the result.  If "#{...}" 
 *   is used, then the view's getter is called during the SAVE phase, and the
 *   setter of the given expression is called with that result.
 * - Other values are converted to the target type on a best-effort basis; for
 *   example an integer, float, or boolean value may be parsed.
 * 
 * Some tag attributes are processed specially:
 * 
 * - The special attribute "binding" allows you to store a reference to the
 *   view object into the given EL expression.  The setter for the expression
 *   is called after constructing the view object from the tag.
 * - The special attributes "onclick" and "onchange" will automatically create
 *   a ClickListener or a ChangeListener on view objects that implements
 *   SourcesClickEvents or SourcesChangeEvents.  The attribute value follows
 *   the syntax of an Action, as above.
 * - The special attributes "class" and "style" will automatically call
 *   the addStyleName() or DOM.setElementAttribute() to apply the given
 *   CSS class or style string to the created view object.
 *
 * When a tag is converted and it contains elements inside it, the following
 * rules apply:
 * 
 * - If the inner tags match methods like setX or addX on the view, those
 *   setters/adders are called once for each element.  The parameters to
 *   the setter or added are calculated as follows:
 *       - Attribute names are matched to parameter names and are 
 *         reordered to match the parameter list
 *       - The special attribute name "class" can be used to match a
 *         method parameter "styleName" because "class" is a java
 *         reserved word.
 *       - The attributes are processed and converted according to the
 *         same rules as calling setters on widgets and views (see above).
 *       - If all parameters are matched to an attribute but one, and the
 *         element has a body (either text or child elements), the body
 *         may be used to fill that parameter as follows:
 *            - If the parameter is a View, Widget, or ViewFactory, the body is
 *              treated as a sub-template
 *            - If the missing parameter is a string, and the method takes just
 *              one parameter or one parameter plus an AsyncCallback, then the
 *              text contents of the element are assumed to be the value of that
 *              parameter
 * - If the View or Widget being constructed has a setView() or setViewFactory(),
 *   the child elements are converted to a sub-template and passed as a view
 *   instance or factory.
 * 
 * The Expression Language has the following features:
 * 
 * - "${...}" refers to a read-only expression, whereas "#{...}" is a read-write
 *   expression
 * - An identifier refers to a property of the template object, accessed by
 *   capitalizing the identifier and prepending "get", "is", or "set" according
 *   to standard bean accessor rules.  Any view with an id="x" attribute is also
 *   available using the assigned id as an identifier.
 * - Dotted expressions can be used to fetch sub-properties; e.g. "a.b.c"
 *   translates to "this.getA().getB().getC()".  The last property may
 *   be asynchronous, but not the intermediate ones (until support for that is
 *   added, I suppose).
 * - Boolean expressions can be constructed using &&, ||, !=, ==, and !.
 * - It also supports the aliases "and", "or", and "not" for &&, ||, and !.
 * - Primitive int values can be made using numbers, like: 123
 * - Primitive long values can be made using the L suffix, like: 1234L
 * - Primitive double values can be made using a decimal number, like: 12.34
 * - Literal strings can be entered using double quotes, like: "hello"
 * - Various automatic conversions will be attempted
 * 
 * Phases:
 * 
 * - Construction: on creation, all the view objects are constructed and
 *   any properties of the views that don't take an expression are
 *   set.
 * - Load: when it is time to display the data from the database, and after
 *   executing any action, load() is called, which reads all the expression 
 *   values and stores them into properties on the views.
 * - Save: before any action is performed, all the #{...} expressions are
 *   set to the matching property from the view object.
 * 
 * Included tags:
 * 
 * <k:view/> All the tags outside this are ignored; this is useful to take pieces of a complete
 * HTML document and make a control out of it without making the page an invalid HTML document.
 * 
 * <k:list value="#{...}"/> The children of the list are repeated once for each item in the
 * collection 
 * 
 * <k:when test="${...}"/> If the condition is false, the contents are removed
 * 
 * <k:when test="${! ...}"/> If the condition is true, the contents are removed
 * 
 * UI pre-defined tags:
 * 
 * <ui:button onclick="${...}"/>
 * 
 * Plus new tags can be defined by creating a tag library file which specifies a mapping from a tag
 * to a View class. The class is constructed and configured at runtime.  See the included
 * META-INF folder for examples.
 * 
 */
public class GeneratedHTMLViewGenerator extends BaseGenerator {
    protected static String escapeMultiline(String input) {
        return escape(input).replace("\\n", "\\n\"+\n\"");
    }

    public static interface TagHandlerApi {
    	
    }
    public static interface TagHandler {
    }
    public static class SimpleTagHandler implements TagHandler {
    	final String contentAttribute;
    	final String viewClassName;
    	final Map<String,String> defaults;
    	
		public SimpleTagHandler(String viewClassName, String contentAttribute, Map<String, String> defaults) {
			super();
			this.viewClassName = viewClassName;
			this.contentAttribute = contentAttribute;
			this.defaults = defaults;
		}

		public JClassType getViewClass(TypeOracle types) throws NotFoundException {
			return types.getType(getViewClassName());
		}

		public String getContentAttribute() {
			return contentAttribute;
		}

		public String getViewClassName() {
			return viewClassName;
		}

		public Map<String, String> getDefaults() {
			return defaults;
		}
    }

    public static class TagLibrary {
    	final HashMap<String,TagHandler> tags = new HashMap<String,TagHandler>();
    	
    	void addSimpleTagHandler(String tag, String viewClassName, String contentAttribute, Map<String, String> defaults) {
    		tags.put(tag, new SimpleTagHandler(viewClassName, contentAttribute, defaults));
    	}

		public TagHandler getHandler(String tag) {
			return tags.get(tag);
		}
    }
    
//    static class Prof {
//    	static String msg;
//    	static long startTime;
//    	static long elapsed() { return (new Date().getTime() - startTime); }
//    	static void start(String msg) {
//    		if(msg == null) {
//    			long t = elapsed();
//    			if(t > 100) {
//    				System.out.println(elapsed()+"ms ...");
//    			}
//    		}
//    		stop();
//    		Prof.msg = msg;
//    	}
//    	static void stop() {
//    		if(msg != null) {
//    			System.out.println(elapsed()+"ms - "+msg);
//    			msg = null;
//    		}
//    		Prof.startTime = new Date().getTime();
//    	}
//    }
    public static class GeneratorInstance extends BaseGenerator.GeneratorInstance {

        static final String KIYAA_CORE_TAGS_NAMESPACE = "http://habitsoft.com/kiyaa/core";
        static final String KIYAA_VIEW_TAGS_NAMESPACE = "http://habitsoft.com/kiyaa/ui";

        static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
        static boolean tagLibrariesLoaded=false;
        static long lastTagLibraryLoad = 0;
        static HashMap<String, TagLibrary> tagLibraries = new HashMap();
        static HashMap<String, String> namespaces = new HashMap();
        protected ClassGenerator rootClassGenerator;
        protected TypeOracle myTypes = new TypeOracle();
        protected Element rootElement;
        private JClassType rootClassType;
        @Override
		public void init() throws UnableToCompleteException {
            super.init();

            // Caching these classes seem to occasionally create some weirdness; we need to
            // re-load the tag libraries each time there is a new compile operation, or we
            // should change it so we can "refresh" them without reparsing the xml files.
            if(!tagLibrariesLoaded) {
            	loadTagLibraries();
            	tagLibrariesLoaded = true;
            	lastTagLibraryLoad = System.currentTimeMillis();
            } else {
            	//refreshTagLibraryClasses();
            }
            
            rootClassGenerator = new ClassGenerator();
            String templatePath = getClassMetadata(baseType, baseType, "kiyaa.template");
            if (templatePath == null) {
                templatePath = getSimpleClassName(baseType, ".") + ".xhtml";
            }
            rootClassType = new JRealClassType(myTypes, myTypes.getOrCreatePackage(baseType.getPackage().getName()), null,
                            false, implName, false);
            rootClassType.setSuperclass(baseType);

            XMLReader reader;
			try {
				reader = XMLReaderFactory.createXMLReader();
			} catch (SAXException caught1) {
				throw new Error(caught1);
			}
            reader.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					// DTDs are just going to slow us down ... sadly
					throw new SAXException("Don't use DTDs, they are evil!");
				}
			});
            nu.xom.Builder b = new nu.xom.Builder(reader);
            Document d;
            java.io.File f;
            JClassType topLevelClass = baseType;
            while(topLevelClass.getEnclosingType() != null) topLevelClass = topLevelClass.getEnclosingType();
			try {
				Class<?> clazzInstance = Class.forName(topLevelClass.getQualifiedSourceName());
                URL resource = clazzInstance.getResource(templatePath);
                if(resource == null) {
                    logger.log(TreeLogger.ERROR, "No template found at "+templatePath);
                    throw new UnableToCompleteException();
                }
                f = new File(resource.toURI());
			} catch (ClassNotFoundException caught1) {
			    logger.log(TreeLogger.ERROR, "Couldn't find class "+topLevelClass+" in order to determine path to template file.", caught1);
				throw new UnableToCompleteException();
			} catch (URISyntaxException caught) {
                logger.log(TreeLogger.ERROR, "Invalid template path: "+templatePath, caught);
				throw new UnableToCompleteException();
			}
            //java.io.File f = new File(new File(baseType.getCompilationUnit().getLocation()).getParentFile(), templatePath);
            try {
                logger.log(TreeLogger.TRACE, "Looking for template as a file with path " + f.getPath(), null);
                if (!f.exists()) {
                    logger.log(TreeLogger.WARN, "Looking for template as a file with path " + f.getPath()
                                    + " failed; looking for template as a resource with path " + templatePath, null);
                    d = b.build(getClass().getResourceAsStream(templatePath));
                } else {
                    d = b.build(f);
                }
            } catch (Exception caught) {
                logger.log(TreeLogger.ERROR, "Failed to load template '" + templatePath + ": "+caught.toString(), null);
                throw new UnableToCompleteException();
            }
            rootElement = getComponentRootElement(d);
            if (rootElement.getAttribute("with-model") != null) {
                final String modelViewClassName = ModelView.class.getName();
                this.composerFactory.addImplementedInterface(modelViewClassName);
                rootClassType.addImplementedInterface(getType(modelViewClassName));
            }
        }

        protected void loadTagLibraries() throws UnableToCompleteException {
            URL[] taglibs;
            try {
                taglibs = Classpath.search("META-INF/", ".kiyaa-taglib.xml");
            } catch (IOException caught) {
                logger.log(TreeLogger.ERROR, null, caught);
                throw new UnableToCompleteException();
            }
            for (int i = 0; i < taglibs.length; i++) {
                URL url = taglibs[i];
                nu.xom.Builder b = new nu.xom.Builder(false);
                try {
                    Document d = b.build(url.openStream());
                    Element root = d.getRootElement();
                    if (!root.getLocalName().equals("kiyaa-taglib")) {
                        continue;
                    }
                    String namespace = root.getFirstChildElement("namespace").getValue();
                    String packagePrefix = "";
                    try {
                        packagePrefix = root.getFirstChildElement("package").getValue() + ".";
                    } catch (NullPointerException npe) {
                    }

                    if(tagLibraries.get(namespace) != null) {
                        //logger.log(TreeLogger.WARN, "Namespace "+namespace+" defined in multiple taglib files, including "+url, null);
                        continue;
                    }
                    TagLibrary library = new TagLibrary();
                    Elements tags = root.getChildElements("tag");
                    for (int j = 0; j < tags.size(); j++) {
                        Element tag = tags.get(j);
                        String tagClass = packagePrefix + tag.getFirstChildElement("tag-class").getValue();
                        String tagName = tag.getFirstChildElement("tag-name").getValue();
						final Element contentAttributeElement = tag.getFirstChildElement("content-attribute");
						String contentAttribute = contentAttributeElement==null?null:contentAttributeElement.getValue();
						Map<String,String> defaults = new TreeMap<String,String>();
						Elements defaultElements = tag.getChildElements("default");
						for(int k=0; k < defaultElements.size(); k++) {
							Element defaultElt = defaultElements.get(k);
							String property = defaultElt.getAttributeValue("for");
							String value = defaultElt.getValue();
							defaults.put(property, value);
						}
						library.addSimpleTagHandler(tagName, tagClass, contentAttribute, defaults);
                    }
                    
                    
                    tagLibraries.put(namespace, library);
                } catch (Throwable caught) {
                    logger.log(TreeLogger.WARN, "Failed to parse taglib at " + url, caught);
                }

            }
        }

        @Override
		protected void addImports() {
            composerFactory.addImport("com.google.gwt.core.client.*");
            composerFactory.addImport("com.google.gwt.user.client.*");
            composerFactory.addImport("com.google.gwt.user.client.rpc.AsyncCallback");
            /*
            composerFactory.addImport("com.google.gwt.user.client.ui.Widget");
            composerFactory.addImport("com.google.gwt.user.client.ui.ClickListener");
            composerFactory.addImport("com.google.gwt.user.client.ui.ChangeListener");
            composerFactory.addImport("com.google.gwt.user.client.ui.FocusListener");
            composerFactory.addImport("com.google.gwt.user.client.ui.FlowPanel");
            */
            composerFactory.addImport("com.google.gwt.user.client.ui.*");
            composerFactory.addImport("com.habitsoft.kiyaa.widgets.*");
            composerFactory.addImport("com.habitsoft.kiyaa.metamodel.*");
            composerFactory.addImport("com.habitsoft.kiyaa.views.*");
            composerFactory.addImport("com.habitsoft.kiyaa.util.*");
        }

        private Element getComponentRootElement(Document d) {
            Element rootElement = d.getRootElement();
            XPathContext context = new XPathContext("k", KIYAA_CORE_TAGS_NAMESPACE);
            Nodes components = d.query("//k:view", context);
            if (components.size() > 0) {
                if (components.size() > 1) {
                    logger.log(TreeLogger.WARN, "Found more than one component; only the first will be used", null);
                }
                rootElement = (Element) components.get(0);
            }
            return rootElement;
        }

        @Override
		protected void generateClassBody() throws UnableToCompleteException {
            rootClassGenerator.generateClassBody(rootElement, rootClassType, false);
        }

        class ClassGenerator {
            HashMap insertedViews = new HashMap();
            HashMap insertedText = new HashMap();
            HashMap values = new HashMap();
            HashMap actions = new HashMap();
            ArrayList<String> ctor = new ArrayList();
            ArrayList<String> memberDecls = new ArrayList();
            ArrayList<String> calculations = new ArrayList();
            ArrayList<String> asyncProxies = new ArrayList();
            ArrayList<String> loads = new ArrayList();
            ArrayList<String> saves = new ArrayList();
            ArrayList<String> clearFields = new ArrayList();
            //ArrayList<String> setModels = new ArrayList();
            ArrayList<Element> subviewClasses = new ArrayList();
            JClassType myModelClass;
            String myModelVarName;
            boolean subviewClass;
            JClassType myClass;
			protected final ArrayList<SubviewInfo> subviews = new ArrayList<SubviewInfo>();
			private boolean hasHtml=false;
			boolean useInnerHTML=false;
			Element myRootElement;
			
			private void generateClassBody(Element rootElement, JClassType myClass, boolean subviewClass)
                            throws UnableToCompleteException {
				myRootElement = rootElement;
				this.myClass = myClass;
                this.subviewClass = subviewClass;
                String withModel = rootElement.getAttributeValue("with-model");
                if (withModel != null) {
                    String[] pieces = withModel.split("\\s+");
                    String modelTypeName;
                    if (pieces.length == 1) {
                        modelTypeName = "java.lang.Object";
                        myModelVarName = pieces[0];
                    } else {
                        modelTypeName = pieces[0];
                        myModelVarName = pieces[1];
                    }
                    this.myModelClass = getType(modelTypeName);
                    String fieldName = myModelVarName;
                    JType fieldType = myModelClass;
                    generateField(fieldName, fieldType);
                } else if (implementsInterface(myClass, getType(ModelView.class.getName()))
                                && (findMethod(myClass.getSuperclass(), "getModel", 0, false) == null || findMethod(myClass
                                                .getSuperclass(), "setModel", 2, false) == null)) {
                    logger.log(TreeLogger.WARN, "Generated views that implement ModelView should define"
                                    + " an attribute with-model='Type modelName'"
                                    + " on their root element ("+rootElement.getQualifiedName()+"), or implement"
                                    + " getModel/setModel in the base class (" + myClass.getName() + ")", null);
                }
                String withVars = rootElement.getAttributeValue("with-vars");
                if(withVars == null) withVars = rootElement.getAttributeValue("with-var");
                if(withVars != null) {
                	String[] vars = withVars.split("\\s*[,;]\\s*");
                	for (int i = 0; i < vars.length; i++) {
						String var = vars[i];
	                    String[] pieces = var.split("\\s+");
	                    String varTypeName;
	                    String fieldName;
	                    if (pieces.length == 1) {
	                        varTypeName = "java.lang.Object";
	                        fieldName = pieces[0];
	                    } else {
	                        varTypeName = pieces[0];
	                        fieldName = pieces[1];
	                    }
	                    
	                    JType fieldType;
	                    try {
	                    	fieldType = types.parse(varTypeName);
	                    } catch(NotFoundException nfe) {
	                    	fieldType = JPrimitiveType.valueOf(varTypeName);
	                    	if(fieldType == null) {
	                    		logger.log(TreeLogger.ERROR, "Can't find any type matching "+varTypeName, null);
	                    		throw new UnableToCompleteException();
	                    	}
	                    } catch (TypeOracleException caught) {
                    		logger.log(TreeLogger.ERROR, "Can't find any type matching "+varTypeName, null);
                    		throw new UnableToCompleteException();
						}
	                    generateField(fieldName, fieldType);
					}
                }
                JClassType enclosingClass = myClass.getEnclosingType();
                if((myClass.isStatic() || subviewClass) && enclosingClass != null) {
                    generateField("my"+enclosingClass.getSimpleSourceName(), enclosingClass);
                }
                if (myModelClass != null && !myModelVarName.equals("model")) {
                    sw.println("public Object getModel() {");
                    sw.indentln("return " + myModelVarName + ";");
                    sw.println("}");
                    new JMethod(myClass, "getModel").setReturnType(getType("java.lang.Object")); 
                    // Provide access to model as "model"
                }
                sw.println("public void validate(AsyncCallback callback) {");
                sw.indentln("callback.onFailure(new Error(\"Not implemented\"));");
                sw.println("}");
                // Make clearFields available as an action
                new JMethod(myClass, "clearFields").setReturnType(JPrimitiveType.VOID); 

                // Two results of this operation:
                // the template string
                // the code to insert widgets into it
                sw.println("public void addFields() {");
                sw.indent();
                
                parseTree(rootElement);
                for (SubviewInfo subviewInfo : subviews) {
                    generateSubview(subviewInfo);
				}
                sw.outdent();
                sw.println("}");

                generatePanel();
                
                generateRemoveFields();

                if(hasHtml)
                	generateTemplate(rootElement);

                generateConstructor(rootElement);
                generateMemberDecls();
                generateLoad();
                generateSave();
                generateClearFields();

                for (Iterator i = asyncProxies.iterator(); i.hasNext();) {
					String line = (String) i.next();
					sw.println(line);
				}
                
                for (Iterator i = calculations.iterator(); i.hasNext();) {
					String line = (String) i.next();
					sw.println(line);
				}
                
                
                if (myModelClass != null)
                    generateSetModel();
                generateSubviewClasses();
            }

			private void generatePanel() {
				if(hasHtml) {
                    sw.println("protected ComplexHTMLPanel panel = new ComplexHTMLPanel();");
                    sw.println("protected void addView(String id, View view) {");
                    sw.indentln("panel.replace(view.getViewWidget(), id);");
                    sw.println("}");
                    sw.println("protected void addWidget(String id, Widget widget) {");
                    sw.indentln("panel.replace(widget, id);");
                    sw.println("}");
                } else {
                    sw.println("protected FlowPanel panel = new FlowPanel();");
                    sw.println("protected void addView(String id, View view) {");
                    sw.indentln("panel.add(view.getViewWidget());");
                    sw.println("}");
                    sw.println("protected void addWidget(String id, Widget widget) {");
                    sw.indentln("panel.add(widget);");
                    sw.println("}");
                }
                sw.println("public Widget getViewWidget() {");
                sw.indentln("return panel;");
                sw.println("}");
			}

            protected void generateField(String fieldName, JType fieldType) {
            	JParameterizedType parameterized = fieldType.isParameterized();
            	if(parameterized != null) fieldType = parameterized.getErasedType();
                final JField field = new JField(this.myClass, fieldName);
                field.setType(fieldType);
                JMethod getter = new JMethod(this.myClass, "get" + capitalize(fieldName));
                getter.setReturnType(fieldType);
                memberDecls.add(getter + "{ return " + fieldName + "; }");
                final JMethod setter = new JMethod(this.myClass, "set" + capitalize(fieldName));
                setter.setReturnType(JPrimitiveType.VOID);
                new JParameter(setter, fieldType, fieldName);
                memberDecls.add(setter + "{ this." + fieldName + " = " + fieldName + "; }");
                memberDecls.add(fieldType.getQualifiedSourceName() + " " + fieldName + ";");
            }

            protected void generateConstructor(Element rootElement) throws UnableToCompleteException {
                // Try to add what fields we can, although the model may be
                // null, we might have other fields.
                // In fact, this form might operate perfectly well on a null
                // model.
            	String ctorArgs = "";
            	final String enclosingClassName = subviewClass?myClass.getEnclosingType().getSimpleSourceName():null;
				if(subviewClass) {
            		ctorArgs = enclosingClassName+" my"+enclosingClassName;            		
            	}
                sw.println("public " + myClass.getSimpleSourceName() + "("+ctorArgs+") {");
                sw.indent();
                
            	if(subviewClass) {
                    sw.println("this.my"+enclosingClassName+" = my"+enclosingClassName+";");
            	}
            	Attribute styleClass = rootElement.getAttribute("class");
            	if(styleClass != null) {
            	    generateSetClass(getType(Widget.class.getName()), "panel", styleClass.getValue());
                    rootElement.removeAttribute(styleClass);
            	}
            	Attribute styleDefn = rootElement.getAttribute("style");
            	if(styleDefn != null) {
            	    generateSetStyle(getType(Widget.class.getName()), "panel", "style", styleDefn.getValue());
            	    rootElement.removeAttribute(styleDefn);
            	}
                sw.outdent();
                sw.println("}");
                
                sw.println("boolean didInit=false;");
                sw.println("private void init() {");
                sw.indent();
                sw.println("if(didInit) return;");
            	if(hasHtml) {
            		if(useInnerHTML) {
	                    sw.println("panel.setTemplate(TEMPLATE);");
	                } else {
	                    sw.println("panel.setDomTemplate(generateDomTree());");
	                }
                }
                sw.println("didInit = true;");
                sw.println("addFields();");
                // if(!usesModel && subviewClass) {
                // sw.println("setModel(null, null);");
                // }
                if (!subviewClass)
                    generateAttributes(rootElement, baseType, "this");
                sw.outdent();
                sw.println("}");
            }

            
            int elementCount=0;
            private void generateTemplate(Element rootElement) {
            	if(useInnerHTML) {
                    StringBuffer templ = new StringBuffer();
                    for (int i = 0; i < rootElement.getChildCount(); i++) {
                        templ.append(rootElement.getChild(i).toXML());
                    }
                    sw.println("static final String TEMPLATE = \"" + escapeMultiline(templ.toString().trim()) + "\";");
                } else {
                	sw.println("static private Element generateDomTree() {");
                	sw.indent();
                    String rootEltVar = writeElement(rootElement, null);
                    sw.println("return "+rootEltVar+";");
                	sw.outdent();
                	sw.println("}");
            	}
            }

			private String writeElement(Node child, String parentEltVar) {
				if(child instanceof Element) {
					Element e = (Element)child;
    				String eltVar = "e"+elementCount;
    				elementCount++;
    				String nodeName = e.getLocalName();
    				if(!e.getNamespaceURI().equals(XHTML_NAMESPACE))
    					nodeName = "DIV";
    				if("DIV".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createDiv();");
    				else if("DIV".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createSpan();");
    				else if("TABLE".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createTable();");
    				else if("TBODY".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createTBody();");
    				else if("THEAD".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createTHead();");
    				else if("TFOOT".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createTFoot();");
    				else if("TH".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createTH();");
    				else if("TD".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createTD();");
    				else if("TR".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createTR();");
    				else if("LABEL".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createLabel();");
    				else if("FIELDSET".equalsIgnoreCase(nodeName))
    					sw.println("Element "+eltVar+" = DOM.createFieldSet();");
    				else
    					sw.println("Element "+eltVar+" = DOM.createElement(\""+nodeName+"\");");
    				if(e.getNamespaceURI().equals(XHTML_NAMESPACE)) {
        				for(int i=0; i < e.getAttributeCount(); i++) {
        					Attribute a = e.getAttribute(i);
        					// TODO Attribute substitutions
        					// TODO Escape strings
        					if(a.getLocalName().equalsIgnoreCase("class")) {
        						sw.println("DOM.setElementProperty("+eltVar+", \"className\", \""+backslashEscape(a.getValue())+"\");");
        					} else {
        						sw.println("DOM.setElementAttribute("+eltVar+", \""+a.getLocalName()+"\", \""+backslashEscape(a.getValue())+"\");");
        					}
        				}
    				}
    				for(int i=0; i < e.getChildCount(); i++) {
    					sw.indent();
    					String childVar = writeElement(e.getChild(i), eltVar);
    					if(childVar != null)
    						sw.println("DOM.appendChild("+eltVar+", "+childVar+");");
    					sw.outdent();
    				}
    				return eltVar;
				} else if(child instanceof Text) {
					Text e = (Text)child;
					Element parentElement = (Element)child.getParent();
					String text = e.getValue();
					if(!text.trim().isEmpty()) {
    					if(parentElement != null && parentElement.getChildCount() == 1 && parentEltVar != null) {
        					sw.println("DOM.setInnerText("+parentEltVar+", \""+backslashEscape(child.getValue())+"\");");
        					return null;
    					} else { 
    						//logger.log(TreeLogger.WARN, "Wrapping text into a SPAN .... "+text, null);
    						// TODO GWT doesn't include a createTextNode() so we have to put text into a span sometimes.  However,
    						// that sucks because it can screw up the rendering of the page :(
            				String eltVar = "t"+elementCount;
            				elementCount++;
            				sw.println("Element "+eltVar+" = DOM.createElement(\"span\");");
        					sw.println("DOM.setInnerText("+eltVar+", \""+backslashEscape(text)+"\");");
        					return eltVar;
    					}
					}
				}
				return null;
			}

			private void generateMemberDecls() {
				for (String line : memberDecls) {
                    sw.println(line.replaceAll("<[^>]+>", "")); // stripping generics
                }
			}

            private void generateRemoveFields() {
                sw.println("public void removeFields() {");
                sw.indent();
                sw.println("// TODO remove all fields");
                sw.println("// didInit = false;");
                sw.outdent();
                sw.println("}");
            }

            private void generateLoad() throws UnableToCompleteException {
            	String name;
            	JMethod loadMethod = findMethod(myClass, "load", 0, true);
            	if(loadMethod != null) {
                	if(loads.isEmpty())
                		return;
            		name="loadImpl";
            		sw.println("public void load(AsyncCallback callback) {");
            		sw.indent();
            		sw.println("init();");
            		if(loadMethod.getParameters().length == 1)
            			sw.println("super.load(new AsyncCallbackProxy(callback) { public void onSuccess(Object result) { "+name+"(this.callback); } });");
            		else
            			sw.println("super.load();");
                    sw.outdent();
            		sw.println("}");
            	} else {
            		name = "load";
            	}
                sw.println("public void "+name+"(final AsyncCallback callback) {");
                sw.indent();
                if(loadMethod == null)
                    sw.println("init();");
                if(loads.isEmpty()) {
                	sw.println("callback.onSuccess(null);");
                } else {
                    sw.println("try {");
                    sw.indent();
                    sw.println("final AsyncCallbackGroup group = new AsyncCallbackGroup();");
                    for (String load : loads) {
                        sw.println(load);
                    }
                    sw.println("group.ready(callback);");
                    sw.outdent();
                    sw.println("} catch(Throwable t) {");
                    sw.indentln("callback.onFailure(t);");
                    sw.println("}");
                }
                sw.outdent();
                sw.println("}");
            }

            private void generateSave() throws UnableToCompleteException {
            	String name;
            	boolean hasSave=(findMethod(myClass,"save", new JType[] {getType(AsyncCallback.class.getName())}) != null);
            	if(hasSave) {
                	if(saves.isEmpty())
                		return;
            		name="saveImpl";
            		sw.println("public void save(AsyncCallback callback) {");
            		sw.indent();
            		sw.println("super.save(new AsyncCallbackProxy(callback) { public void onSuccess(Object result) { "+name+"(this.callback); } });");
                    sw.outdent();
            		sw.println("}");
            	} else {
            		name = "save";
            	}
                sw.println("public void "+name+"(final AsyncCallback callback) {");
                sw.indent();
                if(saves.isEmpty()) {
                	sw.println("callback.onSuccess(null);");
                } else {
                    sw.println("if(!didInit) return;");
                    sw.println("try {");
                    sw.indent();
                    sw.println("AsyncCallbackGroup group = new AsyncCallbackGroup();");
                    for (String save : saves) {
                        sw.println(save);
                    }
                    sw.println("group.ready(callback);");
                    sw.outdent();
                    sw.println("} catch(Throwable t) {");
                    sw.indentln("callback.onFailure(t);");
                    sw.println("}");
                }
                sw.outdent();
                sw.println("}");
            }
            private void generateClearFields() throws UnableToCompleteException {
                sw.println("public void clearFields() {");
                sw.indent();
                sw.println("if(!didInit) return;");
            	JMethod superMethod = findMethod(myClass.getSuperclass(), "clearFields", 0, false);
            	if(superMethod != null && !superMethod.isAbstract()) {
        			sw.println("super.clearFields();");
            	}
                for (String clearField : clearFields) {
                    sw.println(clearField);
                }
                sw.outdent();
                sw.println("}");
            }


            private void generateSetModel() throws UnableToCompleteException {
                sw.println("public void setModel(final Object model, AsyncCallback callback) {");
                sw.indent();                
                sw.println("try {");
                sw.indent();
                sw.println("init();");
                sw.println("if(model == null) { callback.onFailure(new NullPointerException()); return; }");
                sw.println("this." + myModelVarName + " = (" + myModelClass.getQualifiedSourceName() + ") model;");
//                sw.println("AsyncCallbackGroup group = new AsyncCallbackGroup();");
//                for (String call : setModels) {
//                    sw.println(call);
//                }
//                sw.println("group.ready(callback);");
                sw.println("load(callback);");
                sw.outdent();
                sw.println("} catch(Throwable t) {");
                sw.indentln("callback.onFailure(t);");
                sw.println("}");
                sw.outdent();
                sw.println("}");
            }

            private void generateSubviewClasses() throws UnableToCompleteException {
                for (int i = 0; i < subviewClasses.size(); i++) {
                    Element elem = subviewClasses.get(i);
                    String subviewClassName = myClass.getSimpleSourceName()+"Subview" + i;
                    
                    pushLogger("Inside subview "+i+" element "+elem.getQualifiedName()+" class name "+subviewClassName);
                    try {
                        boolean isModelView = elem.getAttribute("with-model") != null;
                        sw.println("protected static class " + subviewClassName
                                        + " implements "+(isModelView?"ModelView":"View")+" {");
                        sw.indent();
                        JClassType genClass = new JRealClassType(myTypes, myTypes.getOrCreatePackage(baseType
                                        .getPackage().getName()), myClass, false, subviewClassName, false);
                        if(isModelView)
                            genClass.addImplementedInterface(getType(ModelView.class.getName()));
                        genClass.setSuperclass(getType("java.lang.Object"));
                        genClass.addModifierBits(0x00000010 /*TypeOracle.MOD_STATIC*/); // HACK, don't know how to set static otherwise
                        new ClassGenerator().generateClassBody(elem, genClass, true);
                        /*
                        sw.println("public static ViewFactory getFactory(final "+myClass.getSimpleSourceName()+" my"+myClass.getSimpleSourceName()+") {");
                        sw.indent();
                        sw.println("return new ViewFactory() {");
                        sw.indent();
                        sw.println("public View createView() {");
                        sw.indent();
                        sw.println("return new " + subviewClassName + "(my"+myClass.getSimpleSourceName()+");");
                        sw.outdent();
                        sw.println("}");
                        sw.outdent();
                        sw.println("};");
                        sw.outdent();
                        sw.println("}");
                        */
                        
                        sw.outdent();
                        sw.println("}");
                    } finally {
                    	popLogger();
                    }
                }
            }

            protected void parseTree(Element rootElement) throws UnableToCompleteException {
                for (int i = 0; i < rootElement.getNamespaceDeclarationCount(); i++) {
                    String prefix = rootElement.getNamespacePrefix(i);
                    String uri = rootElement.getNamespaceURI(prefix);
                    namespaces.put(prefix, uri);
                }
                pushLogger("Inside tag "+rootElement.getQualifiedName());
                try {
                    for (int i = 0; i < rootElement.getChildCount(); i++) {
                        Node childNode = rootElement.getChild(i);
                        if(childNode instanceof Text) {
                        	if(!childNode.getValue().trim().isEmpty())
                        		hasHtml = true;
                        	handleTextSubstitution((Text)childNode);
                        	continue;
                        }
                        if (!(childNode instanceof Element)) {
                        	hasHtml = true;
                            continue;
                        }
                        Element elem = (Element) childNode;
                        
                        String[] namespaceAndTag = getNamespaceAndTag(elem);
                        
                        String namespace = namespaceAndTag[0];
                        String tag = namespaceAndTag[1];
                        if (namespace.equals(XHTML_NAMESPACE)) {
                        	hasHtml = true;
                        	if(useInnerHTML) {
                                if (!"br".equals(tag) && !"hr".equals(tag) && !"input".equals(tag) && !"button".equals(tag)) {
                                    if (elem.getChildCount() == 0) {
                                        elem.appendChild("");
                                    }
                                }
                        	}
                            parseTree(elem);
                            continue;
                        }
                        JClassType tagClass = getTagClass(elem);
                        Element viewElem = new Element("div", XHTML_NAMESPACE);
                        String id = elem.getAttributeValue("id");
                        if (id == null)
                            id = "view" + insertedViews.size();
                        else {
                            new JField(myClass, id).setType(tagClass);
                            new JMethod(myClass, "get" + capitalize(id))
                                            .setReturnType(tagClass);
                            // new JParameter(new JMethod(myClass,
                            // "set"+capitalize(myModelVarName), 0, 0, 0, 0),
                            // myModelClass, myModelVarName);
                        }
    
                        viewElem.addAttribute(new Attribute("id", id));
                        viewElem.appendChild(""); 
                        // Need open/close tag, innerHTML doesn't support XML
                        rootElement.replaceChild(elem, viewElem);
                        insertedViews.put(id, elem);
                        if (tagClass != null) {
                        	subviews.add(new SubviewInfo(elem, id, namespace, tag, tagClass));
                        }
                    }
                } finally {
                	popLogger();
                }
            }

			protected String[] getNamespaceAndTag(Element elem) {
				String[] namespaceAndTag;
				
				String ns = elem.getNamespaceURI();
				String ln = elem.getLocalName();
				String kc = elem.getAttributeValue("kc");
				if (kc != null) {
				    String[] split = kc.split(":", 1);
				    if (split.length == 1) {
				    	ln = kc;
				    } else {
				    	ns = (String) namespaces.get(split[0]);
				    	ln = split[1];
				    }
				}
				namespaceAndTag = new String[] {ns,ln};
				return namespaceAndTag;
			}

			protected JClassType getTagClass(Element elem) throws UnableToCompleteException {
				String[] namespaceAndTag = getNamespaceAndTag(elem);
				String namespace = namespaceAndTag[0];
				String tag = namespaceAndTag[1];
				JClassType tagClass = null;
				if (tag.equals("custom") && namespace.equals(KIYAA_VIEW_TAGS_NAMESPACE)) {
				    String viewClassName = elem.getAttributeValue("viewClass");
				    if (viewClassName != null) {
				        try {
				            tagClass = getType(viewClassName);
				        } catch(UnableToCompleteException e) {
				            logger.log(TreeLogger.ERROR, "Couldn't find custom view class: " + viewClassName,
				                            null);
				            throw e;
				        }
				    } else {
				        logger.log(TreeLogger.ERROR, "custom tag must specify viewClass=, in: " + elem.toXML(),
				                        null);
				        throw new UnableToCompleteException();
				    }
				} else {
					TagLibrary tagLibrary = (TagLibrary) tagLibraries.get(namespace);
				    if (tagLibrary != null) {
				    	TagHandler th = tagLibrary.getHandler(tag);
				    	if(th == null) {
				            logger.log(TreeLogger.ERROR, "No tag '" + tag + "' found in tag library "
				                + namespace, null);
				            throw new UnableToCompleteException();
				    	} else /* if(th instanceof SimpleTagHandler) */ {
				    		final SimpleTagHandler simpleTagHandler = ((SimpleTagHandler)th);
							try {
								tagClass = simpleTagHandler.getViewClass(types);
							} catch (NotFoundException caught) {
				                logger.log(TreeLogger.ERROR, "No class found for tag '" + tag + "' found in tag library "
				                    + namespace, caught);
				                throw new UnableToCompleteException();
							}
							String contentAttribute = simpleTagHandler.getContentAttribute();
							if(contentAttribute != null && elem.getAttribute(contentAttribute) == null && elem.getValue().trim().length()>0) {
								elem.addAttribute(new Attribute(contentAttribute, elem.getValue()));
							}
							
							// Apply default attributes
							for(Map.Entry<String,String> e : simpleTagHandler.getDefaults().entrySet()) {
								if(elem.getAttribute(e.getKey()) == null)
									elem.addAttribute(new Attribute(e.getKey(), e.getValue()));
							}
				    	}
				    } else {
				        logger.log(TreeLogger.WARN, "Namespace \"" + namespace
				                        + "\" not recognized, and not the XHTML namespace (" + XHTML_NAMESPACE
				                        + ")", null);
				    }
				}
				return tagClass;
			}

            
            private void handleTextSubstitution(Text childNode) throws UnableToCompleteException {
            	String text = collapseWhitespace(childNode.getValue());
            	try {
            		pushLogger("Processing text: \""+text+"\"");
            		childNode.setValue(text);
            		
                	Matcher matcher = Pattern.compile("[$#]\\{((\\\\\\}|[^}])*)\\}").matcher(text);
                    int textMarker = 0;
                    StringBuffer stringBuildExpr = new StringBuffer();
                	while(matcher.find()) {
                		
                        if(stringBuildExpr.length() > 0) stringBuildExpr.append(" + ");
                        if(matcher.start() > 0)
                        	stringBuildExpr.append('"').append(backslashEscape(text.substring(textMarker, matcher.start()))).append("\" + ");
                        
                		String path = matcher.group(1);
                        stringBuildExpr.append(path);
                        
                    	textMarker = matcher.end();
                	}
                	
                	ParentNode parent = childNode.getParent();
    				if(stringBuildExpr.length() > 0) {
                		String id;
                		// TODO If the parent element is the root of the view, this doesn't work right
                		if(parent != myRootElement && parent.getChildCount() == 1) {
                			Element parentElement = ((Element)parent);
    						id = parentElement.getAttributeValue("id");
                			if(id == null) {
                        		id = "interpolation"+insertedText.size();
                        		parentElement.addAttribute(new Attribute("id", id));
                        		insertedText.put(id, id);
                			}
                		} else {
                    		id = "interpolation"+insertedText.size();
                    		insertedText.put(id, id);
                            Element element = new Element("span", XHTML_NAMESPACE);
                            element.addAttribute(new Attribute("id", id));
                            element.appendChild("REPLACEME");
                            parent.replaceChild(childNode, element);
                		}
                		if(textMarker < text.length()) {
                			stringBuildExpr.append(" + \"").append(backslashEscape(text.substring(textMarker))).append('"');
                		}
                        
                        JClassType fieldType = getType("java.lang.String");
                        final JField field = new JField(this.myClass, id);
    					field.setType(fieldType);
                        final JMethod setter = new JMethod(this.myClass, "set" + capitalize(id));
                        setter.setReturnType(JPrimitiveType.VOID);
                        new JParameter(setter, fieldType, id);
                        memberDecls.add(setter + "{ panel.setText(\"" + id + "\", " + id + "); }");
                        methodsListCache.remove(this.myClass.toString());
                        
                        //System.out.println("Using string build expression: "+stringBuildExpr+" for "+text);
                		ExpressionInfo expr = findAccessors(stringBuildExpr.toString(), true, true);
                		if(expr == null) {
                			logger.log(TreeLogger.ERROR, "Unable to resolve expression: "+stringBuildExpr, null);
                			throw new UnableToCompleteException();
                		}
                        //System.out.println("Got getter: "+expr.getter);
                		ExpressionInfo textExpr = findAccessors(id, false, false);
                		if(textExpr == null) throw new Error("Weird, couldn't find the field I just added: "+id+" to hold text for "+stringBuildExpr+" even though I added a method "+setter+" and a field "+field+"?");
                		if(expr.isConstant() && expr.getter != null) {
                			sw.println(textExpr.copyStatement(expr));
                		} else {
                			loads.add(textExpr.asyncCopyStatement(expr, "group.member()", true));
                		}
                	}
            	} finally {
            		popLogger();
            	}
			}

			private String collapseWhitespace(String value) {
				return value.replaceAll("([\\s\n])\\s+", " ");
			}

			public class SubviewInfo {
				public Element elem;
				public String id;
				public String namespace;
				public String tag;
				public JClassType subviewClass;

				public SubviewInfo(Element elem, String id, String namespace, String tag, JClassType subviewClass) {
					this.elem = elem;
					this.id = id;
					this.namespace = namespace;
					this.tag = tag;
					this.subviewClass = subviewClass;
				}
			}

			protected void generateSubview(SubviewInfo sv) throws UnableToCompleteException {
                // Some other kind of view
                // just construct it and call the setters/getters
				pushLogger("Generate subview of "+myClass+": "+sv.elem);
				try {
                    generateField(sv.id, sv.subviewClass);
                    if (sv.subviewClass.isAbstract()) {
                        sw.println(sv.id + " = (" + sv.subviewClass.getQualifiedSourceName() + ") GWT.create("
                                        + sv.subviewClass.getQualifiedSourceName() + ".class);");
                    } else {
                        sw.println(sv.id + " = new " + sv.subviewClass.getQualifiedSourceName() + "();");
                    }
                    generateContents(sv.elem, sv.subviewClass, sv.id);
                    generateAttributes(sv.elem, sv.subviewClass, sv.id);
                    generateSubviewCommon(sv.elem, sv.id, sv.id, "this", sv.subviewClass, false);
				} finally {
					popLogger();
				}
            }

            private void generateSubviewCommon(Element elem, String id, String viewExpr, String modelExpr,
                            JClassType viewClass, boolean readOnly) throws UnableToCompleteException {
            	String addMethod = viewClass.isAssignableTo(getType(Widget.class.getName()))?"addWidget":"addView";
                sw.println(addMethod+"(\"" + id + "\", " + viewExpr + ");");
                if (getClassMetadata(viewClass, "kiyaa.staticView") == null) {
                    boolean canLoad = implementsInterface(viewClass, getType(View.class.getName()));
                    boolean canSave = canLoad;
//                    boolean canSetModel = implementsInterface(viewClass, getType(ModelView.class.getName()));
                    JMethod[] methods = viewClass.getOverridableMethods();
                    for (int i = 0; i < methods.length; i++) {
                        JMethod method = methods[i];
                        if (method.getName().equals("load")
                                        && method.getParameters().length == 1
                                        && method.getParameters()[0].getType().getQualifiedSourceName().equals(AsyncCallback.class.getName())) {
                            canLoad = true;
                        } else if (method.getName().equals("save")
                                        && method.getParameters().length == 1
                                        && method.getParameters()[0].getType().getQualifiedSourceName().equals(AsyncCallback.class.getName())) {
                            canSave = true;
//                        } else if (method.getName().equals("setModel")
//                                        && method.getParameters().length == 2
//                                        && implementsInterface(
//                                                        method.getParameters()[1].getType().isClassOrInterface(),
//                                                        getType(AsyncCallback.class.getName()))) {
//                            canLoad = true;
                        }
                    }
                    if (canLoad) {
                        loads.add(viewExpr + ".load(group.member());");
                    }
                    if (!readOnly) {
                        if (canSave)
                            saves.add(viewExpr + ".save(group.member());");
                    }
                    if(canLoad) {
                    	clearFields.add(viewExpr + ".clearFields();");
                    }
                    //if (canSetModel)
                    //    setModels.add(viewExpr + ".setModel(" + modelExpr + ", group.member());");

                }
            }

            protected void generateAttributes(Element elem, JClassType type, String name)
                            throws UnableToCompleteException {
                for (int j = 0; j < elem.getAttributeCount(); j++) {
                    final Attribute attr = elem.getAttribute(j);
                    final String key = attr.getLocalName();

                    // These keys are taken care of in an earlier step
                    if ("viewClass".equals(key) || "id".equals(key) || "with-model".equals(key) || "with-vars".equals(key) || "with-var".equals(key))
                        continue;

                    // final String prefix = attr.getNamespacePrefix();
                    final String value = attr.getValue();
                    generateAttribute(type, name, key, value);
                    
                    // Don't let "class" and "style" propagate to the subview since they'll be
                    // applied to THIS view
                    if("class".equals(key) || "style".equals(key)) {
                        elem.removeAttribute(attr);
                        j--;
                    }
                }

            }

			private void generateAttribute(JClassType type, String name, final String key, final String value)
				throws UnableToCompleteException {
				final ExpressionInfo attributeAccessors = findAccessors(type, name, key, true);
				boolean readOnly = false;
				boolean isExpr=false;
				String path = null;
				ExpressionInfo pathAccessors = null;
				if (((readOnly = value.startsWith("${")) || value.startsWith("#{")) && value.endsWith("}")) {
				    path = value.substring(2, value.length() - 1).trim();
				    pathAccessors = findAccessors(path, false, true);
				    isExpr=true;
				}
				String valueExpr;
				if ("class".equals(key)) {
                    generateSetClass(type, name, value);
                } else if ("style".equals(key)) {
                    generateSetStyle(type, name, key, value);
                } else if ("binding".equals(key)) {
                    generateBinding(type, name, value);
                } else if (attributeAccessors == null) {
	                if ("onclick".equals(key)) {
	                    generateOnClickHandler(type, name, value, pathAccessors);
	                } else if ("onchange".equals(key)) {
	                    generateOnChangeListener(type, name, value, pathAccessors);
	                } else if ("onfocus".equals(key)) {
	                    generateOnFocusListener(type, name, value, pathAccessors);
	                } else if ("onblur".equals(key)) {
	                    generateOnBlurListener(type, name, value, pathAccessors);
	                } else if ("onPressEnter".equalsIgnoreCase(key)) {
	                    generateKeyPressHandler(type, name, value, "KEY_ENTER");
	                } else if ("onPressSpace".equalsIgnoreCase(key)) {
	                    generateKeyPressHandler(type, name, value, "' '");
	                } else if ("onPressEscape".equalsIgnoreCase(key)) {
	                    generateKeyPressHandler(type, name, value, "KEY_ESCAPE");
	                } else if ("onKeyPress".equalsIgnoreCase(key)) {
	                    generateKeyPressHandler(type, name, value, null);
	                } else {
	                    logger.log(TreeLogger.ERROR, "Unable to find a property '" + key + "' in " + type + "; value is '" + value + "'", null);
	                    throw new UnableToCompleteException();
	                }
				} else if (!attributeAccessors.hasSetter()) {
				    logger.log(TreeLogger.ERROR, "Unable to find a setter for attribute '" + key + "' in "
				                    + type + "; value is '" + value + "', getter is "+attributeAccessors.getter+" asyncGetter is "+attributeAccessors.asyncGetter, null);
				    throw new UnableToCompleteException();
				} else if (attributeAccessors.type.equals(getType(Action.class.getName()))
					        && (pathAccessors == null || !pathAccessors.hasGetter()) 
					        && (valueExpr = getAction(value)) != null) {
					if(attributeAccessors.setter==null) {
						logger.log(TreeLogger.ERROR, "Async setters do not support for Actions yet.", null);
						throw new UnableToCompleteException();
					}
				    sw.println(attributeAccessors.setter + "(" + valueExpr + ");");
				} else if (path != null &&
					        attributeAccessors.type.equals(getType(Value.class.getName()))
					        && (valueExpr = getFieldValue(path)) != null) {
					if(attributeAccessors.setter==null) {
						logger.log(TreeLogger.ERROR, "Async setters not supported for Values yet.", null);
						throw new UnableToCompleteException();
					}
				    sw.println(attributeAccessors.setter + "(" + valueExpr + ");");
				} else if (pathAccessors != null && (pathAccessors.getter != null || pathAccessors.asyncGetter != null)) {
				    generateAttributeLoadSave(type, attributeAccessors, pathAccessors, readOnly);
				} else if (path != null && (valueExpr = getFieldValue(path)) != null) {
					ExpressionInfo valueAccessors = findAccessors(getType(Value.class.getName()), valueExpr, "value", true);
					generateAttributeLoadSave(type, attributeAccessors, valueAccessors, readOnly);
				} else if(isExpr) {
					logger.log(TreeLogger.WARN, "Couldn't figure out how to set attribute "+key+" on "+type+"; couldn't find a getter for "+value, null);                    	
				} else if (attributeAccessors.type.equals(getType("java.lang.String"))) {
					ExpressionInfo valueAccessors = new ExpressionInfo("\"" + value + "\"", getType("java.lang.String"), true);
					generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true);
				} else if (attributeAccessors.type.equals(getType("java.lang.Boolean"))) {
				    if (!"true".equals(value) && !"false".equals(value)) {
				        logger.log(TreeLogger.ERROR, "Boolean attribute '" + key + "' should be true or false; got '"+value+"'",
				                        null);
				        throw new UnableToCompleteException();
				    }
					ExpressionInfo valueAccessors = new ExpressionInfo("Boolean." + value.toUpperCase(), attributeAccessors.type, true);
					generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true);
				} else if (attributeAccessors.type == JPrimitiveType.BOOLEAN) {
				    if (!"true".equals(value) && !"false".equals(value)) {
				        logger.log(TreeLogger.ERROR, "Boolean attribute '" + key + "' should be true or false; got '"+value+"'",
				                        null);
				        throw new UnableToCompleteException();
				    }
					ExpressionInfo valueAccessors = new ExpressionInfo(value, attributeAccessors.type, true);
					generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true);
				} else if (attributeAccessors.type == JPrimitiveType.CHAR) {
					ExpressionInfo valueAccessors = new ExpressionInfo("'"+value+"'", attributeAccessors.type, true);
					generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true);					
				} else if (attributeAccessors.type.isPrimitive() != null) {
					ExpressionInfo valueAccessors = new ExpressionInfo(value, attributeAccessors.type, true);
					generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true);
				} else if (attributeAccessors.type.equals(getType("java.lang.Class"))) {
				    try {
				    	ExpressionInfo valueAccessors = new ExpressionInfo(types.getType(value).getQualifiedSourceName()
				            + ".class", attributeAccessors.type, true);
				    	generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true);
				    } catch (NotFoundException caught) {
				        logger.log(TreeLogger.ERROR, "Unable to find class '" + value + "' for class attribute '"
				                        + key + "'", null);
				        throw new UnableToCompleteException();
				    }
				} else {
					if(path != null) {
				    	logger.log(TreeLogger.WARN, "Couldn't figure out how to set attribute "+key+" on "+type+"; couldn't find a getter for "+value, null);                    		
					} else {
						logger.log(TreeLogger.WARN, "Couldn't figure out how to set attribute "+key+" on "+type+" with value "+value+" (did you forget to use ${...}?)", null);
					}
				}
			}

			private String generateSetStyle(JClassType type, String name, final String key, final String value)
				throws UnableToCompleteException {
				// Assuming that we either have a View or a Widget ...
				String widgetExpr;
				if (implementsInterface(type, getType(View.class.getName()))) {
				    widgetExpr = name + ".getViewWidget()";
				} else if (type.isAssignableTo(getType(Widget.class.getName()))) {
				    widgetExpr = name;
				} else {
				    logger.log(TreeLogger.ERROR, "Don't know how to set the style of a " + type, null);
				    throw new UnableToCompleteException();
				}
				sw.println("DOM.setElementAttribute(" + widgetExpr + ".getElement(), \"" + escape(key)
				                + "\", \"" + escape(value) + "\");");
				return widgetExpr;
			}

			private String generateSetClass(JClassType type, String name, final String value)
				throws UnableToCompleteException {
				String widgetExpr;
				if (implementsInterface(type, getType(View.class.getName()))) {
				    widgetExpr = name + ".getViewWidget()";
				} else if (type.isAssignableTo(getType(Widget.class.getName()))) {
				    widgetExpr = name;
				} else {
				    logger.log(TreeLogger.ERROR, "Don't know how to set the style of a " + type, null);
				    throw new UnableToCompleteException();
				}
				String[] styleNames = value.split("\\s+");
				for (int i = 0; i < styleNames.length; i++) {
				    String string = styleNames[i];
				    sw.println(widgetExpr + "."+(i==0?"set":"add")+"StyleName(\"" + escape(string) + "\");");
				    
				}
				return widgetExpr;
			}

			private void generateBinding(JClassType type, String name, String value)
				throws UnableToCompleteException {
				if(value.startsWith("${") || value.startsWith("#{")) value = value.substring(2);
				if(value.endsWith("}")) value = value.substring(0, value.length()-1);
				
				ExpressionInfo accessors = findAccessors(value, false, false);
				if (accessors != null && accessors.setter != null) {
				    sw.println(accessors.copyStatement(new ExpressionInfo(name, type, false)));
				} else {
				    logger.log(TreeLogger.WARN, "Unable to find a "+(accessors == null?"property":"setter method")+" for binding expression: " + value, null);
				}
			}

			private String generateOnChangeListener(JClassType type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!implementsInterface(type, getType(SourcesChangeEvents.class.getName()))) {
				    logger.log(TreeLogger.ERROR, "onchange attribute must be on a View/Widget" +
				    		" that implements SourcesChangeEvents.", null);
				    throw new UnableToCompleteException();
				}
				String actionExpr = getAction(value);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value+" for an onchange handler", null);
				    throw new UnableToCompleteException();
				}
				attachWidgetEventListener(name, actionExpr, "ChangeListener", "onChange(Widget sender)", null);
				return actionExpr;
			}

			private String generateOnFocusListener(JClassType type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!implementsInterface(type, getType(SourcesFocusEvents.class.getName()))) {
				    logger.log(TreeLogger.ERROR, "onfocus attribute must be on a View/Widget" +
				    		" that implements SourcesFocusEvents.", null);
				    throw new UnableToCompleteException();
				}
				String actionExpr = getAction(value);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value+" for an onfocus handler", null);
				    throw new UnableToCompleteException();
				}
				sw.println(name + ".add" + "FocusListener" + "(new " + "FocusListener" + "() {");
				sw.indent();
				sw.println("public void " + "onFocus(Widget sender)" + " {");
				sw.indent();
				sw.println(actionExpr + ".perform(AsyncCallbackFactory.defaultNewInstance());");
				sw.outdent();
				sw.println("}");
				sw.println("public void " + "onLostFocus(Widget sender)" + " { }");
				sw.outdent();
				sw.println("});");
				return actionExpr;
			}

			private String generateOnBlurListener(JClassType type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!implementsInterface(type, getType(SourcesFocusEvents.class.getName()))) {
				    logger.log(TreeLogger.ERROR, "onblur attribute must be on a View/Widget" +
				    		" that implements SourcesFocusEvents.", null);
				    throw new UnableToCompleteException();
				}
				String actionExpr = getAction(value);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value+" for an onblur handler", null);
				    throw new UnableToCompleteException();
				}
				sw.println(name + ".add" + "FocusListener" + "(new " + "FocusListener" + "() {");
				sw.indent();
				sw.println("public void " + "onLostFocus(Widget sender)" + " {");
				sw.indent();
				sw.println(actionExpr + ".perform(AsyncCallbackFactory.defaultNewInstance());");
				sw.outdent();
				sw.println("}");
				sw.println("public void " + "onFocus(Widget sender)" + " { }");
				sw.outdent();
				sw.println("});");
				return actionExpr;
			}
			
			private String generateOnClickHandler(JClassType type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!implementsInterface(type, getType(SourcesClickEvents.class.getName()))) {
				    logger.log(TreeLogger.ERROR, "onclick attribute must be on a View/Widget" +
				    		" that implements SourceClickEvents.", null);
				    throw new UnableToCompleteException();
				}
				String actionExpr = getAction(value);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value
				        +" for an onclick handler", null);
				    throw new UnableToCompleteException();
				}
				attachWidgetEventListener(name, actionExpr, "ClickListener", "onClick(Widget sender)", null);
				return actionExpr;
			}
			private String generateKeyPressHandler(JClassType type, String name, final String value, String keyName)
			throws UnableToCompleteException {
    			if(!implementsInterface(type, getType(SourcesClickEvents.class.getName()))) {
    			    logger.log(TreeLogger.ERROR, name+" attribute must be on a View/Widget" +
    			    		" that implements SourcesKeyEvents.", null);
    			    throw new UnableToCompleteException();
    			}
    			String actionExpr = getAction(value);
    			if(actionExpr == null) {
    			    logger.log(TreeLogger.ERROR, "Unable to find action for "+value
    			        +" for a onPressXXX handler", null);
    			    throw new UnableToCompleteException();
    			}
    			final String condition = keyName==null?null:"keyCode == "+keyName+" && (modifiers & ~MODIFIER_SHIFT) == 0";
                attachWidgetEventListener(name, actionExpr, "KeyboardListenerAdapter", "onKeyPress(Widget sender, char keyCode, int modifiers)", condition);
    			return actionExpr;
    		}
			protected void generateAttributeLoadSave(JClassType type, ExpressionInfo attributeAccessors, ExpressionInfo pathAccessors,
				boolean readOnly)
				throws UnableToCompleteException {
				String loadExpr = attributeAccessors.asyncCopyStatement(pathAccessors, "group.member()", true);
				// Put the value into the widget on load()
				//if(attributeAccessors.getter != null && attributeAccessors.getType().equals(getType(String.class.getName())) && pathAccessors.getter != null) {
					// It turns out that calling setText() and setValue to the same value is a high-cost operation
				//	loadExpr = "if(!"+attributeAccessors.getter+".equals("+pathAccessors.conversionExpr(attributeAccessors.getType())+")) { "+loadExpr+" }";
				//}
				// Constant values stored to a non-async setter are set during initialization
				boolean constantLoad = pathAccessors.isConstant() 
					&& pathAccessors.getter != null 
					&& attributeAccessors.setter != null;
				if(constantLoad)
					sw.println(loadExpr);
				else
					loads.add(loadExpr);
				if (!readOnly) {
				    if (attributeAccessors.getter == null && attributeAccessors.asyncGetter == null) {
				        logger.log(TreeLogger.ERROR, "Missing matching getter for attribute '"+attributeAccessors.setter+"' on "+type+"; use ${} to set the value only.  Value is "+pathAccessors, null);
				        throw new UnableToCompleteException();
				    } else if(pathAccessors.setter == null && pathAccessors.asyncSetter == null) {
				        logger.log(TreeLogger.ERROR, "Missing matching setter for '" + (pathAccessors.getter==null?pathAccessors.asyncGetter:pathAccessors.getter) + "' for attribute '"+attributeAccessors.setter+"' on "+type+"; use ${} to set the value only.", null);
				        throw new UnableToCompleteException();
				    }
				    
				    // If it's a two-way affair, copy the value back on save()
				    String saveExpr = pathAccessors.asyncCopyStatement(attributeAccessors, "group.member()", true);
				    saves.add(saveExpr);
				}
			}

            private void attachWidgetEventListener(String name, String valueExpr, String listenerClass,
                            String listenerMethod, String condition) {
                String adder = "add"+listenerClass;
                if(adder.endsWith("Adapter")) adder = adder.substring(0, adder.length()-7);
            	sw.println(name + "." + adder + "(new " + listenerClass + "() {");
                sw.indent();
                sw.println("public void " + listenerMethod + " {");
                sw.indent();
                if(condition != null) {
                	sw.println("if("+condition+")");
                	sw.indent();
                }
                sw.println(valueExpr + ".perform(AsyncCallbackFactory.defaultNewInstance());");
                if(condition != null)
                	sw.outdent();
                sw.outdent();
                sw.println("}");
                sw.outdent();
                sw.println("});");
            }

            private void generateContents(Element elem, JClassType type, String name)
                            throws UnableToCompleteException {
                // Now, if there are nested tags, decide what to do with them
                // We do this before the attributes, because we want to call
                // setViewFactory() before setting the other
                // attributes. Many of the classes started their life with
                // ViewFactory as a parameter to the ctor and
                // don't behave well if they don't have a child view yet.
                Elements childElems = elem.getChildElements();
                if (elem.getChildCount() > 0) {
            		boolean found_setter=false;
                	if(childElems.size() > 0) {
                    	// TODO Currently this is order-dependent for overloads :-(
                		JMethod[] methods = getAllMethods(type);
                		ArrayList<Element> missing_setter=new ArrayList<Element>();
                    	for(int i=0; i < childElems.size(); i++) {
                    		Element childElem = childElems.get(i);
                    		String setMethod = "set"+capitalize(childElem.getLocalName());
                    		String addMethod = "add"+capitalize(childElem.getLocalName());
                    		boolean elemProcessed = false;
                    		for (int j = 0; j < methods.length; j++) {
    							JMethod method = methods[j];
    							//System.out.println("Looking for "+setMethod+" or "+addMethod+" for "+elem+" found "+method);
    							if(!method.getName().equalsIgnoreCase(setMethod) &&
    								!method.getName().equalsIgnoreCase(addMethod)) {
    								continue;
    							}
    							found_setter = true;
    							final JParameter[] parameters = method.getParameters();
    							String[] paramStrings = new String[parameters.length];
    							boolean generatedSubview = false;
    							HashSet usedAttributes = new HashSet();
    							for (int k = 0; k < parameters.length; k++) {
    								JParameter parameter = parameters[k];
    								String parameterName = parameter.getName();
    								String attribute = parameterName;
    								String value = childElem.getAttributeValue(attribute);
    								if("styleName".equals(parameterName) && value == null) {
    									attribute = "class";
    									value = childElem.getAttributeValue(attribute);
    								}
    								if(value == null) {
    									String parameterTypeName = parameter.getType().getQualifiedSourceName();
    									final boolean isView = parameterTypeName.equals(View.class.getName());
    									final boolean isViewFactory = parameterTypeName.equals(ViewFactory.class.getName());
    									final boolean isWidget = parameterTypeName.equals(Widget.class.getName());
    									final boolean isString = parameterTypeName.equals(String.class.getName());
    									final boolean asyncSetter = k == 0 && parameters.length == 2 && AsyncCallback.class.getName().equals(parameters[1].getType().getQualifiedSourceName());
    									final boolean setter = parameters.length == 1;
    									if((isView || isWidget || isViewFactory) && childElem.getChildCount() > 0 && !generatedSubview) {
    										generatedSubview = true;
    									} else if((isString || asyncSetter || setter) && childElem.getValue().trim().length() > 0 && !generatedSubview) {
    										// Okay, we'll take it, I guess
    									} else {
    										logger.log(TreeLogger.TRACE, "Couldn't match an attribute for parameter "+parameterName, null);
    										found_setter = false;
    										break;
    									}
    								} else {
    									usedAttributes.add(attribute);
    								}
    							}
    							if(usedAttributes.size() < childElem.getAttributeCount()) {
    								logger.log(TreeLogger.TRACE, "Not all attributes used from "+childElem+" to call "+method+":", null);
    								for (int k = 0; k < childElem.getAttributeCount(); k++) {
    									if(!usedAttributes.contains(childElem.getAttribute(k).getLocalName())) {
    										logger.log(TreeLogger.TRACE, "   Attribute "+childElem.getAttribute(k).getLocalName()+" was not used.", null);
    									}
    								}
    								continue; // Not all attributes used, so this isn't the one they wanted
    							}
    							if(!found_setter)
    								continue;
    							for (int k = 0; k < parameters.length; k++) {
    								JParameter parameter = parameters[k];
    								String parameterName = parameter.getName();
    								String value = childElem.getAttributeValue(parameterName);
    								if("styleName".equals(parameterName) && value == null) value = childElem.getAttributeValue("class");
    								String paramString = calculateParameterValueExpressionForView(elem, childElem,
										method, parameters, k, parameter, parameterName, value);
    								paramStrings[k] = paramString;
    							}
    							sw.println(name+"."+method.getName()+"("+joinWithCommas(0, paramStrings)+");");
    							elemProcessed = true;
    							break;
    						}
                    		if(!elemProcessed) {
                    			missing_setter.add(childElem);
                    		}
                    	}
                    	if(found_setter && !missing_setter.isEmpty()) {
                    		for(Element childElem : missing_setter) {
                    			logger.log(TreeLogger.WARN, "Discarded "+childElem+"; couldn't find any matching setter.", null);
                    		}
                    	}
                	}
                    // Support:
                    // setView() - set a view directly
                    // setViewFactory() - set a view factory (used by lists)
                	if(!found_setter) {
                        boolean factory = findMethod(type, "setViewFactory", 1, false)!=null;
                        boolean widget = !factory && findMethod(type, "setWidget", 1, false)!=null;
                    	String createViewExpr = generateCreateSubview(elem, factory, false);
						if (factory) {
                            sw.println(name + ".setViewFactory("+createViewExpr+");");
						} else if(widget) {
						    String fieldName = "subviewInPanel"+memberDecls.size();
                            generateField(fieldName, getType(View.class.getName()));
                            sw.println(fieldName + " = " + createViewExpr + ";");
							sw.println(name + ".setWidget("+fieldName+".getViewWidget());");
							loads.add(fieldName + ".load(group.member());");
                            saves.add(fieldName + ".save(group.member());");
                            clearFields.add(fieldName + ".clearFields();");
                        } else if (findMethod(type, "setView", 1, false) != null
                        	 || (elem.getAttribute("with-model") != null && findMethod(type, "setView", 1, false) != null)) {
                            sw.println(name + ".setView("+createViewExpr+");");                        	
                        } else if(childElems.size() > 0){
                            logger.log(TreeLogger.WARN,
                                "Discarding child elements of "
                                + elem
                                + " because the view class doesn't have setView(), addView(), or setViewFactory(), and the child element tags don't match any setX() or addX() on "+type,
                                null);
                        }
                	}
                }
            }

            /**
             * Return an expression to create a view for the given tag.  This might be a generated
             * subview (ComplexHTMLPanel), a widget, or a view.
             * 
             * By default this returns an expression of type View, pass factory or widget == true (but not both)
             * to get a ViewFactory or a Widget instead.
             * 
             * @param elem
             * @param factory If true, return an expression of type ViewFactory
             * @param widget If true, return an expression of type Widget
             * @return
             * @throws UnableToCompleteException
             */
			private String generateCreateSubview(Element elem, boolean factory, boolean widget) throws UnableToCompleteException {
				assert !(factory && widget);
				
				// TODO In order to resurrect this feature, I need to get the simple views to support load/save
//				boolean hasText=false;
//				boolean hasElements=false;
//				boolean isSimple;
//				if(factory || elem.getAttribute("with-model") != null || elem.getAttribute("with-var") != null || elem.getAttribute("with-vars") != null) {
//					isSimple = false;
//				} else {
//					isSimple = true;
//					for(int i=0; i < elem.getChildCount(); i++) {
//						Node elemChild = elem.getChild(i);
//						if(elemChild instanceof Text) {
//							if(((Text)elemChild).getValue().trim().length() > 0) {
//								if(hasElements || hasText) isSimple = false;
//								else hasText = true;
//							}
//						} else if(elemChild instanceof Element) {
//							String[] namespaceAndTag = getNamespaceAndTag((Element)elemChild);
//							if(namespaceAndTag[0].equals(XHTML_NAMESPACE)) {
//								// If it has HTML tags in it, it's a complicated one
//								isSimple = false;
//							} else {
//								if(hasElements || hasText) isSimple = false;
//								else hasElements = true;
//							}
//						} else {
//							isSimple = false;
//						}
//					}
//				}
				String createViewExpr;
//				if(isSimple) {
//					if(hasText) {
//						String text = elem.getValue();
//						String widgetCtor = "new HTML(\""+escapeMultiline(text)+"\")";
//						if(widget) return widgetCtor;
//				    	createViewExpr = "new WidgetWrapperView("+widgetCtor+")";
//					} else {
//						Element childElem = elem.getChildElements().get(0);
//						// Has to have one or the other or we wouldn't get here
//						JClassType tagClass = getTagClass(childElem);
//						if(tagClass.isAbstract()) {
//							createViewExpr = "("+tagClass.getQualifiedSourceName()+")GWT.create("+tagClass.getQualifiedSourceName()+".class)";
//						} else {
//							createViewExpr = "new "+tagClass.getQualifiedSourceName()+"()";
//						}
//						if(!implementsInterface(tagClass, getType(View.class.getName()))) {
//							if(widget)
//								return createViewExpr;
//							
//							// Assume it's a widget if it isn't a view, since tags must be either a widget or a View
//							createViewExpr = "new WidgetWrapperView("+createViewExpr+")";
//						}
//					}
//				} else {
					String className = addSubviewClass(elem);
					createViewExpr = "new "+className+"("+myClass.getQualifiedSourceName()+".this)";
//				}
				if(factory)
					createViewExpr = "new ViewFactory() { public View createView() { return "+createViewExpr+"; } }";
				else if(widget)
					createViewExpr += ".getViewWidget()";
				return createViewExpr;
			}

			private String calculateParameterValueExpressionForView(Element elem, Element childElem, JMethod method,
				final JParameter[] parameters, int k, JParameter parameter, String parameterName, String value)
				throws UnableToCompleteException {
				String paramString;
				if(value == null) {
					String parameterTypeName = parameter.getType().getQualifiedSourceName();
					final boolean isView = parameterTypeName.equals(View.class.getName());
					final boolean isViewFactory = parameterTypeName.equals((ViewFactory.class.getName()));
					final boolean isWidget = parameterTypeName.equals((Widget.class.getName()));
					final boolean isString = parameterTypeName.equals((String.class.getName()));
					final boolean asyncSetter = k == 0 && parameters.length == 2 && AsyncCallback.class.getName().equals(parameters[1].getType().getQualifiedSourceName());
					final boolean setter = parameters.length == 1;
					if(isView || isWidget || isViewFactory) {
						String withModel = elem.getAttributeValue("with-model");
						if(withModel != null)
							childElem.addAttribute(new Attribute("with-model", withModel));
				        paramString = generateCreateSubview(childElem, isViewFactory, isWidget);
					} else if((isString || asyncSetter || setter) && childElem.getValue().trim().length() > 0) {
						paramString = '"'+escape(childElem.getValue().trim())+'"';
					} else {
						logger.log(TreeLogger.ERROR, "Missing attribute "+parameterName+" for parameter "+(k+1)+" to method "+method, null);
						throw new UnableToCompleteException();
					}
				} else {
					boolean readOnly = false;
					String path = null;
					ExpressionInfo pathAccessors = null;
					if (((readOnly = value.startsWith("${")) || value.startsWith("#{")) && value.endsWith("}")) {
					    path = value.substring(2, value.length() - 1).trim();
					    pathAccessors = findAccessors(path, false, true);
					    if(pathAccessors == null) {
	                        logger.log(TreeLogger.ERROR, "Failed to evaluate parameter "+parameterName+" = "+value+" for method "+method, null);
	                        throw new UnableToCompleteException();
					    }
					}
					if(pathAccessors != null && !readOnly) {
						logger.log(TreeLogger.ERROR, "Using #{...} for a setter with multiple parameters is not supported, use ${...}; setter is "+method+" expression is "+value, null);
						throw new UnableToCompleteException();
					}
					String valueExpr;
					if (parameter.getType().equals(getType(Action.class.getName()))
				        && (pathAccessors == null || !pathAccessors.hasGetter()) 
				        && (valueExpr = getAction(value)) != null) {
						paramString = valueExpr;
					} else if (parameter.getType().equals(getType(Value.class.getName()))
						        && (valueExpr = getFieldValue(path)) != null) {
						paramString = valueExpr;
					} else if (pathAccessors != null) {
						if(pathAccessors.getter == null) {
							logger.log(TreeLogger.ERROR, "Async/write-only values when calling a setter with multiple parameters is not supported currently.", null);
							throw new UnableToCompleteException();
						}
						paramString = pathAccessors.conversionExpr(parameter.getType());
						if(paramString == null) {
							logger.log(TreeLogger.ERROR, "Cannot convert "+path+" to "+parameter.getType()+" for call to "+method, null);
							throw new UnableToCompleteException();
						}
					} else if(parameter.getType().equals(getType("java.lang.String"))) {
					    if(value.startsWith("${"))
					        System.out.println("Warning: expression "+value+" treated as string...");
						paramString = '"'+escape(value)+'"';
					} else {
						logger.log(TreeLogger.ERROR, "Cannot convert "+value+" to "+parameter.getType()+" for call to "+method, null);
						throw new UnableToCompleteException();
					}
				}
				return paramString;
			}

            protected String addSubviewClass(Element childElem) {
                String className = myClass.getSimpleSourceName()+"Subview" + subviewClasses.size();
                subviewClasses.add(childElem);
                return className;
            }

            /**
             * Return an expression which is a value that accesses the given field.
             * 
             * @param path
             * @return
             * @throws UnableToCompleteException
             */
            protected String getFieldValue(String path) throws UnableToCompleteException {
                String existingValue = (String) values.get(path);
                if (existingValue != null) {
                    return existingValue;
                }
                ExpressionInfo accessors = findAccessors(path, true, false);
                if (accessors != null) {
                    final JClassType valueClassType = getType(Value.class.getName());
                	if(accessors.type.equals(valueClassType)) {
                		if(accessors.getter == null) {
                			logger.log(TreeLogger.ERROR, "Can't handle an async Value getter yet; for "+path, null);
                			throw new UnableToCompleteException();
                    	}
                		return accessors.getter;
                	}
                    String valueName = "value" + values.size();
                    values.put(path, valueName);
					generateField(valueName, valueClassType);
                    sw.println(valueName + " = new Value() { ");
                    if(accessors.getter != null) {
                        sw.indentln("public void getValue(AsyncCallback callback) { callback.onSuccess(" + accessors.getter + "); } ");
                    } else if(accessors.asyncGetter != null) {
                        sw.indentln("public void getValue(AsyncCallback callback) { "+callAsyncGetter(accessors.asyncGetter, "callback")+"; } ");
                    } else {
                        sw.indentln("public void getValue(AsyncCallback callback) { callback.onFailure(null); } ");
                    }
                    if (accessors.setter != null) {
                        sw.indentln("public void setValue(Object value, AsyncCallback callback) { try {" +
                        		"" + accessors.setter+"("+converter("value", types.getJavaLangObject(), accessors.type)+");" +
                        				"callback.onSuccess(null); " +
                       			"} catch(Throwable caught) { callback.onFailure(caught); } }");
                    } else if(accessors.asyncSetter != null) {
                        sw.indentln("public void setValue(Object value, AsyncCallback callback) { " + accessors.setter
                            + "("+converter("value", types.getJavaLangObject(), accessors.type)+", callback); }");
                    } else {
                        sw.indentln("public void setValue(Object value, AsyncCallback callback) { callback.onFailure(null); }");
                    }
                    sw.println("};");
                    return valueName;
                }
                /*
                 * ExpressionInfo metadata = getFieldMetadata(path); if(metadata != null) { String
                 * valueName = "value"+values.size(); values.put(path, valueName);
                 * addDeclaration(getType(Value.class.getName()), valueName); sw.println(valueName+" =
                 * "+metadata.getter+".getMetadata().getFieldByPath(\""+metadata.setter+"\").bindToModel(new
                 * Value() {"); sw.indentln("public void getValue(AsyncCallback callback) {
                 * callback.onSuccess("+metadata.getter+"); }"); sw.indentln("public void
                 * setValue(Object value, AsyncCallback callback) { throw new Error(); }");
                 * sw.println("});"); return valueName; }
                 */
                return null;
            }

            protected String getRootView(boolean innerClass) {
            	if(!subviewClass) {
            		if(innerClass) return myClass.getSimpleSourceName()+".this";
            		return "this";
            	}
            	JClassType classToSearch = myClass;
            	String expr = null;
            	while(classToSearch != null) {
            		classToSearch = classToSearch.getEnclosingType();
            		expr = (expr!=null?expr+".":"")+"my"+classToSearch.getSimpleSourceName();
            		if(classToSearch == rootClassType) {
            			return expr;
            		}
            	}
            	throw new Error("Generator is not an inner class of the root view !?!?");
            }
            
			String getAction(final String expr) throws UnableToCompleteException {
				return getAction(expr, true, true);
			}

			/**
			 * Multiple actions can be seperated using semicolons; the actions will run in sequence
			 * until they are all complete, or one fails in which case execution stops and the
			 * failure is returned to the default async callback (if any).
			 * 
			 * Actions which are an expression ${...} or #{...} are trated as expressions leading to
			 * an Action object somewhere, which is executed.
			 * 
			 * Actions starting with ';' won't save before running; actions ending with ';' won't load after
			 * running.  This can be used to avoid full save/load cycles when save/load is slow or unnecessary.
			 */
            protected String getAction(String path, boolean saveBefore, boolean loadAfter) throws UnableToCompleteException {
            	if((path.startsWith("${") || path.startsWith("#{")) && path.endsWith("}")) {
            		path = path.substring(2, path.length()-1);
                    ExpressionInfo expr = findAccessors(path, true, false);
                    if(expr != null && expr.hasGetter())
                		return expr.getGetter();
                    logger.log(TreeLogger.ERROR, "Unable to resolve action variable at path "+path, null);
                    throw new UnableToCompleteException();
            	}
            	if(path.startsWith(";")) {
            		saveBefore = false;
            		path = path.substring(1).trim();
            	}
            	if(path.endsWith(";")) {
            		loadAfter = false;
            		path = path.substring(0, path.length()-1).trim();
            	}
            	String actionKey = path+","+saveBefore+","+loadAfter;
            	String existing = (String) actions.get(actionKey);
                if (existing != null)
                    return existing;
                String actionName = "action" + actions.size();
                actions.put(actionKey, actionName);
                String[] actionSeries = path.split("\\s*;\\s*");
                String rootView = getRootView(true);
                if(actionSeries.length > 1) {
                	sw.println("final ActionSeries "+actionName+"Series = new ActionSeries();");
                	sw.println("final Action "+actionName+" = new ViewAction("+actionName+"Series" +
            			", "+rootView+", "+saveBefore+", "+loadAfter+");");
                	for (int i = 0; i < actionSeries.length; i++) {
                		String action = getAction(actionSeries[i], false, false);
                		sw.println(actionName+"Series.add("+action+");");
                	}
                    return actionName;
                }

                if("".equals(path)) {
	                sw.println("final Action " + actionName + " = new ViewAction(null, "+rootView+", "+saveBefore+", "+loadAfter+");");
                	return actionName;
                }
                
                String[] args = null;
                String preargs = path;
                int argstart;
                if (path.endsWith(")") && (argstart = smartIndexOf(path, '(')) != -1) {
                    args = path.substring(argstart + 1, path.length() - 1).split("\\s*,\\s*");
                    // Check for an empty parameter list
                    if(args.length == 1 && args[0].length() == 0)
                        args = new String[0];
                    preargs = path.substring(0, argstart);
                } else {
                    args = new String[0];
                }

                sw.println("final Action " + actionName + " = new ViewAction(new Action() {");
                sw.indent();
                sw.println("public void perform(AsyncCallback callback) {");
                sw.indent();
                int assignmentIndex = smartIndexOf(preargs, '=');
                if(assignmentIndex != -1) {
                	String left = path.substring(0, assignmentIndex).trim();
                	String right = path.substring(assignmentIndex+1).trim();
                	ExpressionInfo lvalue = findAccessors(left, true, true);
                	if(lvalue == null || (lvalue.setter == null && lvalue.asyncSetter == null)) {
                		logger.log(TreeLogger.ERROR, "Can't find any setter for the left side of "+path, null);
                		throw new UnableToCompleteException();
                	}
                	ExpressionInfo rvalue = findAccessors(right, true, true);
                	if(rvalue == null || (rvalue.getter == null && rvalue.asyncGetter == null)) {
                		logger.log(TreeLogger.ERROR, "Can't find any getter for the right side of "+path, null);
                		throw new UnableToCompleteException();
                	}
                	if(lvalue.asyncSetter == null && rvalue.asyncGetter == null) {
                        sw.println("try {");
                        sw.indent();
                        sw.println(lvalue.copyStatement(rvalue));
                        sw.println("callback.onSuccess(null);");
                        sw.outdent();
                        sw.println("} catch(Throwable caught) {");
                        sw.indentln("callback.onFailure(caught);");
                        sw.println("}");                		
                	} else {
                		sw.println("final AsyncCallback actionCallback = callback;");
                		sw.println(lvalue.asyncCopyStatement(rvalue, "actionCallback", false));
                	}
                } else {
                    int objectPathEnd = preargs.lastIndexOf('.');
                    String objectPath;
                    String methodName;
                    String getter;
                    final JClassType objectType;
                    boolean searchingThis = (objectPathEnd == -1);
                    if (searchingThis) {
                        objectPath = getter = "this";
                        objectType = myClass;
                        methodName = preargs;
                    } else {
                        objectPath = preargs.substring(0, objectPathEnd);
                        methodName = preargs.substring(objectPathEnd+1);
                        ExpressionInfo accessors = findAccessors(objectPath, true, false);
                        if (accessors == null || accessors.getter == null || accessors.type == null) {
                            logger.log(TreeLogger.ERROR, "Can't find any object for " + path, null);
                            return null;
                        }
                        getter = accessors.getter;
                        objectType = accessors.type.isClassOrInterface();
                        if (objectType == null) {
                            logger.log(TreeLogger.ERROR, "Can't call a method on a non-class object of type "
                                            + accessors.type + " for expression " + path, null);
                            throw new UnableToCompleteException();
                        }
                    }
    
                    boolean asyncMethod = false;
                    JMethod actionMethod = null;
                	JClassType searchType = objectType;
                    final String asyncCallbackClassName = AsyncCallback.class.getName();
                    for(;;) {
                        JMethod[] methods = getAllMethods(searchType);
                        //if(methodName.equals("getContact")) System.out.println("Looking for "+methodName+" in "+searchType+" with "+methods.length+" methods to search");
                        for (int i = 0; i < methods.length; i++) {
                            JMethod method = methods[i];
                            if (method.getName().equals(methodName)) {
                            	//System.out.println("Found "+method+" "+method.getName()+" in "+searchType+" getter "+getter+" looking for "+methodName+" with "+args.length+" parameters");
                                int nParams = method.getParameters().length;
                                if(nParams == 0) {
                                	if(args.length == 0) {
                                		actionMethod = method;
                                		asyncMethod = false;
                                		break;
                                	}
                                } else {
                                    final JType lastParameterType = method.getParameters()[nParams - 1].getType();
                                    //System.out.println("Last parameter type is "+lastParameterType.getQualifiedSourceName()+" == async callback? "+lastParameterType.getQualifiedSourceName().equals(asyncCallbackClassName));
									asyncMethod = lastParameterType.getQualifiedSourceName().equals(asyncCallbackClassName);
                                    if (asyncMethod) {
                                        if (nParams == args.length + 1) {
                                            actionMethod = method;
                                            break;
                                        }
                                    } else if (nParams == args.length) {
                                        actionMethod = method;
                                        break;
                                    }
                                }
                            }
                        }
                        if(actionMethod != null)
                        	break;
                        if(searchingThis && subviewClass) {
                        	searchType = searchType.getEnclosingType();
                        	if(searchType == null)
                        		break;
                        	getter = getter+".my"+searchType.getSimpleSourceName();
                        	//System.out.println("Looking up into "+searchType+" getter "+getter+" for "+methodName);
                        } else break;
                    }
                    if (actionMethod == null) {
                        logger.log(TreeLogger.WARN, "getAction(): Unable to find a method with the right number of arguments ("
                                        + args.length + " [ + AsyncCallback]) with name '" + methodName + "' on " + objectType + " in " + myClass
                                        + " for expression " + path +" searchingThis = "+searchingThis, null);
                        return null;
                    }
    
                    for (int i = 0; i < args.length; i++) {
                        String arg = args[i].trim();
                        ExpressionInfo argAccessors = findAccessors(arg, true, false);
                        if (argAccessors == null) {
                            logger.log(TreeLogger.ERROR, "Couldn't evaluate '" + arg + "' as argument to '" + path + "'",
                                            null);
                            throw new UnableToCompleteException();
                        }
                        args[i] = argAccessors.conversionExpr(actionMethod.getParameters()[i].getType());
                    }
    
                    String methodCall;
                    if (getter.startsWith("this.")) {
                        methodCall = getter.substring(5) + "." + methodName;
                    } else if (getter.equals("this")) {
                        methodCall = methodName;
                    } else {
                        methodCall = getter + "." + methodName;
                    }
                    if (asyncMethod) {
                        sw.println(methodCall + "(" + joinWithCommas(0, args) + (args.length > 0 ? ", " : "")
                            + "callback);");
                    } else {
                        sw.println("try {");
                        sw.indent();
                        sw.println(methodCall + "(" + joinWithCommas(0, args) + ");");
                        sw.println("callback.onSuccess(null);");
                        sw.outdent();
                        sw.println("} catch(Throwable caught) {");
                        sw.indentln("callback.onFailure(caught);");
                        sw.println("}");
                    }
                }
                sw.outdent();
                sw.println("}");
                sw.outdent();
                sw.println("}, "+rootView+", "+saveBefore+", "+loadAfter+");");

                return actionName;
            }

            /**
             * Find a Field object for the given path in the given type. This will carry on through
             * fields and accessors, choosing the deepest object that implements HasMetadata, then
             * call getMetadata() on it and use that to fetch the path.
             * 
             * @param inType
             *                The type of the object to search
             * @param expr
             *                The expression which yields an object of that type, with a trailing
             *                '.'
             * @param path
             *                The remainder of the path to search
             * @return A HasMetadata which has the field, plus the remaining path to pass to
             *         getMetadata().getFieldByPath(...)
             * @throws UnableToCompleteException
             *                 If an error occurs
             */
            protected ExpressionInfo getFieldMetadata(JClassType inType, String expr, String path)
                            throws UnableToCompleteException {
                // System.out.println("findField("+inType+", "+expr+",
                // "+path+")");
                if (inType == null)
                    return null;
                String[] splitPath = path.split("\\.", 2);
                String name = splitPath[0].trim();
                if (name.length() == 0) {
                    // TODO Could/should we convert this into a field?
                    return null;
                }
                if (splitPath.length > 1) {
                    JField field = findField(inType, name);
                    // System.out.println("field is "+field);
                    if (field != null) {
                        if (field.isPublic()
                                        || (field.isPrivate() && inType == baseType)
                                        || (field.isProtected() && inType.isAssignableFrom(baseType))
                                        || (field.isDefaultAccess() && inType.getPackage()
                                                        .equals(baseType.getPackage()))) {
                            return getFieldMetadata(field.getType().isClassOrInterface(), expr + "." + name,
                                            splitPath[1]);
                        } else {
                            String methodName = "get" + capitalize(name);
                            JMethod method = findMethod(inType, name, new JType[] {});
                            if (method != null)
                                return getFieldMetadata(field.getType().isClass(), expr + "." + methodName + "()",
                                                splitPath[1]);
                        }
                    }
                }
                if (implementsInterface(inType, getType("com.habitsoft.kiyaa.metamodel.HasMetadata"))) {
                    return new ExpressionInfo(expr, path, getType("java.lang.Object"), false, false, false);
                }
                return null;
            }

            /**
             * Find a Field object given an in-page path. See getFieldMetadata(JClassType inType,
             * String expr, String path) for details.
             */
            protected ExpressionInfo getFieldMetadata(String path) throws UnableToCompleteException {
                return getFieldMetadata(baseType, "this", path);
            }

            protected JField findField(JClassType cls, String name) {
                // System.out.println("findField("+cls+", "+name+")");
                JField field = cls.findField(name);
                if (field != null)
                    return field;
                JClassType superclass = cls.getSuperclass();
                if (superclass == null)
                    return null;
                return findField(superclass, name);
            }

            protected ExpressionInfo findAccessors(final JClassType inType, String expr, final String path, final boolean matchAsync) throws UnableToCompleteException {
                //System.out.println("findAccessors("+inType+", '"+expr+"', '"+path+"', "+matchAsync+")");
                String[] splitPath = smartSplit(path, '.', 2);
                String name = splitPath[0].trim();
                if (name.length() == 0) {
                    return null;
                }
                String getter;
                boolean asyncGetter;
                String setter;
                boolean asyncSetter;
                JType type;
                /*if(name.endsWith("]")) {
            		int openBraceIdx = smartIndexOf(name, '[');
            		if(openBraceIdx == -1) {
            			logger.log(TreeLogger.ERROR, "Can't find opening [ for ] in "+name, null);
            			throw new UnableToCompleteException();
            		}
            		// TODO array indexing
        			throw new UnableToCompleteException();
            	} else */ 
                boolean endsWithParen = name.endsWith(")");
                if(endsWithParen || findMethod(inType, name, 0, matchAsync) != null) {
                    String getterMethodName;
                    String[] args;
                    if(endsWithParen) {
                		int openIdx = smartIndexOf(name, '(');
                		if(openIdx == -1) {
                			logger.log(TreeLogger.ERROR, "Can't find opening ( for ) in "+name, null);
                			throw new UnableToCompleteException();
                		}
                		
                        args = smartSplit(name.substring(openIdx + 1, name.length() - 1), ',', 100);
                        //System.out.println("Splitting '"+name.substring(openIdx + 1, name.length() - 1)+" around ',' gives "+args.length+" args: "+joinWithCommas(0, args));
                        // Check for an empty parameter list
                        if(args.length == 1 && args[0].length() == 0)
                            args = new String[0];
                        getterMethodName = name.substring(0, openIdx);
                    } else {
                        getterMethodName = name;
                        args = new String[0];
                    }
                    JClassType objectType = inType;
    
                    boolean asyncMethod = false;
                    JMethod getterMethod = null;
                	String getName = getterMethodName.matches("^get[A-Z]")?null:"get"+capitalize(getterMethodName);
                	String isName = getterMethodName.matches("^is[A-Z]")?null:"is"+capitalize(getterMethodName);
                    boolean searchingThis = objectType.equals(myClass);
                    //System.out.println("inType = "+inType+" myClass = "+myClass+" path = "+path+" expr = "+expr+" searchingThis = "+searchingThis);
                    for(;;) {
                        JMethod[] methods = getAllMethods(objectType);
                        for (int i = 0; i < methods.length; i++) {
                            JMethod method = methods[i];
                            if (method.getName().equals(getterMethodName) ||
                            	method.getName().equals(getName) ||
                            	method.getName().equals(isName)) {
                            	//System.out.println("Found "+method+" in "+objectType+" looking for "+path);
                                int nParams = method.getParameters().length;
                                if(nParams == 0) {
                                	if(args.length == 0) {
                                		getterMethod = method;
                                		asyncMethod = false;
                                		break;
                                	}
                                } else {
                                    final JType lastParameterType = method.getParameters()[nParams - 1].getType();
                                    asyncMethod = lastParameterType.getQualifiedSourceName().equals(AsyncCallback.class.getName());
                                    if (asyncMethod) {
                                        if (nParams == args.length + 1) {
                                            getterMethod = method;
                                            break;
                                        }
                                    } else if (nParams == args.length) {
                                        getterMethod = method;
                                        break;
                                    }
                                }
                            }
                        }
                        if(searchingThis && subviewClass && getterMethod == null) {
                        	objectType = objectType.getEnclosingType();
                        	if(objectType == null)
                        		break;
                        	expr = expr+".my"+objectType.getSimpleSourceName();
                        	//System.out.println("Ascending to "+expr+" "+objectType);
                        } else break;
                    }
                    if (getterMethod == null) {
                        logger.log(TreeLogger.ERROR, "findAccessors(): Unable to find a method with the right number of arguments ("
                                        + args.length + " [ + AsyncCallback]) with name '" + getterMethodName + "' in " + inType
                                        + " for expression " + path, null);
                        throw new UnableToCompleteException();
                    }
                    getterMethodName = getterMethod.getName();
    
                    for (int i = 0; i < args.length; i++) {
                        String arg = args[i].trim();
                        ExpressionInfo argAccessors = findAccessors(arg, true, false);
                        if (argAccessors == null) {
                            logger.log(TreeLogger.ERROR, "Couldn't evaluate '" + arg + "' as argument to '" + name + "'",
                                            null);
                            throw new UnableToCompleteException();
                        }
                        args[i] = argAccessors.conversionExpr(getterMethod.getParameters()[i].getType());
                    }
    
                    getter = expr + "." + getterMethodName + (asyncMethod && args.length==0?"":"("+joinWithCommas(0, args)+(asyncMethod?",":")"));
                    
                    asyncGetter = asyncMethod;
                    type = asyncMethod?getAsyncReturnType(getterMethod):getterMethod.getReturnType();
                    
                    String setterMethodName = getterMethodName.replaceFirst("^(is|get)", "set");
                    //if(getterMethodName.equals("getAdjustmentAccount"))
                    //	System.out.println("Looking for "+setterMethodName+" to match "+getter+" with "+args.length+" arguments, matchAsync = "+matchAsync+" objectType = "+objectType);
					JMethod setterMethod = findMethod(objectType, setterMethodName, args.length+1, matchAsync);
					if(setterMethod != null) {
						setter = expr + "." + setterMethodName + (asyncMethod && args.length==0?"":"("+joinWithCommas(0, args)+",");
						asyncSetter = setterMethod.getParameters().length == args.length+2;
					} else {
						setter = null;
						asyncSetter = false;
					}
            	} else {
            		// No array or function specifier, so look for a normal property
                    String baseExpr = (expr.equals("this") ? "" : expr + ".");
                    String getterName = "get" + capitalize(name);
                    String setterName = "set" + capitalize(name);
                    JMethod getterMethod = findMethod(inType, getterName, 0, matchAsync);
                    if (getterMethod == null) {
                        getterName = "is" + capitalize(name);
                        getterMethod = findMethod(inType, getterName, 0, matchAsync);
                    }
                    if (getterMethod != null) {
                        asyncGetter = matchAsync && getterMethod.getParameters().length == 1 
                                      && getType(AsyncCallback.class.getName()).isAssignableFrom(getterMethod.getParameters()[0].getType().isClassOrInterface());
                        getter = baseExpr + getterName + (asyncGetter?"":"()"); // No trailing brackets for an async call
                        if(asyncGetter) {
                        	type = getAsyncReturnType(getterMethod);
                        } else {
                            type = getterMethod.getReturnType();
                        }
                    } else {
                    	getter = null;
                    	asyncGetter = false;
                    	type = null;
                    }
                    JMethod setterMethod;
                    if(splitPath.length == 1 && (setterMethod = findMethod(inType, setterName, 1, matchAsync))!=null) {
                    	//System.out.println("Found setter "+setterMethod);
                        setter = baseExpr + setterName;
                        asyncSetter = matchAsync && setterMethod.getParameters().length == 2;
//                        if(!asyncGetter && type != null && !type.isAssignableTo(setterMethod.getParameters()[0].getType())) {
//                        	logger.log(TreeLogger.ERROR, "Setter and getter don't have the same type: "+setter+" and "+getter, null);
//                        	throw new UnableToCompleteException();
//                        } else {
                        	type = setterMethod.getParameters()[0].getType();
                        //}
                    } else {
                    	setter = null;
                    	asyncSetter = false;
                    }
            	}
                
                //System.out.println("Looking for "+name+" in "+inType+", found "+getter+" and "+setter);
                if(getter != null && splitPath.length == 2) {
                    final JClassType classType = type.isClassOrInterface();
                    if(classType == null) {
                    	if(type.isArray() != null) {
                    		if("length".equals(splitPath[1])) {
                    			return new ExpressionInfo(getter+"."+splitPath[1], JPrimitiveType.INT, false);
                    		} else {
                            	logger.log(TreeLogger.ERROR, "Attempting to access a property of array that I don't recognize: "+getter+"."+splitPath[1], null);
                                throw new UnableToCompleteException();
                    		}
                    	}
                    	logger.log(TreeLogger.ERROR, "Attempting to access a property of something that isn't a class or interface: "+getter+" of type "+type+" async = "+asyncGetter, null);
                        throw new UnableToCompleteException();
                    }
                    if(asyncGetter == false) {
                    	// Easy... just get them to create a new getter based on this one
                    	return findAccessors(classType, getter, splitPath[1], matchAsync);
                    } else {
                    	// Oops, we're getting a property of an async property, time for some magic
                    	// The trick: generate a new method that does the first async operation and
                    	// then returns the result of the getter of the proceeding attributes.
                    	ExpressionInfo subexpr = findAccessors(classType, "base", splitPath[1], matchAsync);
                    	if(subexpr == null) {
                    		logger.log(TreeLogger.ERROR, "Failed to find property '"+splitPath[1]+"' of type '"+classType+"' of expression '"+getter+"'", null);
                            throw new UnableToCompleteException();
                    	}
                    	String getterName = "getAsync"+asyncProxies.size();
                    	if(subexpr.hasGetter()) {
                        	if(subexpr.getter != null) {
                        		// Synchronous sub-expression, how merciful! 
                            	asyncProxies.add("    public void "+getterName+"(AsyncCallback callback) {\n"+
                            		             "        "+callAsyncGetter(getter, "new AsyncCallbackProxy(callback) {\n"+
                            		             "            public void onSuccess(Object result) {\n"+
                            		             "                "+type.getQualifiedSourceName()+" base = ("+type.getQualifiedSourceName()+") result;\n"+
                            		             "                super.onSuccess("+subexpr.getter+");\n"+
                            		             "            }\n"+
                            		             "        }")+";\n"+
                            		             "    }\n");
                        	} else if(subexpr.asyncGetter != null) {
                            	asyncProxies.add("    public void "+getterName+"(AsyncCallback callback) {\n"+
               		             "        "+callAsyncGetter(getter, "new AsyncCallbackProxy(callback) {\n"+
               		             "            public void onSuccess(Object result) {"+
               		             "                "+type.getQualifiedSourceName()+" base = ("+type.getQualifiedSourceName()+") result;\n"+
               		             "                "+callAsyncGetter(subexpr.asyncGetter, "callback")+";\n"+
               		             "            }\n"+
               		             "        }")+";\n"+
               		             "    }\n");
                        	}
                    	} else getterName = null;
                    	String setterName = "setAync"+asyncProxies.size();
                    	if(subexpr.hasSetter()) {
                        	if(subexpr.setter != null) {
                        		// Synchronous sub-expression, how merciful! 
                            	asyncProxies.add("    public void "+setterName+"(final "+subexpr.type.getQualifiedSourceName()+" value, AsyncCallback callback) {\n"+
                  		             "        "+callAsyncGetter(getter, "new AsyncCallbackProxy(callback) {\n"+
                            		             "            public void onSuccess(Object result) {\n"+
                            		             "                "+type.getQualifiedSourceName()+" base = ("+type.getQualifiedSourceName()+") result;\n"+
                            		             "                "+subexpr.setter+"(value);\n"+
                            		             "                super.onSuccess(null);\n"+
                            		             "            }\n"+
                   		             "        }")+";\n"+
                            		             "    }");
                        	} else if(subexpr.asyncSetter != null) {
                            	asyncProxies.add("    public void "+setterName+"(final "+subexpr.type.getQualifiedSourceName()+" value, AsyncCallback callback) {\n"+
                 		             "        "+callAsyncGetter(getter, "new AsyncCallbackProxy(callback) {\n"+
               		             "            public void onSuccess(Object result) {\n"+
               		             "                "+type.getQualifiedSourceName()+" base = ("+type.getQualifiedSourceName()+") result;\n"+
               		             "                "+subexpr.asyncSetter+"(value, callback);\n"+
               		             "            }\n"+
              		             "        }")+";\n"+
               		             "    }");
                        	}
                    	} else setterName = null;
                    	return new ExpressionInfo(getterName, setterName, subexpr.type, true, true, false);
                    }
                } else if(setter != null && splitPath.length == 1) {
                	return new ExpressionInfo(getter, setter, type, asyncGetter, asyncSetter, false);
                }
                
                /*
                JClassType superclass = inType.getSuperclass();
                if (superclass != null && !types.getJavaLangObject().equals(superclass)) {
                	ExpressionInfo inherited = findAccessors(superclass, expr, path, matchAsync);
                	if(inherited != null) {
                		if(getter == null) { 
                			if(inherited.getter != null) { getter = inherited.getter; asyncGetter = false; }
                			else if(inherited.asyncGetter != null) { getter = inherited.asyncGetter; asyncGetter = true; }
                		}
                		if(setter == null) { 
                			if(inherited.setter != null) { setter = inherited.setter; asyncSetter = false; }
                			else if(inherited.asyncSetter != null) { setter = inherited.asyncSetter; asyncSetter = true; }
                		}
                		if(type == null) type = inherited.getType();
                	}
                }
                */
                if(type != null) {
                	return new ExpressionInfo(getter, setter, type, asyncGetter, asyncSetter, false);
                }
                //System.out.println("Failed to find property "+name+" on "+inType);
                return null;
            }

            
            /**
             * A wrapper for string.split() that doesn't split inside matching ()'s or []'s.
             * 
             * maxlen refers to the maximum length of the resulting array.
             */
            private String[] smartSplit(String s, char seperator, int maxlen) {
            	ArrayList result = new ArrayList();
            	int lastCut=0;
            	maxlen--;
            	while(result.size() < maxlen) {
            		int i = smartIndexOf(s, seperator, lastCut);
            		if(i == -1) {
            			break;
            		}
            		
        			result.add(s.substring(lastCut, i));
        			lastCut = i+1;
            	}
            	result.add(s.substring(lastCut));
            	return (String[]) result.toArray(new String[result.size()]);
            }

            /**
             * Version of indexOf() that ignores results inside brackets or quotes
             */
            private int smartIndexOf(String s, String term, int start) {
            	int depth=0;
            	int quote=0;
            	int i=start;
            	for(;;) {
					if(depth == 0 && s.substring(i).startsWith(term)) {
            			return i;
            		} else if(quote != 0) {
            			if(s.charAt(i) == quote && s.charAt(i-1) != '\\') {
            				quote = 0;
            				depth--;
            				i++;
            			}
            		} else if(s.charAt(i) == '"' || s.charAt(i) == '\'') {
            			quote = s.charAt(i);
            			depth++;
            		} else if(s.charAt(i) == '(' || s.charAt(i) == '[') {
            			depth++; i++;
            		} else if(s.charAt(i) == ')' || s.charAt(i) == ']') {
            			depth--; i++;
            		} else if(s.substring(i).startsWith(term)) {
            			// Found the term, but it's inside brackets or quotes or something, so skip it
            			i += term.length();
            		}
            		int nextPos;
            		if(quote != 0) {
            			nextPos = s.indexOf(quote, i+1);
            			// if nextPos == -1, it's a mismatched quote
            		} else {
            			nextPos = s.indexOf('(', i);
            			nextPos = minAboveZero(nextPos, s.indexOf('"', i));
            			nextPos = minAboveZero(nextPos, s.indexOf('\'', i));
            			nextPos = minAboveZero(nextPos, s.indexOf(')', i));
            			nextPos = minAboveZero(nextPos, s.indexOf(']', i));
            			nextPos = minAboveZero(nextPos, s.indexOf('[', i));
            			nextPos = minAboveZero(nextPos, s.indexOf(term, i));
            		}
        			if(nextPos == -1)
        				return -1;
        			i = nextPos;
            	}
            }
            private int smartIndexOf(String s, String term) {
            	return smartIndexOf(s, term, 0);
            }
            
            private int minAboveZero(int a, int b) {
            	if(b < 0) return a;
            	if(a < 0) return b;
            	return Math.min(a, b);
            }
            /**
             * Version of indexOf() that ignores results inside brackets.
             */
            int smartIndexOf(String s, char term, int start) {
            	if(s.length()==0)
            		return -1;
            	int depth=0;
            	int quote=0;
            	int i=start;
            	for( ;; ) {
            		if(depth == 0 && s.charAt(i) == term) {
            			return i;
            		} else if(quote != 0) {
            			if(s.charAt(i) == quote && s.charAt(i-1) != '\\') {
            				quote = 0;
            				depth--;
            				i++;
            			}
            		} else if(s.charAt(i) == '"' || s.charAt(i) == '\'') {
            			quote = s.charAt(i);
            			depth++;
            		} else if(s.charAt(i) == '(' || s.charAt(i) == '[') {
            			depth++; i++;
            		} else if(s.charAt(i) == ')' || s.charAt(i) == ']') {
            			depth--; i++;
            		} 
            		int nextPos;
            		if(quote != 0) {
            			nextPos = s.indexOf(quote, i+1);
            			// if nextPos == -1, it's a mismatched quote
            		} else {
            			nextPos = s.indexOf('(', i);
            			nextPos = minAboveZero(nextPos, s.indexOf('"', i));
            			nextPos = minAboveZero(nextPos, s.indexOf('\'', i));
            			nextPos = minAboveZero(nextPos, s.indexOf(')', i));
            			nextPos = minAboveZero(nextPos, s.indexOf(']', i));
            			nextPos = minAboveZero(nextPos, s.indexOf('[', i));
            			if(depth == 0)
            				nextPos = minAboveZero(nextPos, s.indexOf(term, i));
            		}
            		//System.out.println("Next pos in <<"+s+">> is "+nextPos+" <<"+(nextPos==-1?"":s.substring(nextPos))+">> i="+i+" <<"+s.substring(i)+">> term='"+term+"' depth="+depth);
        			if(nextPos == -1)
        				return -1;
        			i = nextPos;
            	}
            }
            int smartIndexOf(String s, char term) {
            	return smartIndexOf(s, term, 0);
            }

            /**
             * Find a getter/setter pair for an object path.
             * 
             * The getter is a complete expression; the setter typically ends at the method name or
             * an assignment operator and should have its parameter wrapped in parentheses.
             * 
             * This only works for getters/setters that can be found using static type information.
             * 
             * This returns null for the setter if attempts to set the value should be ignored.
             * @param innerType
             *                If the expression is going into a nested anonymous class, pass true
             * @param matchAsync If true, include asynchronous methods in the search; otherwise do not include them
             * 
             * @throws UnableToCompleteException 
             */
            protected ExpressionInfo findAccessors(String path, boolean innerType, boolean matchAsync) throws UnableToCompleteException {
            	//System.out.println("findAccessors("+path+")");
            	int operIdx;
            	if(((operIdx = smartIndexOf(path, "&&")) != -1 
            		|| (operIdx = smartIndexOf(path, " and ")) != -1 
            		|| (operIdx = smartIndexOf(path, "||")) != -1
            		|| (operIdx = smartIndexOf(path, " or ")) != -1 
            		|| (operIdx = smartIndexOf(path, ">=")) != -1 
            		|| (operIdx = smartIndexOf(path, "<=")) != -1 
            		|| (operIdx = smartIndexOf(path, ">")) != -1 
            		|| (operIdx = smartIndexOf(path, "<")) != -1 
            		|| (operIdx = smartIndexOf(path, "==")) != -1 
            		|| (operIdx = smartIndexOf(path, "!=")) != -1
            		|| (operIdx = smartIndexOf(path, "+")) != -1
            		|| (operIdx = smartIndexOf(path, "-")) != -1
            		|| (operIdx = smartIndexOf(path, "/")) != -1
            		|| (operIdx = smartIndexOf(path, "*")) != -1)) {
            		return handleBinaryOperator(path, innerType, matchAsync, operIdx);
            	} else if(path.startsWith("!") || path.startsWith("not ")) {
            		String remainder = path.startsWith("!")?path.substring(1).trim():path.substring(4).trim();            		
            		//System.out.println("Boolean NOT");                	            		
            		final ExpressionInfo accessors = findAccessors(remainder, innerType, matchAsync);
            		if(accessors != null) {
            			return new ExpressionInfo(accessors, new OperatorInfo() {
            				@Override
							public String onGetExpr(String expr) throws UnableToCompleteException {
            					return "!(" + converter(expr, accessors.type, JPrimitiveType.BOOLEAN) + ')';
            				}
            				@Override
							public String onSetExpr(String expr) throws UnableToCompleteException {
            					return converter("!(" + expr + ')', JPrimitiveType.BOOLEAN, accessors.type);
            				}
            			});
            		} else {
            			return null;
            		}
            	} else if(path.startsWith("-") || path.startsWith("~")) {
            		String remainder = path.substring(1).trim();
            		final String oper = path.substring(0,1);
            		ExpressionInfo accessors = findAccessors(remainder, innerType, matchAsync);
            		if(accessors != null) {
            			return new ExpressionInfo(accessors, new OperatorInfo() {
            				@Override
							public String onGetExpr(String expr) {
            					return oper + '(' + expr + ')';
            				}
            				@Override
							public String onSetExpr(String expr) {
            					return oper + '(' + expr + ')';
            				}
            			});
            		} else {
            			return null;
            		}
            	} else if(path.length() > 0 && (Character.isDigit(path.charAt(0)) || (path.length() >= 2 && path.charAt(0) == '-' && Character.isDigit(path.charAt(1))))) {
            		if(smartIndexOf(path, '.') != -1) {
            			// floating-point
            			return new ExpressionInfo(path, JPrimitiveType.DOUBLE, true);
            		} else if(path.endsWith("L")) {
            			return new ExpressionInfo(path, JPrimitiveType.LONG, true);
            		} else {
            			return new ExpressionInfo(path, JPrimitiveType.INT, true);
            		}
            	} else if(path.equals("true") || path.equals("false")) {
            		return new ExpressionInfo(path, JPrimitiveType.BOOLEAN, true);
            	} else if(path.equals("null")) {
            		return new ExpressionInfo(path, types.getJavaLangObject(), true);
            		
            	} else if(path.startsWith("\"") && path.endsWith("\"")) {
            		return new ExpressionInfo(path, getType("java.lang.String"), true);
            		
            	}
            	String thisExpr = innerType ? myClass.getSimpleSourceName() + ".this" : "this";
                JClassType classToSearch = myClass;
                if (path.equals("this")) {
                	while(classToSearch != rootClassType) {
                		classToSearch = classToSearch.getEnclosingType();
                		if(classToSearch == null)
                			break;
                		thisExpr = thisExpr+".my"+classToSearch.getSimpleSourceName();
                	}
                	
                	// When they use the expression "this", be sure to use the superclass of the generated class;
                	// the generated class won't behave well with isAssignableFrom() and isAssignableTo() because
                	// it's a fake object we created and isn't "known" by the type oracle.
                    return new ExpressionInfo(thisExpr, null, rootClassType.getSuperclass(), false, false, false);
                }
                
                // like books.service.AccountType.ACCOUNTS_RECEIVABLE or abc.def.Foo.bar
                Matcher staticReference = Pattern.compile("([a-z0-9_]+(?:\\.[a-z0-9_]+)+(?:\\.[A-Z][A-Za-z0-9_]+)+)\\.([A-Za-z0-9_]+.*)").matcher(path);
                if(staticReference.matches()) {
                	String className = staticReference.group(1);
                	String property = staticReference.group(2);
                	//System.out.println("Static reference: "+className+" property "+property);
                	JClassType staticType = getType(className);  
                	JField field = staticType.getField(property);
                	if(field != null && field.isStatic()) {
                	    return new ExpressionInfo(path, field.getType(), field.isFinal());
                	}
                	return findAccessors(staticType, className, property, matchAsync);
                }
                
                for (;;) {
                    ExpressionInfo accessors = findAccessors(classToSearch, thisExpr, path, matchAsync);
                    if (accessors != null) {
                    	//System.out.println("Found in "+classToSearch+" "+thisExpr+" for "+path);
                        return accessors;
                    } else {
                        classToSearch = classToSearch.getEnclosingType();
                        if (classToSearch == null) {
                        	//System.out.println("Looking in "+classToSearch+" "+thisExpr+" for "+path+" failed");
                            return null;
                        }
                        if(myClass.isStatic() || subviewClass) {
                            thisExpr = thisExpr+".my"+classToSearch.getSimpleSourceName();
                        } else {
                            thisExpr = classToSearch.getQualifiedSourceName() + ".this";
                        }
                    }
                }
            }

			private ExpressionInfo handleBinaryOperator(String path, boolean innerType, boolean matchAsync, int operIdx)
				throws UnableToCompleteException {
				final String leftPath = path.substring(0, operIdx).trim();
				ExpressionInfo left = findAccessors(leftPath, innerType, matchAsync);
				
				int operLen;
				String oper;
				boolean arithmeticOper=false;
				if(path.startsWith(" or ", operIdx)) {
					operLen = 4;
					oper = "||";
				} else if(path.startsWith(" and ", operIdx)) {
					operLen = 5;
					oper = "&&";
				} else if("+-*/".indexOf(path.charAt(operIdx)) != -1) {
					operLen = 1;
					oper = path.substring(operIdx, operIdx+1);
					arithmeticOper = true;
				} else {
					operLen = 2;
					oper = path.substring(operIdx, operIdx+operLen);
				}
				final String rightPath = path.substring(operIdx+operLen).trim();
				ExpressionInfo right = findAccessors(rightPath, innerType, matchAsync);
				if(left == null || !left.hasGetter()) {
					logger.log(TreeLogger.ERROR, "Can't evaluate "+leftPath+" in "+path+" match async = "+matchAsync, null);
					throw new UnableToCompleteException();
				}
				if(right == null || !right.hasGetter()) {
					logger.log(TreeLogger.ERROR, "Can't evaluate "+rightPath+" in "+path+" match async = "+matchAsync, null);
					throw new UnableToCompleteException();
				}
				JType targetOperandType;
				JType resultType;
				JType java_lang_String = getType("java.lang.String");
				if ("&&".equals(oper) || "||".equals(oper)){
					targetOperandType = JPrimitiveType.BOOLEAN;
					resultType = JPrimitiveType.BOOLEAN;
				} else if("+".equals(oper) && (right.getType().equals(java_lang_String) || left.getType().equals(java_lang_String))) {
					targetOperandType = java_lang_String;
					resultType = java_lang_String;
				} else {
					targetOperandType = left.type;
					if(arithmeticOper)
						resultType = left.type;
					else // all other operations are boolean
						resultType = JPrimitiveType.BOOLEAN;
				}
				
				// Is it an asynchronous calculation?
				if(left.asyncGetter != null || right.asyncGetter != null) {
					return handleAsyncBinaryOperator(left, oper, right, targetOperandType, resultType);
				}
				
				String convertRight = right.conversionExpr(targetOperandType);
				if(convertRight == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+right.getType()+" "+right.getter+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				String convertLeft = left.conversionExpr(targetOperandType);
				if(convertLeft == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+left.getter+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				return new ExpressionInfo("(" + convertLeft + oper + convertRight + ")", resultType, left.isConstant() && right.isConstant());
			}

			private ExpressionInfo handleAsyncBinaryOperator(ExpressionInfo left, String oper, ExpressionInfo right,
				JType targetOperandType, JType resultType) throws UnableToCompleteException {
				String getterName = "getCalculation"+calculations.size();
				String leftTypeName = left.type.getQualifiedSourceName();
				String classLeftTypeName = getBoxedClassName(left.type);
				String rightTypeName = right.type.getQualifiedSourceName();
				String classRightTypeName = getBoxedClassName(right.type);
				final String resultClassTypeName = getBoxedClassName(resultType);
				String convertRight = converter("right", right.getType(), targetOperandType);
				if(convertRight == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+right.getType()+" "+right.getter+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				String convertLeft = converter("left", left.getType(), targetOperandType);
				if(convertLeft == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+left.getType()+" "+left.getter+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				if(left.asyncGetter != null) {
					if(right.asyncGetter != null) {
						calculations.add("    public void "+getterName+"(AsyncCallback<"+resultClassTypeName+"> callback) {\n"+
				             "        "+callAsyncGetter(left.asyncGetter, "new AsyncCallbackProxy<"+classLeftTypeName+">(callback) {\n"+
				             "            public void onSuccess(final "+classLeftTypeName+" left) {\n"+
				             "                "+callAsyncGetter(right.asyncGetter, "new AsyncCallbackProxy<"+classRightTypeName+">(callback) {\n"+
					         "                public void onSuccess(final "+classRightTypeName+" right) {\n"+
					         "                    callback.onSuccess("+convertLeft+oper+convertRight+");\n"+
					         "                }\n"+
					         "            }")+";\n"+
				             "            }\n"+
				             "        }")+";\n"+
				             "    }\n");
					} else {
						calculations.add("    public void "+getterName+"(AsyncCallback<"+resultClassTypeName+"> callback) {\n"+
				             "        "+callAsyncGetter(left.asyncGetter, "new AsyncCallbackProxy<"+classLeftTypeName+">(callback) {\n"+
				             "            public void onSuccess(final "+classLeftTypeName+" left) {\n"+
					         "                final "+rightTypeName+" right = "+right.getter+";\n"+
					         "                callback.onSuccess("+convertLeft+oper+convertRight+");\n"+
				             "            }\n"+
				             "        }")+";\n"+
				             "    }\n");
					}
				} else {
					// right.asyncGetter must != null
					calculations.add("    public void "+getterName+"(AsyncCallback<"+resultClassTypeName+"> callback) {\n"+
				         "            final "+leftTypeName+" left = "+left.getter+";\n"+
				         "            "+callAsyncGetter(right.asyncGetter, "new AsyncCallbackProxy<"+classRightTypeName+">(callback) {\n"+
				         "            public void onSuccess(final "+classRightTypeName+" right) {\n"+
				         "                callback.onSuccess("+convertLeft+oper+convertRight+");\n"+
				         "            }\n"+
				         "        }")+";\n"+
				         "    }\n");
					
				}
				return new ExpressionInfo(getterName, null, resultType, true, false, false);
			}

			private String getBoxedClassName(final JType type) {
				return type.isPrimitive()!=null?type.isPrimitive().getQualifiedBoxedSourceName():type.getQualifiedSourceName();
			}

        }

        /**
         * Return true if the given class, or one of its superclasses, implements the given
         * interface.
         * 
         * Part of the reason for this method is that the isAssignableTo and isAssignableFrom
         * methods on JClassType don't check for interfaces, only superclasses.
         */
        protected boolean implementsInterface(JClassType cls, JClassType iface) {
            if (cls == iface) {
                return true;
            }
            // System.out.println("implementsInterface("+cls+", "+iface+")");
            JClassType[] interfaces = cls.getImplementedInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                // System.out.println("implementsInterface("+cls+", "+iface+")
                // -- "+interfaces[i]+" == "+iface+"?");
                if (interfaces[i].equals(iface))
                    return true;
                if (interfaces[i].isAssignableTo(iface))
                    return true;
            }
            JClassType superclass = cls.getSuperclass();
            if (superclass == null)
                return false;
            return implementsInterface(superclass, iface);
        }

        public Object backslashEscape(String substring) {
			return substring.replaceAll("([\'\"\\\\])", "\\\\$1").replace("\n", "\\n").replace("\r", "\\r");
		}

		public JType getAsyncReturnType(JMethod method) throws UnableToCompleteException {
        	
        	JParameter[] parameters = method.getParameters();
			int parameterCount = parameters.length;
			if(parameterCount > 0) {
        		JParameterizedType parameterized = parameters[parameterCount-1].getType().isParameterized();
				if(parameterized != null) {
        			if(parameterized.getQualifiedSourceName().equals(AsyncCallback.class.getName())) {
        				return parameterized.getTypeArgs()[0];
        			}
        		}
        	}
        	
        	JType type = types.getJavaLangObject();
        	
        	String[][] returnType = method.getMetaData("gwt.asyncReturnType");
        	if(returnType.length > 0 && returnType[0].length > 0) {
        		String typeName = returnType[0][0];
        		try {
					return getType(typeName);
				} catch (UnableToCompleteException caught) {
					logger.log(TreeLogger.ERROR, "gwt.asyncReturnType specified invalid type name "+typeName, null);
					throw new UnableToCompleteException();
				}
        	}
        	
        	// If it's an async interface, lookup the type from the main interface
        	JClassType enclosingType = method.getEnclosingType();
        	//System.out.println("Looking for async return type for "+method+", enclosing type is "+enclosingType);
        	if(enclosingType.getName().endsWith("Async") && enclosingType.isInterface() != null) {
        		try {
        			final String qualifiedSourceName = enclosingType.getQualifiedSourceName();
					JClassType iface = getType(qualifiedSourceName.substring(0, qualifiedSourceName.length()-5));
					JType[] syncParameters = new JType[parameterCount-1];
					for (int i = 0; i < syncParameters.length; i++) {
						syncParameters[i] = parameters[i].getType();
					}
        			JMethod syncMethod = findMethod(iface, method.getName(), syncParameters);
        			if(syncMethod != null) {
        				type = syncMethod.getReturnType();
        			} else {
                    	System.out.println("Couldn't find synchronous version of async method "+method+", enclosing type is "+enclosingType+" synchronous interface is "+iface);
        				
        			}
        		} catch(Throwable caught) {
        			// Darn
                	System.out.println("Failed to get async return type for "+method+", enclosing type is "+enclosingType);
                	caught.printStackTrace();
        		}
        	}
        	
        	return type;
		}

		/**
         * Try to find a method on the given class with the given name and the right number of
         * parameters.
         * 
         * @param inType
         *                Class to search for methods; superclasses are not automatically searched
         * @param name
         *                Name of the method to search for
         * @param parameterCount
         *                Number of parameters we want
         * @param matchAsync If true, an async method with parameterCount + 1 will be matched
         * @return A JMethod if a match is found, or null
         * @throws UnableToCompleteException 
         */
        public JMethod findMethod(JClassType inType, String name, int parameterCount, boolean matchAsync) throws UnableToCompleteException {
            JMethod[] overloads = getAllMethods(inType);
            //System.out.println("Looking for "+name+" with "+parameterCount+" parameters matchAsync = "+matchAsync+" in "+inType);
            for (int i = 0; i < overloads.length; i++) {
                JMethod candidate = overloads[i];
                if(!candidate.getName().equals(name))
                	continue;
                //if(name.equals("setAdjustmentAccount")) System.out.println("Looking for "+name+" with "+parameterCount+" parameters matchAsync = "+matchAsync+", found "+candidate);
                
                if (candidate.getParameters().length == parameterCount) {
                    return candidate;
                }
                if(matchAsync
                	&& candidate.getParameters().length == parameterCount+1
                	&& candidate.getParameters()[parameterCount].getType().getQualifiedSourceName().equals(AsyncCallback.class.getName())) {
                	return candidate;
                }
            }
            return null;
        }

        public JMethod findMethod(JClassType inType, String name, JType[] paramTypes) {
        	for( ; inType != null; inType = inType.getSuperclass()) {
        		JMethod method = inType.findMethod(name, paramTypes);
        		//System.out.println("Looking for "+name+" on "+inType+": "+method);
        		if(method != null)
        			return method;
        	}
        	return null;
        }
        
        Map<String,JMethod[]> methodsListCache = new HashMap<String,JMethod[]>();
        /**
         * Return all methods of a class, including superclasses.
         */
        public JMethod[] getAllMethods(JClassType inType) {
        	JMethod[] cachedOverridableMethods = methodsListCache.get(inType.toString());        	
            if (cachedOverridableMethods == null) {
                Map<String,JMethod> methodsBySignature = new TreeMap<String,JMethod>();
                List<JMethod> results = new ArrayList<JMethod>();
                for(JClassType curCls = inType; curCls != null; curCls = curCls.getSuperclass()) {
                	final JClassType[] implementedInterfaces = curCls.getImplementedInterfaces();
                	for (int i = -1; i < implementedInterfaces.length; i++) {
						JClassType interfaceType = i==-1?curCls:implementedInterfaces[i];
	                    JMethod[] methods = interfaceType.getMethods();
	                	for (int j = 0; j < methods.length; j++) {
	        				JMethod method = methods[j];
	        				final String key = method.toString();
	        				//if(interfaceType.isInterface() != null)
	        				//	System.out.println(key);
	        				if(method.isStatic())
	        					continue;
	        				if(!methodsBySignature.containsKey(key)) {
	        					methodsBySignature.put(key, method);
	        					results.add(method);
	        				}
	        			}
					}
                }
                int size = results.size();
                cachedOverridableMethods = (JMethod[]) results.toArray(new JMethod[size]);
                methodsListCache.put(inType.toString(), cachedOverridableMethods);
              }
              return cachedOverridableMethods;
        }

		protected String converter(final String inExpr, final JType inType, final JType outType)
                        throws UnableToCompleteException {
            JPrimitiveType primitiveType;
            JClassType classType;
            JArrayType arrayType;
            JEnumType enumType;
            String attributeValueExpr = null;
            final JClassType java_lang_String = getType("java.lang.String");
			final JClassType java_lang_Object = types.getJavaLangObject();
            if (inType.equals(outType)) {
                attributeValueExpr = inExpr;
            } else if((enumType = outType.isEnum()) != null && inType.equals(java_lang_String)) {
            	attributeValueExpr = enumType.getQualifiedSourceName()+".valueOf("+inExpr+")";
            } else if ((classType = outType.isClassOrInterface()) != null 
                && inType.isClassOrInterface() != null) {
                JClassType inClass = inType.isClassOrInterface().getErasedType();
                classType = classType.getErasedType();
				if (classType.isAssignableFrom(inClass) || inExpr.equals("null")) {
                    // No cast needed
                    attributeValueExpr = inExpr;
                } else if (classType.isAssignableTo(inClass)) {
                    // Downcast
                    attributeValueExpr = "((" + classType.getQualifiedSourceName() + ") " + inExpr + ")";
                } else if(inType.equals(java_lang_String) &&
                	(classType.equals(getType("java.lang.Long"))
                		|| classType.equals(getType("java.lang.Double"))
                		|| classType.equals(getType("java.lang.Float"))
                		|| classType.equals(getType("java.lang.Integer")))) {
                	// TODO Generalize this to more types than just Long; Long was what I needed but others may want something else
                	attributeValueExpr = "("+inExpr+".length()==0?null:new "+ classType.getQualifiedSourceName() +"("+inExpr+"))";
                } else if(outType.equals(java_lang_String)) {
                	attributeValueExpr = "("+inExpr+"==null?\"\":String.valueOf("+inExpr+"))";
                } else {
                    logger.log(TreeLogger.ERROR, "Not assignable in either direction: "+inType+" and "+classType+" for "+inExpr, null);
                }
            } else if((arrayType = outType.isArray()) != null) {
            	if(inType.equals(java_lang_String) && arrayType.getComponentType().equals(java_lang_String)) {
            		if(inExpr.startsWith("\"")) {
            			String[] strings = inExpr.substring(1, inExpr.length()-2).split("\\s*,?\\s*");
            			if(strings.length == 1 && strings[0].equals("")) return "new String[] {}";
            			for (int i = 0; i < strings.length; i++) {
							String string = strings[i];
							strings[i] = "\"" + string + "\"";
						}
            			return "new String[] {"+joinWithCommas(0, strings)+"}";
            		} else {
            			return inExpr+".split(\"\\\\s*,\\\\s*\")";
            		}
            	} else if(inExpr.equals("null")) {
            		attributeValueExpr = inExpr;
            	} else if((inType.isArray() != null && inType.isArray().getComponentType().getQualifiedSourceName().equals("java.lang.Object")) 
            		      || inType.getQualifiedSourceName().equals("java.lang.Object")) {
                    // Downcast
                    attributeValueExpr = "((" + arrayType.getComponentType().getErasedType().getQualifiedSourceName() + "[]) " + inExpr + ")";
            	} else if(arrayType.getComponentType().getErasedType().equals(types.getJavaLangObject())) {
            		attributeValueExpr = inExpr;
                } else {
                	logger.log(TreeLogger.ERROR, "Can't convert from "+inType+" to array type "+outType+" metaclass "+outType.getClass()+" erased type "+outType.getErasedType()+" element erased type "+arrayType.getComponentType().getErasedType(), null);
                	return null;
                }
            } else if ((primitiveType = outType.isPrimitive()) != null) {
				if (primitiveType == JPrimitiveType.BOOLEAN) {
                    if (inType.equals(getType("java.lang.Boolean"))) {
                        attributeValueExpr = inExpr + ".booleanValue()";
                    } else if (inType.equals(java_lang_String)) {
                        attributeValueExpr = "Boolean.parseBoolean(" + inExpr + ")";
                    } else if("null".equals(inExpr) || "false".equals(inExpr) || "".equals(inExpr)) {
                    	attributeValueExpr = "false";
                    } else if("true".equals(inExpr)) {
                    	attributeValueExpr = "true";
                    } else if (inType.equals(java_lang_Object)) {
                        attributeValueExpr = "(" + inExpr + " != null && (!(" + inExpr
                        + " instanceof Boolean) || ((Boolean)" + inExpr + ").booleanValue()))";
                    } else if (inType.isClassOrInterface() != null) {
                        attributeValueExpr = "(" + inExpr + " != null)";
                    }
                } else {
					if (primitiveType == JPrimitiveType.INT) {
						if (inType.equals(JPrimitiveType.LONG) || inType.equals(JPrimitiveType.DOUBLE) || inType.equals(JPrimitiveType.FLOAT)) {
							attributeValueExpr = "((int)"+inExpr+")";
						} else if (inType.equals(getType("java.lang.Integer"))) {
					        attributeValueExpr = inExpr + ".intValue()";					        
					    } else if (inType.equals(java_lang_Object)) {
					        attributeValueExpr = "((Integer)" + inExpr + ").intValue()";
					    } else if (inType.equals(java_lang_String)) {
					        attributeValueExpr = "Integer.parseInt(" + inExpr + ")";
					    }
					} else if (primitiveType == JPrimitiveType.LONG) {
						if (inType.equals(JPrimitiveType.INT) || inType.equals(JPrimitiveType.DOUBLE) || inType.equals(JPrimitiveType.FLOAT)) {
							attributeValueExpr = "((long)"+inExpr+")";
						} else if (inType.equals(getType("java.lang.Long"))) {
					        attributeValueExpr = inExpr + ".longValue()";
					    } else if (inType.equals(java_lang_Object)) {
					        attributeValueExpr = "((Long)" + inExpr + ").longValue()";
					    } else if (inType.equals(java_lang_String)) {
					        attributeValueExpr = "Long.parseLong(" + inExpr + ")";
					    }
					} else if (primitiveType == JPrimitiveType.DOUBLE) {
						if (inType.equals(JPrimitiveType.INT) || inType.equals(JPrimitiveType.LONG) || inType.equals(JPrimitiveType.FLOAT)) {
							attributeValueExpr = "((double)"+inExpr+")";
						} else if (inType.equals(getType("java.lang.Double"))) {
					        attributeValueExpr = inExpr + ".doubleValue()";
					    } else if (inType.equals(java_lang_Object)) {
					        attributeValueExpr = "((Double)" + inExpr + ").doubleValue()";
					    } else if (inType.equals(java_lang_String)) {
					        attributeValueExpr = "Double.parseDouble(" + inExpr + ")";
					    }
					} else if (primitiveType == JPrimitiveType.FLOAT) {
						if (inType.equals(JPrimitiveType.INT) || inType.equals(JPrimitiveType.LONG) || inType.equals(JPrimitiveType.DOUBLE)) {
							attributeValueExpr = "((float)"+inExpr+")";
						} else if (inType.equals(getType("java.lang.Float"))) {
					        attributeValueExpr = inExpr + ".floatValue()";
					    } else if (inType.equals(java_lang_Object)) {
					        attributeValueExpr = "((Float)" + inExpr + ").floatValue()";
					    } else if (inType.equals(java_lang_String)) {
					        attributeValueExpr = "Float.parseFloat(" + inExpr + ")";
					    }
					} else {
						logger.log(TreeLogger.ERROR, "Primitive output type not handled yet: "+outType, null);
						return null;
					}
				}
            } else if((primitiveType = inType.isPrimitive()) != null && outType.equals(java_lang_String)) {
            	attributeValueExpr = "String.valueOf("+inExpr+")";
            }
            return attributeValueExpr;
        }

		public String callAsyncGetter(String asyncGetter, String callback) {
			if(asyncGetter.endsWith(",")) {
				return asyncGetter + " " + callback +")";
			} else {
				return asyncGetter + "(" + callback + ")";
			}
		}
		
		/**
		 * Provide access to a non-async value in transition in order
		 * to perform some kind of transformation on it.
		 * @author dobes
		 *
		 */
		class OperatorInfo {
			public String onGetExpr(String expr) throws UnableToCompleteException {
				return expr;
			}
			public String onSetExpr(String expr) throws UnableToCompleteException {
				return expr;
			}
			
		}
        class ExpressionInfo {
        	final String getter;
            final String setter;
            final String asyncGetter;
            final String asyncSetter;
            final boolean constant;
            final ExpressionInfo baseExpression;
            final JType type;
            ArrayList<OperatorInfo> operators;
            
            public ExpressionInfo(String getter, String setter, JType type, boolean asyncGetter, boolean asyncSetter, boolean constant) {
                super();
                if(asyncGetter) {
                	this.asyncGetter = getter;
                	this.getter = null;
                }
                else {
                	this.getter = getter;
                	this.asyncGetter = null;
                }
                if(asyncSetter) {
                	this.asyncSetter = setter;
                	this.setter = null;
                }
                else {
                	this.setter = setter;
                	this.asyncSetter = null;
                }
                this.type = type;
                this.constant = constant;
                this.baseExpression = null;
            }

            public boolean hasSetter() {
				return setter != null || asyncSetter != null;
			}

			public boolean hasGetter() {
				return getter != null || asyncGetter != null;
			}

			public ExpressionInfo(String getter, String setter, JType type) {
				this(getter, setter, type, false, false, false);
			}

            public ExpressionInfo(String expr, JType type, boolean constant) {
				this(expr, null, type, false, false, constant);
			}

            public ExpressionInfo(ExpressionInfo x, OperatorInfo operator) {
            	this.getter = x.getter;
            	this.setter = x.setter;
            	this.asyncGetter = x.asyncGetter;
            	this.asyncSetter = x.asyncSetter;
            	this.constant = false;
            	this.type = x.type;
            	this.baseExpression = null;
            	addOperator(operator);
			}

			String applyGetOperators(String expr) throws UnableToCompleteException {
				if(operators == null) return expr;
            	for (Iterator i = operators.iterator(); i.hasNext();) {
					OperatorInfo oper = (OperatorInfo) i.next();
					expr = oper.onGetExpr(expr);
				}
            	return expr;
            }
            String applySetOperators(String expr) throws UnableToCompleteException {
				if(operators == null) return expr;
            	for (Iterator i = operators.iterator(); i.hasNext();) {
					OperatorInfo oper = (OperatorInfo) i.next();
					expr = oper.onSetExpr(expr);
				}
            	return expr;
            }
			protected String copyStatement(ExpressionInfo src) throws UnableToCompleteException {
                String converted = src.conversionExpr(type);
                if(converted == null) {
                    logger.log(TreeLogger.ERROR, "Unable to convert "+src.type.getQualifiedSourceName()+" to "+type.getQualifiedSourceName()+" for "+setter+" = "+(src.getter!=null?src.getter:src.asyncGetter), null);
                    throw new UnableToCompleteException();
                }
                return callSetter(setter, applySetOperators(converted)).toString();
            }

            protected String conversionExpr(JType targetType) throws UnableToCompleteException {
            	if(getter == null) {
            		logger.log(TreeLogger.ERROR, "This expression is not async - use asyncCopyStatement() for async support!", new Error());
            		throw new UnableToCompleteException();
            	}
                String converted = converter(getter, type, targetType);
				if(converted == null)
					return null;
				String postOp = applyGetOperators(converted);
				if(postOp == null) {
					logger.log(TreeLogger.ERROR, "Converted type successfully, but applying operators returned null!", null);
				}
				return postOp;
            }
            
            /**
             * Asynchronous copy (load) from one expression to another.
             * 
             * @param src Source value to read from
             * @param callback Expression string for the callback to invoke on completion
             * @param maySkipCallback If true, and both this and src are not asynchronous, doesn't call the callback
             * @return A statement to be put into the source which does the copy
             */
            protected String asyncCopyStatement(ExpressionInfo src, String callback, boolean maySkipCallback) throws UnableToCompleteException {
            	if(setter != null) {
            		if(src.getter != null) {
            			if(maySkipCallback)
            				return copyStatement(src);
            			else
            				return copyStatement(src)+callback+".onSuccess();";
            		} else if(src.asyncGetter != null) {
            			String converted = converter(src.applyGetOperators(converter("result", types.getJavaLangObject(), src.type)), src.type, type);
            			if(converted == null) {
            				logger.log(TreeLogger.ERROR, "Can't convert "+src.type+" to "+type+" for copy from "+src.asyncGetter+" to "+setter, null);
            				throw new UnableToCompleteException();
            			}
                    	return callAsyncGetter(src.asyncGetter, "(new AsyncCallbackProxy("+callback+") {" +
            			"public void onSuccess(Object result) {" +
            				"try {" +
                				callSetter(setter, applySetOperators(converted)) +
        						"super.onSuccess(null);" +
    						"} catch(Throwable caught) {" +
        						"super.onFailure(caught);" +
    						"}" +
    					"}" +
        			"})")+";";
            		} else throw new NullPointerException("No getter!");
            	} else if(asyncSetter != null) {
            		if(src.getter != null) {
            			String converted = converter(src.getter, src.type, type);
            			if(converted == null) {
            				logger.log(TreeLogger.ERROR, "Can't convert "+src.type+" to "+type+" for copy from "+src.getter+" to "+asyncSetter, null);
            				throw new UnableToCompleteException();
            			}
                    	return callSetter(asyncSetter, converted, callback).toString();
            		} else if(src.asyncGetter != null) {
            			String converted = converter(src.applyGetOperators(converter("result", types.getJavaLangObject(), src.type)), src.type, type);
            			if(converted == null) {
            				logger.log(TreeLogger.ERROR, "Can't convert "+src.type+" to "+type+" for copy from "+src.asyncGetter+" to "+asyncSetter, null);
            				throw new UnableToCompleteException();
            			}
                    	return callAsyncGetter(src.asyncGetter, "new AsyncCallbackProxy("+callback+") {" +
                    			"public void onSuccess(Object result) {" +
                    				callSetter(asyncSetter, applySetOperators(converted)+", callback") +
                    			"}}")+";";
            		} else throw new NullPointerException("No getter!");
            	} else throw new NullPointerException("No setter!");
            }

			public String getGetter() {
				return getter;
			}

			public String getSetter() {
				return setter;
			}

			public String getAsyncGetter() {
				return asyncGetter;
			}

			public String getAsyncSetter() {
				return asyncSetter;
			}

			public boolean isConstant() {
				return constant;
			}

			public JType getType() {
				return type;
			}

			@Override
			public String toString() {
				return getter!=null?getter:asyncGetter!=null?asyncGetter:setter!=null?setter:asyncSetter!=null?asyncSetter:type.toString();
			}
			public void addOperator(OperatorInfo info) {
				if(operators == null) operators = new ArrayList<OperatorInfo>();
				operators.add(info);
			}
        }

    }
	public static StringBuffer callSetter(String setter, String value) throws UnableToCompleteException {
		StringBuffer sb = new StringBuffer(setter);
		if(!(setter.endsWith("(") || setter.endsWith(",")))
			sb.append('(');
		return sb.append(value).append(");");
	}
	public static StringBuffer callSetter(String setter, String value, String callback) throws UnableToCompleteException {
		StringBuffer sb = new StringBuffer(setter);
		if(!(setter.endsWith("(") || setter.endsWith(",")))
			sb.append('(');
		return sb.append(value).append(", ").append(callback).append(");");
	}


    @Override
	protected GeneratorInstance createGeneratorInstance() {
        return new GeneratorInstance();
    }
}
