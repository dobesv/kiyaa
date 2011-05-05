package com.habitsoft.kiyaa.rebind;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.rebind.typeinfo.ExpressionInfo;
import com.habitsoft.kiyaa.rebind.typeinfo.GeneratedClassInfo;
import com.habitsoft.kiyaa.rebind.typeinfo.GeneratedInnerClassInfo;
import com.habitsoft.kiyaa.rebind.typeinfo.GeneratorMethodInfo;
import com.habitsoft.kiyaa.rebind.typeinfo.GeneratorTypeInfo;
import com.habitsoft.kiyaa.rebind.typeinfo.JClassTypeWrapper;
import com.habitsoft.kiyaa.rebind.typeinfo.JTypeWrapper;
import com.habitsoft.kiyaa.rebind.typeinfo.PrimitiveTypeInfo;
import com.habitsoft.kiyaa.rebind.typeinfo.RuntimeClassWrapper;
import com.habitsoft.kiyaa.util.Name;
import com.habitsoft.kiyaa.views.GeneratedHTMLView.ActionMethod;
import com.habitsoft.kiyaa.views.GeneratedHTMLView.TemplatePath;
import com.habitsoft.kiyaa.views.ModelView;
import com.habitsoft.kiyaa.views.View;
import com.habitsoft.kiyaa.views.ViewFactory;
import com.habitsoft.xhtml.dtds.FailingEntityResolver;
import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;
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
 * - "#{...}" is evaluated during load to set the target attribute, and
 *   during save the attribute is copied back into the same location by
 *   changing getters to setters.
 * - "%{...}" is a constant read-only expression, evaluated just once
 *   during initialization
 * - "${...}" refers to a read-only expression, which is evaluated during
 *   load, after the base class load() if any.
 * - "@{...}" is an "early load" read-only expression, which is evaluated
 *   during load, before the base class load() if any.  If there is no
 *   base class load this is equivalent to "${...}"
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
	public static class ActionInfo {
        final String action;
        final boolean object;
        final boolean async;
        final boolean saveBefore;
        final boolean loadAfter;
        final String targetView;
		final String origExpr;
		final int timeout;
        
        private ActionInfo(String origExpr, String action, boolean object, boolean async, String targetView, boolean saveBefore, boolean loadAfter, int timeout) {
        	this.origExpr = origExpr;
            this.action = action;
            this.object = object;
            this.async = async;
            this.saveBefore = saveBefore;
            this.loadAfter = loadAfter;
            this.targetView = targetView;
            this.timeout = timeout;
        }
        
        /**
         * One or more statements that perform the required action.
         * 
         * When async == true this will return success or failure to
         * a callback parameter named "callback".
         * 
         * When async == false this will not invoke any callback.
         */
        public String getAction() {
            return action;
        }
        public boolean isAsync() {
            return async;
        }
        public boolean issaveBefore() {
            return saveBefore;
        }
        public boolean isloadAfter() {
            return loadAfter;
        }
        public int getTimeout() {
			return timeout;
		}
        
        /**
         * @param callbackExpr AsyncCallback to return errors or success to
         * @param callbackOptional True if the callback expression doesn't have to be called (i.e. group.<Void>member() or AsyncCallbackFactory.<Void>defaultNewInstance() calls)
         * @return one or more statements separated by a semicolon to execute this action.
         */
        public String toString(String callbackExpr, boolean callbackOptional) {
            if(object || async || saveBefore) {
                if(saveBefore || loadAfter) {
                    return "ViewAction.performOnView("+(action==null?"null":toActionCtor())+", "+targetView+", "+saveBefore+", "+loadAfter+(timeout>0?", "+timeout:"")+", "+callbackExpr+");";
                } else if(action == null) {
                    return "";
                } else {
                    return toActionCtor()+".perform("+callbackExpr+");";
                }
            } else {
                String syncAction=action;
                if(syncAction == null) syncAction = "";
                else syncAction = "try { "+syncAction+" } catch(Throwable t) { "+callbackExpr+".onFailure(t); return; }";
                if(loadAfter) {
                    // Not async, and no load before
                    return syncAction+targetView+".load("+callbackExpr+");";
                } else if(callbackOptional) {
                    // Not async, no load before, no save after
                    return syncAction;
                } else {
                    // Not async, no load before, no save after, still have to call the callback, though.
                    return syncAction+callbackExpr+".onSuccess(null);";
                }
            }
        }

        public String toViewAction() {
            if(saveBefore || loadAfter)
                return "new ViewAction("+toActionCtor()+", "+targetView+", "+saveBefore+", "+loadAfter+(timeout>0?", "+timeout:"")+")";
            else
                return toActionCtor();
        }
        private String toActionCtor() {
            String actionObj = object ? action
                : async ? "new Action(\""+escape(origExpr)+"\") { public void perform(AsyncCallback callback) { "+(timeout>0?"if(callback instanceof AsyncCallbackExtensions) ((AsyncCallbackExtensions)callback).resetTimeout("+timeout+"); ":"")+action+" }}"
                : "new Action(\""+escape(origExpr)+"\") { public void perform(AsyncCallback callback) { try { "+action+" callback.onSuccess(null); } catch(Throwable t) { callback.onFailure(t); }}}";
            return actionObj;
        }
    }
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

		public GeneratorTypeInfo getViewClass(TypeOracle types) throws NotFoundException {
			return JTypeWrapper.wrap(types.getType(getViewClassName()));
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
    
    /**
	 * Provide access to a non-async value in transition in order
	 * to perform some kind of transformation on it.
	 * @author dobes
	 *
	 */
    public static class OperatorInfo {
		public String onGetExpr(String expr) throws UnableToCompleteException {
			return expr;
		}
		public String onSetExpr(String expr) throws UnableToCompleteException {
			return expr;
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

    	public static final String KIYAA_CORE_TAGS_NAMESPACE = "http://habitsoft.com/kiyaa/core";
        public static final String KIYAA_VIEW_TAGS_NAMESPACE = "http://habitsoft.com/kiyaa/ui";
        public static final String SUBVIEW_CLASS_NAME_ATTRIBUTE = "subviewClassName";
		public static final String PARENT_VIEW_FIELD_NAME = "_pv";
    	public static final String ROOT_VIEW_FIELD_NAME = "_root";

        static final String XHTML_NAMESPACE = "http://www.w3.org/1999/xhtml";
        static boolean tagLibrariesLoaded=false;
        static long lastTagLibraryLoad = 0;
        static HashMap<String, TagLibrary> tagLibraries = new HashMap<String, TagLibrary>();
        static HashMap<String, String> namespaces = new HashMap<String, String>();
        protected ClassGenerator rootClassGenerator;
        protected Element rootElement;
        protected int subviewNumber;
        
        public static class SubviewToGenerate {
        	public String name;
        	public Element element;        	
        	public GeneratedClassInfo parentViewClass;
			public SubviewToGenerate(String name, Element element, GeneratedClassInfo parentViewClass) {
				super();
				this.name = name;
				this.element = element;
				this.parentViewClass = parentViewClass;
			}
        	
        }
        protected LinkedList<SubviewToGenerate> subviewsToGenerate = new LinkedList<SubviewToGenerate>();
        
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
            String templatePath = getSimpleClassName(baseType, ".") + ".xhtml";
            final TemplatePath annotation = baseType.getAnnotation(TemplatePath.class);
            if(annotation != null) {
                templatePath = annotation.value();
                //System.out.println("Found TemplatePath annotation on "+baseType+" with value "+templatePath);
            }
            
            rootElement = loadAndParseTemplate(templatePath);
            if (rootElement.getAttribute("with-model") != null) {
                final String modelViewClassName = ModelView.class.getName();
                this.composerFactory.addImplementedInterface(modelViewClassName);
            }
        }

		protected Element loadAndParseTemplate(String templatePath) throws Error,
				UnableToCompleteException {
			XMLReader reader;
			try {
				reader = XMLReaderFactory.createXMLReader();
			} catch (SAXException caught1) {
				throw new Error(caught1);
			}
			// Load XHTML declaration from the jar file, otherwise fail if someone wants to load a DTD
            reader.setEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
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
                    final InputStream resourceAsStream = getClass().getResourceAsStream(templatePath);
                    try {
                        d = b.build(resourceAsStream);
                    } finally {
                        resourceAsStream.close();
                    }
                } else {
                    d = b.build(f);
                }
            } catch (Exception caught) {
                logger.log(TreeLogger.ERROR, "Failed to load template '" + templatePath + ": "+caught.toString(), null);
                throw new UnableToCompleteException();
            }
            return getComponentRootElement(d);
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
            GeneratedClassInfo rootViewClass = new GeneratedClassInfo(implName, JClassTypeWrapper.wrap(baseType));
			rootClassGenerator.generateClassBody(rootElement, rootViewClass, null, rootViewClass);
			
			// Now generate the subview classes create as part of creating those views
			generateSubviewClasses(rootViewClass);
        }

        private void generateSubviewClasses(GeneratedClassInfo rootViewClass) throws UnableToCompleteException {
            while(!subviewsToGenerate.isEmpty()) {
            	SubviewToGenerate sv = subviewsToGenerate.removeFirst();
                Element elem = sv.element;
                String subviewClassName = sv.name;
                
                pushLogger("Inside subview element "+elem.getQualifiedName()+" class name "+subviewClassName);
                try {
                    boolean isModelView = elem.getAttribute("with-model") != null;
                    sw.println("protected static class " + subviewClassName
                                    + " implements "+(isModelView?"ModelView":"View")+" {");
                    sw.indent();
                    GeneratedClassInfo genClass = new GeneratedInnerClassInfo(subviewClassName, rootViewClass, commonTypes.object, true);
                    
                    if(isModelView)
                        genClass.addImplementedInterface(getType(ModelView.class.getName()));
                    new ClassGenerator().generateClassBody(elem, genClass, sv.parentViewClass, rootViewClass);
                    sw.outdent();
                    sw.println("}");
                } finally {
                	popLogger();
                }
            }
        }

        
        class ClassGenerator {
			protected HashMap<String, Element> insertedViews = new HashMap<String, Element>();
            protected HashMap<String, String> insertedText = new HashMap<String, String>();
            protected HashMap<String, String> values = new HashMap<String, String>();
            protected HashMap<String,ActionInfo> actions = new HashMap<String, ActionInfo>();
            protected ArrayList<String> memberDecls = new ArrayList<String>();
            protected ArrayList<String> calculations = new ArrayList<String>();
            protected ArrayList<String> asyncProxies = new ArrayList<String>();
            protected ArrayList<String> earlyLoads = new ArrayList<String>();
            protected ArrayList<String> earlyAsyncLoads = new ArrayList<String>();
            protected ArrayList<String> loads = new ArrayList<String>();
            protected ArrayList<String> asyncLoads = new ArrayList<String>();
            protected ArrayList<String> subviewLoads = new ArrayList<String>();
            protected ArrayList<String> saves = new ArrayList<String>();
            protected ArrayList<String> clearFields = new ArrayList<String>();
            protected LinkedHashSet<String> fieldNames = new LinkedHashSet<String>();
            protected GeneratorTypeInfo myModelClass;
            protected String myModelVarName;
			protected final ArrayList<SubviewInfo> subviews = new ArrayList<SubviewInfo>();
			protected boolean hasHtml=false;
			protected boolean useInnerHTML=false;
			protected Element myRootElement;
			protected GeneratedClassInfo myClass;
			protected GeneratedClassInfo parentViewClass;
			protected GeneratedClassInfo rootViewClass;
			
			private void generateClassBody(Element rootElement, GeneratedClassInfo myClass, GeneratedClassInfo parentViewClass, GeneratedClassInfo rootViewClass)
                            throws UnableToCompleteException {
				this.myRootElement = rootElement;
                this.myClass = myClass;
                this.parentViewClass = parentViewClass;
                this.rootViewClass = rootViewClass;
                
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
                    generateField(myModelVarName, myModelClass);
                } else if (myClass.implementsInterface(commonTypes.modelView)) {
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
	                    generateField(fieldName, JTypeWrapper.wrap(fieldType));
					}
                }
                if(parentViewClass != null) {
                    generateField(PARENT_VIEW_FIELD_NAME, parentViewClass);
                    generateField(ROOT_VIEW_FIELD_NAME, rootViewClass);
                }
                if (myModelClass != null && !myModelVarName.equals("model")) {
                    myClass.addField(myModelVarName, myModelClass);
                    sw.println("public Object getModel() {");
                    sw.indentln("return " + myModelVarName + ";");
                    sw.println("}");
                    myClass.addGetter("model", RuntimeClassWrapper.OBJECT, "getModel", false);
                }
                
                sw.println("public void validate(AsyncCallback callback) {");
                sw.indentln("callback.onFailure(new Error(\"Not implemented\"));");
                sw.println("}");
                myClass.addMethod("validate", PrimitiveTypeInfo.VOID, commonTypes.asyncCallback);
                
                // Make clearFields available as an action
                myClass.addMethod("clearFields", PrimitiveTypeInfo.VOID);

                // Two results of this operation:
                // the template string
                // the code to insert widgets into it
                sw.println("public void addFields() {");
                sw.indent();
                sw.println("try {");
                sw.indent();
                
                parseTree(rootElement);
                for (SubviewInfo subviewInfo : subviews) {
                    generateSubview(subviewInfo);
				}
                sw.println("} catch(Throwable t) {");
                sw.indent();
                sw.println("String whichField;");
                sw.print("if(!didInit) whichField = \"before didInit\";");
                for (String field : fieldNames) {
                    sw.println("else if("+field+" == null) whichField = \"before "+field+"\";");
                }
                sw.println("else whichField = \"after "+(fieldNames.isEmpty()?"didInit":"last field")+"\";");
                sw.println("throw new Error(\""+myClass.getName()+".addFields() threw an exception \"+whichField+\": \"+t, t);");
                sw.outdent();
                sw.println("}");
                sw.outdent();
                sw.println("}");
                myClass.addMethod("addFields", PrimitiveTypeInfo.VOID);
                
                generatePanel();
                
                generateRemoveFields();

                if(hasHtml)
                	generateTemplate(rootElement);

                generateConstructor(rootElement);
                generateMemberDecls();
                generateLoad();
                generateSave();
                generateClearFields();

                for (Iterator<String> i = asyncProxies.iterator(); i.hasNext();) {
					String line = i.next();
					sw.println(line);
				}
                
                for (Iterator<String> i = calculations.iterator(); i.hasNext();) {
					String line = i.next();
					sw.println(line);
				}
                
                
                if (myModelClass != null)
                    generateSetModel();
            }

			private void generatePanel() throws UnableToCompleteException {
				String rootView = getRootView(false);
				if(hasHtml) {
                    sw.println("protected ComplexHTMLPanel panel = new ComplexHTMLPanel();");
                    sw.println("protected void addWidget(String id, Widget widget) {");
                    sw.indentln("panel.replace("+rootView+".maybeEnsureDebugId(id, widget), id);");
                    sw.println("}");
                    
                } else {
                    sw.println("protected FlowPanel panel = new FlowPanel();");
                    sw.println("protected void addWidget(String id, Widget widget) {");
                    sw.indentln("panel.add("+rootView+".maybeEnsureDebugId(id, widget));");
                    sw.println("}");
                }
				if(parentViewClass == null) {
	                sw.println("protected <T extends View> T maybeEnsureDebugId(String id, T view) { maybeEnsureDebugId(id, view.getViewWidget()); return view; }");
	                sw.println("protected Widget maybeEnsureDebugId(String id, Widget widget) {");
	                sw.indent();
	                sw.println("try {");
	                sw.indent();
	                sw.println("String panelId = panel.getElement().getId();");
	                sw.println("if(panelId != null && panelId.startsWith(UIObject.DEBUG_ID_PREFIX))");
	                sw.indentln("widget.ensureDebugId(panelId.substring(UIObject.DEBUG_ID_PREFIX.length())+'-'+id);");
	                sw.outdent();
	                sw.println("} catch(Throwable t) {com.allen_sauer.gwt.log.client.Log.warn(\"ensureDebugId failed on \"+widget+\": \"+t);}");
	                sw.println("return widget;");
	                sw.outdent();
	                sw.println("}");
				}
                sw.println("protected void addView(String id, View view) {");
                sw.indentln("addWidget(id, view.getViewWidget());");
                sw.println("}");
                sw.println("public Widget getViewWidget() {");
                sw.indentln("return panel;");
                sw.println("}");
			}

			protected void generateField(String fieldName, GeneratorTypeInfo fieldType) {
                myClass.addField(fieldName, fieldType);
                memberDecls.add(fieldType.getParameterizedQualifiedSourceName() + " " + fieldName + ";");
                fieldNames.add(fieldName);
            }

            protected void generateConstructor(Element rootElement) throws UnableToCompleteException {
                // Try to add what fields we can, although the model may be
                // null, we might have other fields.
                // In fact, this form might operate perfectly well on a null
                // model.
            	String ctorArgs = "";
				if(parentViewClass != null) {
            		ctorArgs = parentViewClass.getName()+" parentView, "+rootViewClass.getName()+" rootView";            		
            	}
                sw.println("public " + myClass.getSimpleSourceName() + "("+ctorArgs+") {");
                sw.indent();
                
            	if(parentViewClass != null) {
                    sw.println("this."+PARENT_VIEW_FIELD_NAME+" = parentView;");
                    sw.println("this."+ROOT_VIEW_FIELD_NAME+" = rootView;");
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
                sw.println(myClass.getName()+" init() {");
                sw.indent();
                sw.println("if(didInit) return this;");
                sw.println("didInit = true;");
            	if(hasHtml) {
            		if(useInnerHTML) {
	                    sw.println("panel.setTemplate(TEMPLATE);");
	                } else {
	                    sw.println("panel.setDomTemplate(generateDomTree());");
	                }
                }
                sw.println("addFields();");
                
                // if(!usesModel && subviewClass) {
                // sw.println("setModel(null, null);");
                // }
                if (parentViewClass == null)
                    generateAttributes(rootElement, JTypeWrapper.wrap(baseType), "this");
                sw.println("return this;");
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
        					String value = a.getValue();
        					if(value.matches("[$#%@]\\{.*\\}$"))
        					    value = "";
                            if(a.getLocalName().equalsIgnoreCase("class")) {
        						sw.println("DOM.setElementProperty("+eltVar+", \"className\", \""+backslashEscape(value)+"\");");
        					} else {
        						sw.println("DOM.setElementAttribute("+eltVar+", \""+a.getLocalName()+"\", \""+backslashEscape(value)+"\");");
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
        					final String value = child.getValue();
        					if(!value.matches("[$#%@]\\{.*\\}$"))
        					    sw.println("DOM.setInnerText("+parentEltVar+", \""+backslashEscape(value)+"\");");
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
                    sw.println(line.replaceAll("<[^>]*>", "")); // strip generics
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
            	String name = "load";
                final boolean nothingToLoad = loads.isEmpty() && subviewLoads.isEmpty() && asyncLoads.isEmpty() && earlyLoads.isEmpty() && earlyAsyncLoads.isEmpty();
            	boolean baseClassHasSyncLoad = myClass.getSuperclass().hasMethodMatching("load", false, PrimitiveTypeInfo.VOID);
				boolean baseClassHasAsyncLoad = myClass.getSuperclass().hasMethodMatching("load", false, PrimitiveTypeInfo.VOID, commonTypes.asyncCallback);
				final boolean baseClassLoads = baseClassHasSyncLoad || baseClassHasAsyncLoad;
                if(baseClassHasAsyncLoad) {
                	if(nothingToLoad)
                		return;
            		sw.println("public void load(AsyncCallback<Void> callback) {");
            		sw.indent();
            		sw.println("try { init(); } catch(Throwable t) { callback.onFailure(t); return; }");
                    sw.println("callback = new AsyncCallbackDirectProxy<Void>(callback) { public void onSuccess(Void result) { loadImpl(takeCallback()); } };");
            		for(String load : earlyLoads) {
            		    sw.println(load);
            		}
            		if(earlyAsyncLoads.isEmpty()) {
                        sw.println("super.load(callback);");
            		} else {
            		    sw.println("AsyncCallbackGroup group = new AsyncCallbackGroup()");
            		    for(String load : earlyAsyncLoads) {
            		        sw.println(load);
            		    }
            		    sw.println("super.load(group.<Void>member());");
            		    sw.println("group.ready(callback);");
            		}
                    sw.outdent();
            		sw.println("}");
            		name = "loadImpl";
            	}
                sw.println("public void "+name+"(final AsyncCallback callback) {");
                sw.indent();
                if(!baseClassLoads)
                    sw.println("try { init(); } catch(Throwable t) { callback.onFailure(t); return; }");
                if(baseClassHasSyncLoad)
                    sw.println("try { init(); super.load(); } catch(Throwable t) { callback.onFailure(t); return; }");
                if(nothingToLoad) {
                	sw.println("callback.onSuccess(null);");
                } else {
                    sw.println("try {");
                    sw.indent();
                    sw.println("final AsyncCallbackGroup group = new AsyncCallbackGroup(\""+myClass.getName()+".load()\");");
                    if(!baseClassHasAsyncLoad) {
                        for (String load : earlyAsyncLoads) {
                            sw.println(load);
                        }
                        for(String load : earlyLoads) {
                            sw.println(load);
                        }
                    }
                    for (String load : loads) {
                        sw.println(load);
                    }
                    for (String load : asyncLoads) {
                        sw.println(load);
                    }
                    if((asyncLoads.isEmpty() && (baseClassLoads || earlyAsyncLoads.isEmpty())) || subviewLoads.isEmpty()) {
                        for (String load : subviewLoads) {
                            sw.println(load);
                        }
                        sw.println("group.ready(callback);");
                    } else {
                        sw.println("group.ready(new AsyncCallbackDirectProxy<Void>(callback) {");
                        sw.indent();
                        sw.println("public void onSuccess(Void result) {");
                        sw.indent();
                        sw.println("final AsyncCallbackGroup group = new AsyncCallbackGroup(\""+myClass.getName()+".load() (subviews)\");");
                        sw.println("try {");
                        sw.indent();
                        for (String load : subviewLoads) {
                            sw.println(load);
                        }
                        sw.println("group.ready(callback);");
                        sw.outdent();
                        sw.println("} catch(Throwable t) {");
                        sw.indentln("callback.onFailure(t);");
                        sw.println("}");
                        sw.outdent();
                        sw.println("}");
                        sw.outdent();
                        sw.println("});");
                    }
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
            	boolean baseClassHasSave=myClass.getSuperclass().hasMethodMatching("save", false, PrimitiveTypeInfo.VOID, commonTypes.asyncCallback);
            	boolean nothingToSave = saves.isEmpty();
            	if(baseClassHasSave) {
					if(nothingToSave)
                		return;
            		name="saveImpl";
            		sw.println("public void save(AsyncCallback<Void> callback) {");
            		sw.indent();
            		sw.println("super.save(new AsyncCallbackDirectProxy<Void>(callback, \""+myClass.getName()+".save()\") { public void onSuccess(Void result) { "+name+"(takeCallback()); } });");
                    sw.outdent();
            		sw.println("}");
            	} else {
            		name = "save";
            	}
                sw.println("public void "+name+"(final AsyncCallback<Void> callback) {");
                sw.indent();
                if(nothingToSave) {
                	sw.println("callback.onSuccess(null);");
                } else {
                    sw.println("if(!didInit) return;");
                    sw.println("try {");
                    sw.indent();
                    sw.println("AsyncCallbackGroup group = new AsyncCallbackGroup(\""+myClass.getName()+".save()\");");
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
            	boolean baseClassHasClearFields = myClass.getSuperclass().hasMethodMatching("clearFields", false, PrimitiveTypeInfo.VOID);
            	if(baseClassHasClearFields) {
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
                sw.println("this." + myModelVarName + " = (" + myModelClass.getName() + ") model;");
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
                        if(KIYAA_CORE_TAGS_NAMESPACE.equalsIgnoreCase(namespace) && "insert".equals(tag)) {
                    		String templatePath = elem.getAttributeValue("templatePath");
                    		Element newElem = (Element) loadAndParseTemplate(templatePath).copy();
							rootElement.replaceChild(elem, newElem);
							parseTree(newElem);
                    		continue;
                        }
                        GeneratorTypeInfo tagClass = getTagClass(elem);
                        Element viewElem = new Element(XHTML_NAMESPACE.equals(elem.getNamespaceURI())?elem.getLocalName():"div", XHTML_NAMESPACE);
                        String id = identifier(elem.getAttributeValue("id"));
                        if (id == null)
                            id = "view" + insertedViews.size();
                        else {
                        	myClass.addField(id, tagClass);
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
				    String[] split = kc.split(":", 2);
				    if (split.length == 1) {
				    	ln = kc;
				    } else {
				    	ns = namespaces.get(split[0]);
				    	ln = split[1];
				    }
				}
				namespaceAndTag = new String[] {ns,ln};
				return namespaceAndTag;
			}

			protected GeneratorTypeInfo getTagClass(Element elem) throws UnableToCompleteException {
				String[] namespaceAndTag = getNamespaceAndTag(elem);
				String namespace = namespaceAndTag[0];
				String tag = namespaceAndTag[1];
				GeneratorTypeInfo tagClass = null;
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
					TagLibrary tagLibrary = tagLibraries.get(namespace);
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
				                        + ") while looking for class for tag "+tag, null);
				    }
				}
				return tagClass;
			}

            
            private void handleTextSubstitution(Text childNode) throws UnableToCompleteException {
            	String text = collapseWhitespace(childNode.getValue());
            	try {
            		pushLogger("Processing text: \""+text+"\"");
            		childNode.setValue(text);
            		
                	Matcher matcher = Pattern.compile("[$#%@]\\{((\\\\\\}|[^}])*)\\}").matcher(text);
                    int textMarker = 0;
                    boolean areConstant=true;
                    boolean areEarly=true;
                    StringBuffer stringBuildExpr = new StringBuffer();
                	while(matcher.find()) {
                		
                        if(stringBuildExpr.length() > 0) stringBuildExpr.append(" + ");
                        if(matcher.start() > 0)
                        	stringBuildExpr.append('"').append(backslashEscape(text.substring(textMarker, matcher.start()))).append("\" + ");
                        
                		String path = matcher.group(1);
                		char typeChar = matcher.group().charAt(0);
                		if(typeChar != '%') areConstant = false;
                		if(typeChar != '@' && typeChar != '%') areEarly = false; // constant is as good as early :-)
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
                            element.appendChild("$$$");
                            parent.replaceChild(childNode, element);
                		}
                		if(textMarker < text.length()) {
                			stringBuildExpr.append(" + \"").append(backslashEscape(text.substring(textMarker))).append('"');
                		}
                        
                        String setterName = "set" + capitalize(id);
                        memberDecls.add("public final void "+ setterName + "(String newValue) { panel.setText(\"" + id + "\", newValue); }");
                        myClass.addMethod(setterName, PrimitiveTypeInfo.VOID, RuntimeClassWrapper.STRING);
                        
                        //System.out.println("Using string build expression: "+stringBuildExpr+" for "+text);
                		ExpressionInfo expr = findAccessors(stringBuildExpr.toString(), true, true);
                		if(expr == null) {
                			logger.log(TreeLogger.ERROR, "Unable to resolve expression: "+stringBuildExpr, null);
                			throw new UnableToCompleteException();
                		}
                        //System.out.println("Got getter: "+expr.getter);
                		ExpressionInfo textExpr = new ExpressionInfo(expr.getOriginalExpr(), id, setterName, RuntimeClassWrapper.STRING);
                		if((expr.isConstant() || areConstant) && expr.hasSynchronousGetter()) {
                			sw.println(textExpr.copyStatement(expr));
                		} else if(areEarly) {
                		    if(expr.hasSynchronousGetter())
                		        earlyLoads.add(textExpr.copyStatement(expr));
                		    else
                		        earlyAsyncLoads.add(textExpr.asyncCopyStatement(expr, "group.<Void>member()", true));
                		} else {
                			asyncLoads.add(textExpr.asyncCopyStatement(expr, "group.<Void>member()", true));
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
				public GeneratorTypeInfo subviewClass;

				public SubviewInfo(Element elem, String id, String namespace, String tag, GeneratorTypeInfo subviewClass) {
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
                        sw.println(sv.id + " = (" + sv.subviewClass.getParameterizedQualifiedSourceName() + ") GWT.create("
                                        + sv.subviewClass.getName() + ".class);");
                    } else {
                        boolean takesTag = sv.subviewClass.implementsInterface(JClassTypeWrapper.wrap(types.findType("com.habitsoft.kiyaa.views.TakesElementName")));
                        sw.println(sv.id + " = new " + sv.subviewClass.getName() + (takesTag?"(\""+escape(sv.elem.getLocalName())+"\", \""+escape(sv.elem.getNamespaceURI())+"\");":"();"));
                    }
                    generateContents(sv.elem, sv.subviewClass, sv.id);
                    generateAttributes(sv.elem, sv.subviewClass, sv.id);
                    generateSubviewCommon(sv.elem, sv.id, sv.id, "this", sv.subviewClass, false);
				} finally {
					popLogger();
				}
            }

			/**
			 * Generate code to pass along any load(), save(), and clearFields() calls
			 * to subviews that support those operations.
			 */
            private void generateSubviewCommon(Element elem, String id, String viewExpr, String modelExpr,
                            GeneratorTypeInfo viewClass, boolean readOnly) throws UnableToCompleteException {
            	boolean isWidget = viewClass.isSubclassOf(commonTypes.widget);
				String addMethod = isWidget?"addWidget":"addView";
                sw.println(addMethod+"(\"" + id + "\", " + viewExpr + ");");
                boolean isView = viewClass.implementsInterface(commonTypes.view);
				boolean hasLoad = isView || viewClass.hasMethodMatching("load", true, PrimitiveTypeInfo.VOID, commonTypes.asyncCallback);
                if (hasLoad) {
                    subviewLoads.add(viewExpr + ".load(group.<Void>member());");
                }
                if (!readOnly) {
                    boolean hasSave = isView || viewClass.hasMethodMatching("save", true, PrimitiveTypeInfo.VOID, commonTypes.asyncCallback);
                    if (hasSave)
                        saves.add(viewExpr + ".save(group.<Void>member());");
                }
                boolean hasClearFields = isView || viewClass.hasMethodMatching("clearFields", true, PrimitiveTypeInfo.VOID);
                if(hasClearFields) {
                	clearFields.add(viewExpr + ".clearFields();");
                }
            }

            protected void generateAttributes(Element elem, GeneratorTypeInfo type, String name)
                            throws UnableToCompleteException {
                for (int j = 0; j < elem.getAttributeCount(); j++) {
                    final Attribute attr = elem.getAttribute(j);
                    final String key = attr.getLocalName();

                    // These keys are taken care of in an earlier step
                    if ("viewClass".equals(key) || "id".equals(key) || "with-model".equals(key) || "with-vars".equals(key) || "with-var".equals(key) || "kc".equals(key))
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

			private void generateAttribute(GeneratorTypeInfo type, String name, final String key, final String value)
				throws UnableToCompleteException {
				LocalTreeLogger.pushLogger(logger.branch(TreeLogger.INFO, "Attribute "+key+"='"+value+"' in element "+name+" which is a "+type));
				try {
					ExpressionInfo baseExpr = new ExpressionInfo(name, name, type, false);
					ExpressionInfo attributeAccessors = findAccessors(baseExpr, key, true, false);
					// Automatically propagate some properties to the getViewWidget()
					if(attributeAccessors == null && key.matches("visible|width|height|title") && type.implementsInterface(commonTypes.view)) 
						attributeAccessors = findAccessors(baseExpr, "viewWidget."+key, true, false);
					if(key.equals("class")) {
						if(type.implementsInterface(commonTypes.view))
							attributeAccessors = findAccessors(baseExpr, "viewWidget.styleName", false, false);
						else if(type.isSubclassOf(commonTypes.uiObject))
							attributeAccessors = findAccessors(baseExpr, "styleName", false, false);
						else
							logger.log(TreeLogger.WARN, "Found attribute 'class' on something that isn't a View or Widget (a "+type+")");
					}
					boolean readOnly = false;
					boolean constant = false;
					boolean earlyLoad = false;
					boolean isExpr=false;
					String path = null;
					ExpressionInfo pathAccessors = null;
					if (((readOnly = (value.startsWith("${") 
							|| (earlyLoad = value.startsWith("@{")) 
							|| (constant = value.startsWith("%{")))) 
							|| value.startsWith("#{")) && value.endsWith("}")) {
					    path = value.substring(2, value.length() - 1).trim();
					    pathAccessors = findAccessors(path, false, true);
					    isExpr=true;
					}
					String valueExpr;
					ActionInfo action;
					if ("class".equals(key) && pathAccessors == null) {
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
					                    + type + "; value is '" + value + "', getter is "+attributeAccessors+" asyncGetter is "+attributeAccessors.getAsyncGetter(), null);
					    throw new UnableToCompleteException();
					} else {
						final GeneratorTypeInfo attributeType = attributeAccessors.getType();
						if (attributeType.equals(commonTypes.action)
							        && (pathAccessors == null || !pathAccessors.hasGetter()) 
							        && (action = getAction(value, false)) != null) {
							if(attributeAccessors.hasSynchronousSetter() == false) {
								logger.log(TreeLogger.ERROR, "Async setters do not support for Actions yet.", null);
								throw new UnableToCompleteException();
							}
						    sw.println(attributeAccessors.callSetter(action.toViewAction()).toString());
						} else if (path != null // If this is a ${...} or #{...}
								&& commonTypes.value.equals(attributeType) // and the target attribute accepts a Value object
							    ) {
							valueExpr = getFieldValue(path);
							if(valueExpr == null) {
								logger.log(TreeLogger.ERROR, "Failed to evaluate expression to construct Value object for "+key+"="+path+" on "+type, null);
								throw new UnableToCompleteException();
							}
							if(attributeAccessors.hasSynchronousSetter() == false) {
								if(attributeAccessors.hasAsynchronousSetter())
									logger.log(TreeLogger.ERROR, "Async setters not supported for Value yet; found use of async setter "+attributeAccessors.getAsyncSetter()+" for attribute "+key+" on "+type+" to store value "+path, null);
								else
									logger.log(TreeLogger.ERROR, "No setter found for attribute "+key+" on "+type, null);
								throw new UnableToCompleteException();
							}
						    sw.println(attributeAccessors.callSetter(valueExpr).toString());
						} else if (pathAccessors != null && pathAccessors.hasGetter()) {
						    generateAttributeLoadSave(type, attributeAccessors, pathAccessors, readOnly, constant, earlyLoad);
						} else if (path != null && (valueExpr = getFieldValue(path)) != null) {
							logger.log(TreeLogger.WARN, "Using a Value "+valueExpr+"to read "+path+" for attribute "+key+"="+path);
							ExpressionInfo valueAccessors = findAccessors(new ExpressionInfo(path, valueExpr, commonTypes.value, true), "value", true, false);
							generateAttributeLoadSave(type, attributeAccessors, valueAccessors, readOnly, constant, earlyLoad);
						} else if(isExpr) {
							logger.log(TreeLogger.WARN, "Couldn't figure out how to set attribute "+key+" on "+type+"; couldn't find a getter for "+value, null);                    	
						} else if (attributeType.equals(getType("java.lang.String"))) {
							ExpressionInfo valueAccessors = new ExpressionInfo(path, "\"" + backslashEscape(value) + "\"", getType("java.lang.String"), true);
							generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true, true, true);
						} else if (attributeType.isEnum()) {
							if(attributeType.getEnumMembers().contains(value)) {
						        ExpressionInfo valueAccessors = new ExpressionInfo(path, attributeType.getParameterizedQualifiedSourceName()+"."+value, attributeType, true);
						        generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true, true, true);
						    } else {
						        logger.log(TreeLogger.ERROR, "Enum constant '" + value + "' not found in enum "+attributeType.getParameterizedQualifiedSourceName()+" for attribute "+key,
						            null);
						        throw new UnableToCompleteException();
						    }
						} else if (attributeType.equals(getType("java.lang.Boolean"))) {
						    if (!"true".equals(value) && !"false".equals(value)) {
						        logger.log(TreeLogger.ERROR, "Boolean attribute '" + key + "' should be true or false; got '"+value+"'",
						                        null);
						        throw new UnableToCompleteException();
						    }
							ExpressionInfo valueAccessors = new ExpressionInfo(path, "Boolean." + value.toUpperCase(), attributeType, true);
							generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true, true, true);
						} else if (attributeType.getName().equals("boolean")) {
						    if (!"true".equals(value) && !"false".equals(value)) {
						        logger.log(TreeLogger.ERROR, "Boolean attribute '" + key + "' should be true or false; got '"+value+"'",
						                        null);
						        throw new UnableToCompleteException();
						    }
							ExpressionInfo valueAccessors = new ExpressionInfo(path, value, attributeType, true);
							generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true, true, true);
						} else if (attributeType.getName().equals("char")) {
							ExpressionInfo valueAccessors = new ExpressionInfo(path, "'"+backslashEscape(value)+"'", attributeType, true);
							generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true, true, true);					
						} else if (attributeType.isPrimitive()) {
							ExpressionInfo valueAccessors = new ExpressionInfo(path, value, attributeType, true);
							generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true, true, true);
						} else if (attributeType.equals(getType("java.lang.Class"))) {
						    try {
						    	ExpressionInfo valueAccessors = new ExpressionInfo(path, types.getType(value).getQualifiedSourceName()
									    + ".class", attributeType, true);
						    	generateAttributeLoadSave(type, attributeAccessors, valueAccessors, true, true, true);
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
				} finally {
					LocalTreeLogger.popLogger();
				}
			}

			private String generateSetStyle(GeneratorTypeInfo type, String name, final String key, final String value)
				throws UnableToCompleteException {
				// Assuming that we either have a View or a Widget ...
				String widgetExpr;
				if (type.implementsInterface(commonTypes.view)) {
				    widgetExpr = name + ".getViewWidget()";
				} else if (type.isSubclassOf(commonTypes.widget)) {
				    widgetExpr = name;
				} else {
				    logger.log(TreeLogger.ERROR, "Don't know how to set the style of a " + type, null);
				    throw new UnableToCompleteException();
				}
				sw.println("DOM.setElementAttribute(" + widgetExpr + ".getElement(), \"" + escape(key)
				                + "\", \"" + escape(value) + "\");");
				return widgetExpr;
			}

			private String generateSetClass(GeneratorTypeInfo type, String name, final String value)
				throws UnableToCompleteException {
				String widgetExpr;
				if (type.implementsInterface(commonTypes.view)) {
				    widgetExpr = name + ".getViewWidget()";
				} else if (type.isSubclassOf(commonTypes.widget)) {
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

			private void generateBinding(GeneratorTypeInfo type, String name, String value)
				throws UnableToCompleteException {
				if(value.startsWith("${") || value.startsWith("#{") || value.startsWith("%{") || value.startsWith("@{")) value = value.substring(2);
				if(value.endsWith("}")) value = value.substring(0, value.length()-1);
				
				ExpressionInfo accessors = findAccessors(value, false, false);
				if (accessors != null && accessors.hasSetter()) {
				    sw.println(accessors.copyStatement(new ExpressionInfo(name, name, type, false)));
				} else {
				    logger.log(TreeLogger.WARN, "Unable to find a "+(accessors == null?"property":"setter method")+" for binding expression: " + value, null);
				}
			}

			private ActionInfo generateOnChangeListener(GeneratorTypeInfo type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!type.implementsInterface(commonTypes.sourcesChangeEvents)) {
				    logger.log(TreeLogger.ERROR, "onchange attribute must be on a View/Widget" +
				    		" that implements SourcesChangeEvents.", null);
				    throw new UnableToCompleteException();
				}
				ActionInfo actionExpr = getAction(value, true);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value+" for an onchange handler", null);
				    throw new UnableToCompleteException();
				}
				attachWidgetEventListener(name, actionExpr, "ChangeListener", "onChange(Widget sender)", null, value);
				return actionExpr;
			}

			private ActionInfo generateOnFocusListener(GeneratorTypeInfo type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!type.implementsInterface(commonTypes.sourcesFocusEvents)) {
				    logger.log(TreeLogger.ERROR, "onfocus attribute must be on a View/Widget" +
				    		" that implements SourcesFocusEvents.", null);
				    throw new UnableToCompleteException();
				}
				ActionInfo actionExpr = getAction(value, true);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value+" for an onfocus handler", null);
				    throw new UnableToCompleteException();
				}
				sw.println(name + ".add" + "FocusListener" + "(new " + "FocusListener" + "() {");
				sw.indent();
				sw.println("public void " + "onFocus(Widget sender)" + " {");
				sw.indent();
				sw.println(actionExpr.toString("AsyncCallbackFactory.<Void>defaultNewInstance()", true));
				sw.outdent();
				sw.println("}");
				sw.println("public void " + "onLostFocus(Widget sender)" + " { }");
				sw.outdent();
				sw.println("});");
				return actionExpr;
			}

			private ActionInfo generateOnBlurListener(GeneratorTypeInfo type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!type.implementsInterface(commonTypes.sourcesFocusEvents)) {
				    logger.log(TreeLogger.ERROR, "onblur attribute must be on a View/Widget" +
				    		" that implements SourcesFocusEvents.", null);
				    throw new UnableToCompleteException();
				}
				ActionInfo actionExpr = getAction(value, true);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value+" for an onblur handler", null);
				    throw new UnableToCompleteException();
				}
				sw.println(name + ".add" + "FocusListener" + "(new " + "FocusListener" + "() {");
				sw.indent();
				sw.println("public void " + "onLostFocus(Widget sender)" + " {");
				sw.indent();
				sw.println(actionExpr.toString("AsyncCallbackFactory.<Void>defaultNewInstance()", true));
				sw.outdent();
				sw.println("}");
				sw.println("public void " + "onFocus(Widget sender)" + " { }");
				sw.outdent();
				sw.println("});");
				return actionExpr;
			}
			
			private ActionInfo generateOnClickHandler(GeneratorTypeInfo type, String name, final String value,
				ExpressionInfo pathAccessors) throws UnableToCompleteException {
				if(!type.implementsInterface(commonTypes.sourcesClickEvents)) {
				    logger.log(TreeLogger.ERROR, "onclick attribute must be on a View/Widget" +
				    		" that implements SourceClickEvents.", null);
				    throw new UnableToCompleteException();
				}
				ActionInfo actionExpr = getAction(value, true);
				if(actionExpr == null) {
				    logger.log(TreeLogger.ERROR, "Unable to find action for "+value
				        +" for an onclick handler", null);
				    throw new UnableToCompleteException();
				}
				attachWidgetEventListener(name, actionExpr, "ClickListener", "onClick(Widget sender)", null, value);
				return actionExpr;
			}
			private ActionInfo generateKeyPressHandler(GeneratorTypeInfo type, String name, final String value, String keyName)
			throws UnableToCompleteException {
    			if(!type.implementsInterface(commonTypes.sourcesClickEvents)) {
    			    logger.log(TreeLogger.ERROR, name+" attribute must be on a View/Widget" +
    			    		" that implements SourcesKeyEvents.", null);
    			    throw new UnableToCompleteException();
    			}
    			ActionInfo actionExpr = getAction(value, true);
    			if(actionExpr == null) {
    			    logger.log(TreeLogger.ERROR, "Unable to find action for "+value
    			        +" for a onPressXXX handler", null);
    			    throw new UnableToCompleteException();
    			}
    			final String condition = keyName==null?null:"keyCode == "+keyName+" && (modifiers & ~MODIFIER_SHIFT) == 0";
                attachWidgetEventListener(name, actionExpr, "KeyboardListenerAdapter", "onKeyPress(Widget sender, char keyCode, int modifiers)", condition, value);
    			return actionExpr;
    		}
			protected void generateAttributeLoadSave(GeneratorTypeInfo type, ExpressionInfo attributeAccessors, ExpressionInfo pathAccessors,
				boolean readOnly, boolean constant, boolean earlyLoad)
				throws UnableToCompleteException {
				LocalTreeLogger.pushLogger(logger.branch(TreeLogger.INFO, "Attribute load/save for "+attributeAccessors.setterString()+(readOnly?" = ":" <=> ")+pathAccessors));
				try {
					String loadExpr = attributeAccessors.asyncCopyStatement(pathAccessors, "group.<Void>member()", true);
					// Put the value into the widget on load()
					//if(attributeAccessors.getter != null && attributeAccessors.getType().equals(getType(String.class.getName())) && pathAccessors.getter != null) {
						// It turns out that calling setText() and setValue to the same value is a high-cost operation
					//	loadExpr = "if(!"+attributeAccessors.getter+".equals("+pathAccessors.conversionExpr(attributeAccessors.getType())+")) { "+loadExpr+" }";
					//}
					// Constant values stored to a non-async setter are set during initialization
					boolean constantLoad = (constant || pathAccessors.isConstant()) 
						&& pathAccessors.hasSynchronousGetter() 
						&& attributeAccessors.hasSynchronousSetter();
					boolean asyncLoad = pathAccessors.hasAsynchronousGetter() || attributeAccessors.hasAsynchronousSetter();
					if(constantLoad)
						sw.println(loadExpr);
					else if(earlyLoad) {
					    if(asyncLoad)
					        earlyAsyncLoads.add(loadExpr);
					    else
					        earlyLoads.add(loadExpr);
					} else if(asyncLoad)
					    asyncLoads.add(loadExpr);
					else
						loads.add(loadExpr);
					if (!(readOnly || constant)) {
					    if (!attributeAccessors.hasGetter()) {
					        logger.log(TreeLogger.ERROR, "Missing matching getter for attribute '"+attributeAccessors.getSetter()+"' on "+type+"; use ${} to set the value only.  Value is "+pathAccessors, null);
					        throw new UnableToCompleteException();
					    } else if(!pathAccessors.hasSetter()) {
					        logger.log(TreeLogger.ERROR, "Missing matching setter for '" + pathAccessors + "' for attribute '"+attributeAccessors.getSetter()+"' on "+type+"; use ${} to set the value only.", null);
					        throw new UnableToCompleteException();
					    }
					    
					    // If it's a two-way affair, copy the value back on save()
					    String saveExpr = pathAccessors.asyncCopyStatement(attributeAccessors, "group.<Void>member()", true);
					    saves.add(saveExpr);
					}
				} finally {
					LocalTreeLogger.popLogger();
				}
				
			}

            private void attachWidgetEventListener(String name, ActionInfo action, String listenerClass,
                            String listenerMethod, String condition, String actionStr) {
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
                sw.println(action.toString("AsyncCallbackFactory.<Void>defaultNewInstance()", true));
                if(condition != null)
                	sw.outdent();
                sw.outdent();
                sw.println("}");
                sw.outdent();
                sw.println("});");
            }

            private void generateContents(Element elem, GeneratorTypeInfo type, String name)
                            throws UnableToCompleteException {
            	// Assuming for now this is an existing GWT defined class; not the one we're generating and not a primitive type
        		final JClassType classType = ((JClassTypeWrapper)type).getClassType();
        		
                // Now, if there are nested tags, decide what to do with them
                // We do this before the attributes, because we want to call
                // setViewFactory() before setting the other
                // attributes. Many of the classes started their life with
                // ViewFactory as a parameter to the ctor and
                // don't behave well if they don't have a child view yet.
                Elements childElems = elem.getChildElements();
                if (elem.getChildCount() > 0) {
            		boolean foundSetter=false;
                	if(childElems.size() > 0) {
                    	// TODO Currently this is order-dependent for overloads :-(
						JMethod[] methods = classType.getOverridableMethods();
                		ArrayList<Element> missingSetter=new ArrayList<Element>();
                		ArrayList<ArrayList<JMethod>> missingSetterCandidates = new ArrayList<ArrayList<JMethod>>();
                    	for(int i=0; i < childElems.size(); i++) {
                    		Element childElem = childElems.get(i);
                    		String capChildElementName = capitalize(childElem.getLocalName());
							String setMethodName = "set"+capChildElementName;
                    		String addMethodName = "add"+capChildElementName;
                    		boolean elemProcessed = false;
                    		ArrayList<JMethod> foundSetters = new ArrayList<JMethod>(methods.length);
                    		for (int j = 0; j < methods.length; j++) {
    							JMethod method = methods[j];
    							//System.out.println("Looking for "+setMethod+" or "+addMethod+" for "+elem+" found "+method);
    							Name methodNameAnnotation = method.getAnnotation(Name.class);
    							if(methodNameAnnotation != null) {
    								if(!methodNameAnnotation.value().equalsIgnoreCase(childElem.getLocalName()))
    									continue;
    							} else if(!method.getName().equalsIgnoreCase(setMethodName) &&
    								!method.getName().equalsIgnoreCase(addMethodName)) {
    								continue;
    							}
    							foundSetters.add(method);
    							foundSetter = true;
    							final JParameter[] parameters = method.getParameters();
    							String[] paramStrings = new String[parameters.length];
    							boolean generatedSubview = false;
    							HashSet<String> usedAttributes = new HashSet<String>();
    							boolean failedParameterMatch = false;
    							for (int k = 0; k < parameters.length; k++) {
    								JParameter parameter = parameters[k];
    								String parameterName = getParameterName(parameter);
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
    										logger.log(TreeLogger.TRACE, "Couldn't match an attribute for parameter "+parameterName+" from "+method+" for tag "+childElem.toXML(), null);
    										failedParameterMatch = true;
    									}
    								} else {
    									usedAttributes.add(attribute);
    								}
    							}
    							if(failedParameterMatch) // At least one parameter did not match
    								continue;
    							if(usedAttributes.size() < childElem.getAttributeCount()) {
    								logger.log(TreeLogger.TRACE, "Not all attributes used from "+childElem.toXML()+" to call "+method+":", null);
    								for (int k = 0; k < childElem.getAttributeCount(); k++) {
    									if(!usedAttributes.contains(childElem.getAttribute(k).getLocalName())) {
    										logger.log(TreeLogger.TRACE, "   Attribute "+childElem.getAttribute(k).getLocalName()+" was not used.", null);
    									}
    								}
    								continue; // Not all attributes used, so this isn't the one they wanted
    							}
    							for (int k = 0; k < parameters.length; k++) {
    								JParameter parameter = parameters[k];
    								String parameterName = getParameterName(parameter);
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
                    			//System.out.println("NOT PROCESSED: " + elem);
                    			missingSetter.add(childElem);
                    			missingSetterCandidates.add(foundSetters);
                    		}
                    	}
                    	if(foundSetter && !missingSetter.isEmpty()) {
                    		int i=0;
                    		for(Element childElem : missingSetter) {
                    			logger.log(TreeLogger.WARN, "Ignored "+childElem.toXML()+"; couldn't find any matching setter.", null);
                    			for(JMethod candidate : missingSetterCandidates.get(i++)) {
                    				logger.log(TreeLogger.WARN, "Candidate setter method: "+candidate);
                    			}
                    		}
                    	}
                	}
                    // Support:
                    // setView() - set a view directly
                    // setViewFactory() - set a view factory (used by lists)
                	if(!foundSetter) {
                        boolean factory = type.hasMethodMatching("setViewFactory", true, null, commonTypes.viewFactory);
                        boolean widget = !factory && type.hasMethodMatching("setWidget", true, null, commonTypes.widget);
                        
                        String fieldName = "sv"+memberDecls.size();
                        String id = elem.getAttributeValue("id");
                        if(id == null) {
                            id = fieldName;
                        }
                    	String createViewExpr = generateCreateSubview(elem, factory, false, id);
                        final boolean modelView = elem.getAttribute("with-model") != null;
                        generateField(fieldName, factory?commonTypes.viewFactory:
                                                  //widget?commonTypes.widget:
                                                  modelView?commonTypes.modelView:
                                                  commonTypes.view);
                        sw.println(fieldName + " = " + createViewExpr + ";");
						if (factory) {
                            sw.println(name + ".setViewFactory("+fieldName+");");
						} else if(widget) {
							sw.println(name + ".setWidget("+fieldName+".getViewWidget());");
							subviewLoads.add(fieldName + ".load(group.<Void>member());");
                            saves.add(fieldName + ".save(group.<Void>member());");
                            clearFields.add(fieldName + ".clearFields();");
                        } else if (type.hasMethodMatching("setView", true, null, commonTypes.view)) {
                            sw.println(name + ".setView("+fieldName+");");                        	
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
             * @param id ID of the element, used for debug IDs
             * @return
             * @throws UnableToCompleteException
             */
			private String generateCreateSubview(Element elem, boolean factory, boolean widget, String id) throws UnableToCompleteException {
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
					String className = enqueueSubviewClass(elem);
					String thisViewExpr = myClass.getParameterizedQualifiedSourceName()+".this";
					String rootViewExpr = parentViewClass != null ? ROOT_VIEW_FIELD_NAME : thisViewExpr; // If we have no parent, we are the root view
					createViewExpr = getRootView(factory) + ".maybeEnsureDebugId(\"" + id + "\", new " + className + "(" + thisViewExpr + ", " + rootViewExpr + ")).init()";
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
				        paramString = generateCreateSubview(childElem, isViewFactory, isWidget, parameterName);
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
					if (((readOnly = (value.startsWith("${") || value.startsWith("@{") || value.startsWith("%{"))) || value.startsWith("#{")) && value.endsWith("}")) {
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
					ActionInfo action;
					if (parameter.getType().isClass() != null 
						&& parameter.getType().isClass().isAssignableTo(commonTypes.action.getJClassType())
				        && (pathAccessors == null || !pathAccessors.hasGetter()) 
				        && (action = getAction(value, true)) != null) {
						paramString = action.toViewAction();
					} else if (path != null
							    && commonTypes.value.getJClassType().getErasedType().equals(parameter.getType().isClassOrInterface().getErasedType())
						        && (valueExpr = getFieldValue(path)) != null) {
						paramString = valueExpr;
					} else if (pathAccessors != null) {
						if(!pathAccessors.hasSynchronousGetter()) {
							logger.log(TreeLogger.ERROR, "Async/write-only values when calling a setter with multiple parameters is not supported currently.", null);
							throw new UnableToCompleteException();
						}
						paramString = pathAccessors.conversionExpr(JTypeWrapper.wrap(parameter.getType()));
						if(paramString == null) {
							logger.log(TreeLogger.ERROR, "Cannot convert "+pathAccessors.getType()+" '"+path+"' to "+parameter.getType()+" for call to "+method, null);
							throw new UnableToCompleteException();
						}
					// If we get here, there's no "${...}" or "#{...}" so it's a constant value / string or an action
					} else if(parameter.getType().getQualifiedSourceName().equals("java.lang.String")) {
					    if(value.startsWith("${") || value.startsWith("%{") || value.startsWith("#{") || value.startsWith("@{"))
					        System.out.println("Warning: expression "+value+" treated as string...");
						paramString = '"'+escape(value)+'"';
					} else {
						logger.log(TreeLogger.ERROR, "Cannot convert '"+value+"' to "+parameter.getType()+" for call to "+method, null);
						throw new UnableToCompleteException();
					}
				}
				return paramString;
			}

			/**
			 * Add a subview to the list of subview that need to be generated after this class.
			 */
            protected String enqueueSubviewClass(Element childElem) {
            	String className = subviewClassName(childElem.getLocalName());
				subviewsToGenerate.add(new SubviewToGenerate(className, childElem, myClass));
                return className;
            }

			protected String subviewClassName(String localName) {
            	subviewNumber++;
				StringBuffer sb = new StringBuffer();
				char[] chars = localName.toCharArray();
            	boolean capNext = true;
            	for(char ch : chars) {
            		if(ch == '_' || ch == '-') {
            			capNext = true;
            			continue;
            		}
            		if(Character.isJavaIdentifierPart(ch)) {
                		if(capNext) { ch = Character.toUpperCase(ch); capNext = false; }
            			sb.append(ch);
            		}
            	}
            	sb.append(subviewNumber);
            	String className = sb.toString();
				return className;
			}

            /**
             * Return an expression which is a Value that accesses the given field.
             * 
             * A Value is an object that can be given to a class to dynamically load
             * or set a value without knowing much about it - a kind of wrapper for
             * a variable.
             */
            protected String getFieldValue(String path) throws UnableToCompleteException {
            	LocalTreeLogger.pushLogger(logger.branch(TreeLogger.INFO, "Trying to create a Value object for path '"+path+"'"));
            	try {
	                String existingValue = values.get(path);
	                if (existingValue != null) {
	                    return existingValue;
	                }
	                ExpressionInfo accessors = findAccessors(path, true, true);
	                if (accessors != null) {
	                	if(commonTypes.value.equals(accessors.getType())) {
	                		if(!accessors.hasSynchronousGetter()) {
	                			logger.log(TreeLogger.ERROR, "Can't handle an async Value getter yet; for "+path, null);
	                			throw new UnableToCompleteException();
	                    	}
	                		return accessors.getterExpr();
	                	}
	                    String valueName = "value" + values.size();
	                    values.put(path, valueName);
						generateField(valueName, commonTypes.value);
	                    sw.println(valueName + " = new Value() { ");
	                    if(accessors.hasSynchronousGetter()) {
	                        sw.indentln("public void getValue(AsyncCallback callback) { callback.onSuccess(" + accessors.getterExpr() + "); } ");
	                    } else if(accessors.hasAsynchronousGetter()) {
	                        sw.indentln("public void getValue(AsyncCallback callback) {\n\t\t\t\t"+accessors.callAsyncGetter("callback")+";\n\t\t\t\t} ");
	                    } else {
	                        sw.indentln("public void getValue(AsyncCallback callback) { callback.onFailure(null); } ");
	                    }
	                    if (accessors.hasSynchronousSetter()) {
	                        sw.indentln("public void setValue(Object value, AsyncCallback callback) { try {\n\t\t\t\t" +
	                        		accessors.callSetter(ExpressionInfo.converter("value", RuntimeClassWrapper.OBJECT, accessors.getType())) +
	                        				"\n\t\t\t\tcallback.onSuccess(null); } catch(Throwable caught) { callback.onFailure(caught); } }");
	                    } else if(accessors.hasAsynchronousSetter()) {
	                        sw.indentln("public void setValue(Object value, AsyncCallback callback) { " + 
	                        		accessors.callAsyncSetter(ExpressionInfo.converter("value", RuntimeClassWrapper.OBJECT, accessors.getType()), "callback") 
	                        		+"}");
	                        
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
                } finally {
                	LocalTreeLogger.popLogger();
                }
            }

            protected String getRootView(boolean innerClass) throws UnableToCompleteException {
            	if(parentViewClass == null) {
            		if(innerClass) return myClass.getSimpleSourceName()+".this";
            		return "this";
            	} else {
            		return ROOT_VIEW_FIELD_NAME;
            	}
            }
            
			ActionInfo getAction(String expr, boolean innerClass) throws UnableToCompleteException {
                final Matcher matcher = Pattern.compile("^on\\s+([^:]+):\\s*(.*)$").matcher(expr);
                final String targetView;
                if(matcher.matches()) {
                    final ExpressionInfo targetViewExpr = findAccessors(matcher.group(1), innerClass, false);
                    if(targetViewExpr == null || !targetViewExpr.hasSynchronousGetter()) {
                        logger.log(TreeLogger.ERROR, "Unable to resolve target view expression '"+matcher.group(1)+"' for action '"+expr+"'", null);
                        throw new UnableToCompleteException();
                    }
                    targetView = targetViewExpr.getGetter();
                    expr = matcher.group(2);
                } else {
                    targetView = getRootView(innerClass);
                }
			    
				return getAction(expr, targetView, true, true);
			}

			/**
			 * Multiple actions can be seperated using semicolons; the actions will run in sequence
			 * until they are all complete, or one fails in which case execution stops and the
			 * failure is returned to the default async callback (if any).
			 * 
			 * Actions which are an expression ${...} or #{...} or %{...} or @{..} are trated as expressions leading to
			 * an Action object somewhere, which is executed.
			 * 
			 * Actions starting with ';' won't save before running; actions ending with ';' won't load after
			 * running.  This can be used to avoid full save/load cycles when save/load is slow or unnecessary.
			 */
            protected ActionInfo getAction(String path, String targetView, boolean saveBefore, boolean loadAfter) throws UnableToCompleteException {
            	if((path.startsWith("${") || path.startsWith("#{") || path.startsWith("%{") || path.startsWith("@{")) && path.endsWith("}")) {
            		path = path.substring(2, path.length()-1);
                    ExpressionInfo expr = findAccessors(path, true, false);
                    if(expr != null && expr.hasSynchronousGetter())
                		return new ActionInfo(path, expr.getGetter(), true, true, targetView, saveBefore, loadAfter, 0);
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
            	String actionKey = path+","+saveBefore+","+targetView+","+loadAfter;
            	ActionInfo existing = actions.get(actionKey);
                if (existing != null)
                    return existing;
                String[] actionSeries = path.split("\\s*;\\s*");
                if(actionSeries.length > 1) {
                	ArrayList<String> actionList = new ArrayList<String>();
                	int timeout=0;
                	for (int i = 0; i < actionSeries.length; i++) {
                		ActionInfo action = getAction(actionSeries[i], targetView, false, false);
                		timeout += action.getTimeout();
                		if(action != null && action.getAction() != null)
                		    actionList.add(action.toActionCtor());
                	}
                	String ctor = "new ActionSeries("+StringUtils.join(actionList, ",\n\t\t\t")+")";
                    return new ActionInfo(path, ctor, true, true, targetView, saveBefore, loadAfter, timeout);
                }

                if("".equals(path) || "null".equals(path)) {
	                //sw.println("final Action " + actionName + " = new ViewAction(null, "+rootView+", "+saveBefore+", "+loadAfter+");");
                	return new ActionInfo(path, null, true, true, targetView, saveBefore, loadAfter, 0);
                }
                
                String[] args;
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

                int assignmentIndex = smartIndexOf(preargs, '=');
                if(assignmentIndex != -1) {
                	String left = path.substring(0, assignmentIndex).trim();
                	String right = path.substring(assignmentIndex+1).trim();
                	ExpressionInfo lvalue = findAccessors(left, true, true);
                	if(lvalue == null || (lvalue.hasSynchronousSetter() == false && lvalue.hasAsynchronousSetter() == false)) {
                		logger.log(TreeLogger.ERROR, "Can't find any setter for the left side of "+path, null);
                		throw new UnableToCompleteException();
                	}
                	ExpressionInfo rvalue = findAccessors(right, true, true);
                	if(rvalue == null || !rvalue.hasGetter()) {
                		logger.log(TreeLogger.ERROR, "Can't find any getter for the right side of "+path, null);
                		throw new UnableToCompleteException();
                	}
                	if(lvalue.hasAsynchronousSetter() == false && rvalue.hasAsynchronousGetter() == false) {
                        return new ActionInfo(path, lvalue.copyStatement(rvalue), false, false, targetView, saveBefore, loadAfter, 0);
                	} else {
                	    return new ActionInfo(path, lvalue.asyncCopyStatement(rvalue, "callback", false), false, true, targetView, saveBefore, loadAfter, 0);
                	}
                } else {
                    int objectPathEnd = preargs.lastIndexOf('.');
                    String objectPath;
                    String methodName;
                    String getter;
                    final GeneratorTypeInfo objectType;
                    boolean searchingThis = (objectPathEnd == -1);
                    if (searchingThis) {
                        objectPath = getter = "this";
                        objectType = myClass;
                        methodName = preargs;
                    } else {
                        objectPath = preargs.substring(0, objectPathEnd);
                        methodName = preargs.substring(objectPathEnd+1);
                        ExpressionInfo accessors = findAccessors(objectPath, true, false);
                        if (accessors == null || !accessors.hasSynchronousGetter() || accessors.getType() == null) {
                            logger.log(TreeLogger.ERROR, "Can't find any object for " + objectPath + " for action "+ path, null);
                            return null;
                        }
                        getter = accessors.getterExpr();
                        objectType = accessors.getType();
                        if (objectType.isPrimitive()) {
                            logger.log(TreeLogger.ERROR, "Can't call a method on a primitive "
                                            + accessors.getType() + " for expression " + path, null);
                            throw new UnableToCompleteException();
                        }
                    }
    
                    boolean asyncMethod = false;
                    GeneratorMethodInfo actionMethod = null;
                    GeneratorTypeInfo searchType = objectType;
                    if(searchType == null) {
                        logger.log(TreeLogger.ERROR, "Can't call a method on a " + objectType + " for expression " + path, null);
                        throw new UnableToCompleteException();
                    }
                    final String asyncCallbackClassName = AsyncCallback.class.getName();
                    for(;;) {
                    	// Look for a synchronous action method with the right number of parameters
                    	actionMethod = searchType.findMethodMatching(methodName, true, PrimitiveTypeInfo.VOID, new GeneratorTypeInfo[args.length]);
                    	if(actionMethod != null) {
                    		asyncMethod = false;
                    		break;
                    	}
                    	
                    	// Look for the method with one extra parameter which is the async callback
                    	GeneratorTypeInfo[] asyncParamTypes = new GeneratorTypeInfo[args.length+1];
                    	asyncParamTypes[args.length] = commonTypes.asyncCallback;
						actionMethod = searchType.findMethodMatching(methodName, true, PrimitiveTypeInfo.VOID, asyncParamTypes );
                    	if(actionMethod != null) {
                    		asyncMethod = true;
                    		break;
                    	}
                        if(searchingThis && searchType instanceof GeneratedInnerClassInfo) {
                        	searchType = searchType.getFieldType(PARENT_VIEW_FIELD_NAME, true);
                        	if(searchType == null)
                        		break;
                        	getter = getter+"."+PARENT_VIEW_FIELD_NAME;
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
                            logger.log(TreeLogger.ERROR, "Couldn't evaluate '" + arg + "' as argument to '" + path + "'", null);
                            throw new UnableToCompleteException();
                        }
                        args[i] = argAccessors.conversionExpr(actionMethod.getParameterTypes()[i]);
                    }
    
                    String methodCall;
                    if (getter.startsWith("this.")) {
                        methodCall = getter.substring(5) + "." + methodName;
                    } else if (getter.equals("this")) {
                        methodCall = methodName;
                    } else {
                        methodCall = getter + "." + methodName;
                    }
                    
                    final ActionMethod annotation = actionMethod.getAnnotation(ActionMethod.class);
                    int timeout=0;
                    if(annotation != null) {
                        saveBefore = annotation.saveBefore();
                        loadAfter = annotation.loadAfter();
                        timeout = annotation.timeout();
                    }
                    if (asyncMethod) {
                        return new ActionInfo(path, methodCall + "(" + joinWithCommas(0, args) + (args.length > 0 ? ", " : "")
                            + "callback);", false, true, targetView, saveBefore, loadAfter, timeout);
                    } else {
                        return new ActionInfo(path, methodCall + "(" + joinWithCommas(0, args) + ");", false, false, targetView, saveBefore, loadAfter, timeout);
                    }
                }
            }

            /**
             * Find getter and setter for the given expression (path) relative to the given base expression (base).
             * 
             * @param base Starting point, used as the prefix for the returned expression info
             * @param path Path relative to that starting point
             * @param matchAsync If true, allow an asynchronous getter and/or setter to be returned
             * @param staticAccess If true, use static methods instead if instance methods
             * @return A new ExpressionInfo representing whatever getter and setter could be found
             * @throws UnableToCompleteException If it fails to figure out the expression
             */
            protected ExpressionInfo findAccessors(ExpressionInfo base, final String path, final boolean matchAsync, boolean staticAccess) throws UnableToCompleteException {
            	LocalTreeLogger.pushLogger(logger.branch(TreeLogger.INFO, "findAccessors('"+base+"', path='"+path+"', matchAsync="+matchAsync+", staticAccess="+staticAccess+")"));
            	try {
	            	
	                GeneratorTypeInfo inType = base.getType();
	                if(inType.isPrimitive()) {
	                    logger.log(TreeLogger.ERROR, "Can't find any member inside of non-class type "+base.getType()+" with path "+path);
	                    throw new UnableToCompleteException();
	                }
	                String expr = base.getGetter();
	                
	                //System.out.println("findAccessors("+inType+", '"+expr+"', '"+path+"', "+matchAsync+")");
	                // Split "path" into two parts - the part before the first dot and the "rest" of the expression
	                String[] splitPath = smartSplit(path, '.', 2);
	                String name = splitPath[0];
	                if (name.length() == 0) {
	                    return null;
	                }
	                String getter;
	                boolean asyncGetter = false;
	                String setter = null;
	                boolean asyncSetter = false;
	                GeneratorTypeInfo type;
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
					boolean methodInvokation = endsWithParen // Has (), assume method call
							|| inType.hasMethodMatching(name, true, null) // no parens, but expression has the same name as a zero-arg method
							|| (matchAsync && inType.hasMethodMatching(name, true, null, commonTypes.asyncCallback)); // no parens, but expression has the same name as an async method and async is OK
					boolean lastOrOnlyPartOfTheExpression = splitPath.length == 1;
					if(methodInvokation) {
	                    String getterMethodName;
	                    ExpressionInfo[] args;
	                    GeneratorTypeInfo[] argTypes;
	                    if(endsWithParen) {
	                		int openIdx = smartIndexOf(name, '(');
	                		if(openIdx == -1) {
	                			logger.log(TreeLogger.ERROR, "Can't find opening ( for ) in "+name, null);
	                			throw new UnableToCompleteException();
	                		}
	                		
	                        String[] argExprs = smartSplit(name.substring(openIdx + 1, name.length() - 1), ',', 100);
	                        //System.out.println("Splitting '"+name.substring(openIdx + 1, name.length() - 1)+" around ',' gives "+args.length+" args: "+joinWithCommas(0, args));
	                        // Check for an empty parameter list
	                        if(argExprs.length == 1 && argExprs[0].length() == 0)
	                            argExprs = new String[0];
	                        getterMethodName = identifier(name.substring(0, openIdx));
	                        args = new ExpressionInfo[argExprs.length];
	                        argTypes = new GeneratorTypeInfo[argExprs.length];
	                        for(int i=0; i < argExprs.length; i++) {
	                        	String arg = argExprs[i].trim();
	                            ExpressionInfo argAccessors = findAccessors(arg, true, false);
	                            if (argAccessors == null) {
	                                logger.log(TreeLogger.ERROR, "Couldn't evaluate '" + arg + "' as argument to '" + name + "'", null);
	                                throw new UnableToCompleteException();
	                            }
	                            args[i] = argAccessors;
	                            argTypes[i] = argAccessors.getType();
	                        }
	                    } else {
	                        getterMethodName = identifier(name);
	                        args = new ExpressionInfo[0];
	                        argTypes = new GeneratorTypeInfo[0];
	                    }
	                    GeneratorTypeInfo objectType = inType;
	                    boolean searchingThis = objectType.equals(myClass);
	                    boolean asyncMethod = false;
	                    GeneratorMethodInfo getterMethod = null;
	                    for(;;) {
		                    GeneratorTypeInfo[] syncArgTypesWildcard = new GeneratorTypeInfo[argTypes.length];
		                    
		                    GeneratorTypeInfo[] asyncArgTypesWildcard = new GeneratorTypeInfo[argTypes.length+1];
		                    asyncArgTypesWildcard[argTypes.length] = commonTypes.asyncCallback;
		                    GeneratorTypeInfo[] asyncArgTypes = Arrays.copyOf(argTypes, argTypes.length+1);
		                    asyncArgTypes[argTypes.length] = commonTypes.asyncCallback;
		                    
		                    HashSet<String> candidates = new HashSet<String>();
		                    candidates.add(getterMethodName);
		                    String capGetterMethodName = capitalize(getterMethodName);
							candidates.add("get"+capGetterMethodName);
		                    candidates.add("is"+capGetterMethodName);
		                    for(String candidate : candidates) {
		                    	getterMethod = objectType.findMethodMatching(candidate, true, null, argTypes);
		                    	if(getterMethod == null)
		                    		getterMethod = objectType.findMethodMatching(candidate, true, null, syncArgTypesWildcard);
		                    	if(getterMethod != null) {
		                    		asyncMethod = false;
		                    		type = getterMethod.getReturnType();
		                    		break;
		                    	}
		                    	
		                    	if(matchAsync) {
		                    		getterMethod = objectType.findMethodMatching(candidate, true, null, asyncArgTypes);
		                    		if(getterMethod == null)
		                    			getterMethod = objectType.findMethodMatching(candidate, true, null, asyncArgTypesWildcard);
			                    	if(getterMethod != null) {
			                    		asyncMethod = true;
			                    		type = getterMethod.getAsyncReturnType();
			                    		if(type == null) type = commonTypes.object;
			                    		break;
			                    	}
		                    	}
		                    }
		                    if(searchingThis && getterMethod == null && objectType instanceof GeneratedInnerClassInfo) {
	                        	objectType = objectType.getFieldType(PARENT_VIEW_FIELD_NAME, true);
	                        	if(objectType == null)
	                        		break;
	                        	expr = expr+"."+PARENT_VIEW_FIELD_NAME;
	                        	//System.out.println("Ascending to "+expr+" "+objectType);
	                        } else break;
	                    }
	                    if (getterMethod == null) {
	                        logger.log(TreeLogger.ERROR, "findAccessors(): Unable to find a "+(staticAccess?"static":"instance")+" method with the right number of arguments ("
	                                        + args.length + (matchAsync?" [ + optional AsyncCallback]":"")+") with name '" + getterMethodName + "' in " + inType
	                                        + " for expression '" + path + "'", null);
	                        throw new UnableToCompleteException();
	                    }
	    
	                    StringBuffer getterBuf = new StringBuffer();
	                    getterBuf.append(expr).append('.').append(getterMethod.getName()).append('(');
	                    
	                    for (int i = 0; i < args.length; i++) {
	                    	if(i > 0) getterBuf.append(", ");
	                    	getterBuf.append(args[i].conversionExpr(getterMethod.getParameterTypes()[i]));
	                    }
	                    
	                    if(asyncMethod) {
	                    	if(args.length > 0) getterBuf.append(","); // trailing comma for async methods so we can append the callback parameter when we call the method
	                    } else getterBuf.append(')');
	    
	                    getter = getterBuf.toString();
	                    
	                    asyncGetter = asyncMethod;
	                    type = asyncMethod?getterMethod.getAsyncReturnType():getterMethod.getReturnType();
	                    
	                    // Find the matching setter method (if any).
	                    String setterMethodName = getterMethod.getName().replaceFirst("^(is|get)", "set");
	                    GeneratorTypeInfo[] setterArgTypes = Arrays.copyOf(argTypes, argTypes.length+1);
	                    setterArgTypes[argTypes.length] = type;
	                    GeneratorMethodInfo setterMethod = objectType.findMethodMatching(setterMethodName, true, null, setterArgTypes);
	                    
	                    // If searching for something matching the types we got doesn't work, try it a wildcard for the type
	                    if(setterMethod == null) {
	                    	setterArgTypes = new GeneratorTypeInfo[argTypes.length+1];
	                    	setterArgTypes[argTypes.length] = type;
	                    	setterMethod = objectType.findMethodMatching(setterMethodName, true, null, setterArgTypes);
	                    }
	                    
	                    if(setterMethod == null) {
	                    	setterArgTypes = Arrays.copyOf(argTypes, argTypes.length+2);
	                    	setterArgTypes[argTypes.length] = type;
	                    	setterArgTypes[argTypes.length+1] = commonTypes.asyncCallback;
	                        setterMethod = objectType.findMethodMatching(setterMethodName, true, PrimitiveTypeInfo.VOID, setterArgTypes);
	                        if(setterMethod == null) {
	                        	setterArgTypes = new GeneratorTypeInfo[argTypes.length+2];
	                        	setterArgTypes[argTypes.length] = type;
	                        	setterArgTypes[argTypes.length+1] = commonTypes.asyncCallback;
	                            setterMethod = objectType.findMethodMatching(setterMethodName, true, PrimitiveTypeInfo.VOID, setterArgTypes);
	                        }
	                        if(setterMethod != null)
	                        	asyncSetter = true;
	                    } else {
	                    	asyncSetter = false;
	                    }
						if(setterMethod != null) {
		                    StringBuffer setterBuf = new StringBuffer();
		                    setterBuf.append(expr).append('.').append(setterMethod.getName()).append('(');
		                    
		                    for (int i = 0; i < args.length; i++) {
		                    	if(i > 0) setterBuf.append(", ");
		                    	String convertedArg = args[i].conversionExpr(setterMethod.getParameterTypes()[i]);
		                    	if(convertedArg.isEmpty()) {
		                    		throw new IllegalStateException("Got empty result back from "+args[i]+" converted to "+setterMethod.getParameterTypes()[i]);
		                    	}
								setterBuf.append(convertedArg);
		                    }
		                    
		                    if(args.length > 0)
		                    	setterBuf.append(","); // trailing comma for setters so we can append the value parameter and possibly the async callback 
		    
		                    setter = setterBuf.toString();
						}
	            	} else {
	            		// No array or function specifier, so look for a normal property or field
	                    String baseExpr = (expr.equals("this") ? "" : expr + ".");
	                    name = identifier(name);
	                    String getterName = "get" + capitalize(name);
	                    String setterName = "set" + capitalize(name);
	                    GeneratorMethodInfo getterMethod = inType.findMethodMatching(getterName, true, null);
	                    if(getterMethod == null && matchAsync) { // Check for async version, if allowed in this context
	                    	getterMethod = inType.findMethodMatching(getterName, true, PrimitiveTypeInfo.VOID, commonTypes.asyncCallback);
	                    	asyncGetter = getterMethod != null;
	                    }
	                    if (getterMethod == null) {
	                        getterName = "is" + capitalize(name);
	                        getterMethod = inType.findMethodMatching(getterName, true, null);
	                        if(getterMethod == null && matchAsync) { // Check for async version, if allowed in this context
	                        	getterMethod = inType.findMethodMatching(getterName, true, PrimitiveTypeInfo.VOID, commonTypes.asyncCallback);
	                        	asyncGetter = getterMethod != null;
	                        }
	                    }
	                    if (getterMethod != null) {
	                        getter = baseExpr + getterName + (asyncGetter?"":"()"); // No trailing brackets for an async call
	                        if(asyncGetter) {
	                        	type = getterMethod.getAsyncReturnType();
	                        	if(type == null) type = commonTypes.object;
	                        } else {
	                            type = getterMethod.getReturnType();
	                        }
	                    } else {
	                    	asyncGetter = false;
	                    	
	                    	// Try direct field access
	                    	type = inType.getFieldType(name, baseExpr.startsWith("this."));
	                    	if(type != null) {
	                    		getter = baseExpr + name;
	                    		setter = baseExpr + name + "=";
	                    		asyncSetter = false;
	                    		asyncGetter = false;
	                    	} else {
	                        	getter = null;
	                    	}
	                    }
	                    if(setter == null) {
		                    GeneratorMethodInfo setterMethod;
		                    // Only look for the setter if this is the last (or only) part of the chain.  i.e. for an expression
		                    // a.b.c we would only look for a setter for c, not a or b.
		                    if(lastOrOnlyPartOfTheExpression) {
		                    	setterMethod = inType.findMethodMatching(setterName, true, null, (GeneratorTypeInfo)null);
		                    	if(setterMethod == null) {
		                        	setterMethod = inType.findMethodMatching(setterName, true, PrimitiveTypeInfo.VOID, (GeneratorTypeInfo)null, commonTypes.asyncCallback);
		                        	asyncSetter = setterMethod != null;
		                    	}
		                    	if(setterMethod != null) {
			                    	//System.out.println("Found setter "+setterMethod);
			                        setter = baseExpr + setterName + "(";
			                    	type = setterMethod.getParameterTypes()[0];
		                    	}
		                    }
	                    }
	            	}
	                
	                //System.out.println("Looking for "+name+" in "+inType+", found "+getter+" and "+setter);
	                if(getter != null && splitPath.length == 2) {
	                	if(type.isArray()) {
	                		if("length".equals(splitPath[1])) {
	                			return new ExpressionInfo(path, getter+"."+splitPath[1], PrimitiveTypeInfo.INT, false);
	                		} else {
	                        	logger.log(TreeLogger.ERROR, "Attempting to access a property of array that I don't recognize: "+getter+"."+splitPath[1], null);
	                            throw new UnableToCompleteException();
	                		}
	                	} else if(type.isPrimitive()) {
	                    	logger.log(TreeLogger.ERROR, "Attempting to access a property of a primitive type: "+getter+" of type "+type+" async = "+asyncGetter, null);
	                        throw new UnableToCompleteException();
	                	}
	
	                    if(asyncGetter == false) {
	                    	// Easy... just get them to create a new getter based on this one
	                    	return findAccessors(new ExpressionInfo(path, getter, type, isConstants(type)), splitPath[1], matchAsync, staticAccess);
	                    } else {
	                    	// Oops, we're getting a property of an async property, time for some magic
	                    	// The trick: generate a new method that does the first async operation and
	                    	// then returns the result of the getter of the proceeding attributes.
	                    	ExpressionInfo subexpr = findAccessors(new ExpressionInfo(path, "base", type, false), splitPath[1], matchAsync, staticAccess);
	                    	if(subexpr == null) {
	                    		logger.log(TreeLogger.ERROR, "Failed to find property '"+splitPath[1]+"' of type '"+type+"' of expression '"+getter+"'", null);
	                            throw new UnableToCompleteException();
	                    	}
	                    	String getterName = "getAsync"+asyncProxies.size();
	                    	if(subexpr.hasGetter()) {
	                        	if(subexpr.hasSynchronousGetter()) {
	                        		// Synchronous sub-expression, how merciful! 
	                            	asyncProxies.add("    public void "+getterName+"(AsyncCallback<Object> callback) {\n"+
	                            		             "        "+ExpressionInfo.callAsyncGetter(getter, "new AsyncCallbackDirectProxy<Object>(callback, \""+path+"\") {\n"+
	                            		             "            public void onSuccess(Object result) {\n"+
	                            		             "                "+type.getParameterizedQualifiedSourceName()+" base = ("+type.getParameterizedQualifiedSourceName()+") result;\n"+
	                            		             "                returnSuccess("+subexpr.getterExpr()+");\n"+
	                            		             "            }\n"+
	                            		             "        }")+";\n"+
	                            		             "    }\n");
	                        	} else if(subexpr.hasAsyncGetter()) {
	                            	asyncProxies.add("    public void "+getterName+"(AsyncCallback<Object> callback) {\n"+
	               		             "        "+ExpressionInfo.callAsyncGetter(getter, "new AsyncCallbackDirectProxy<Object>(callback, \""+path+"\") {\n"+
	               		             "            public void onSuccess(Object result) {"+
	               		             "                "+type.getParameterizedQualifiedSourceName()+" base = ("+type.getParameterizedQualifiedSourceName()+") result;\n"+
	               		             "                "+subexpr.callAsyncGetter("callback")+";\n"+
	               		             "            }\n"+
	               		             "        }")+";\n"+
	               		             "    }\n");
	                        	}
	                    	} else getterName = null;
	                    	String setterName = "setAync"+asyncProxies.size();
	                    	if(subexpr.hasSetter()) {
	                        	if(subexpr.hasSynchronousSetter()) {
	                        		// Synchronous sub-expression, how merciful! 
	                            	asyncProxies.add("    public void "+setterName+"(final "+subexpr.getType().getParameterizedQualifiedSourceName()+" value, AsyncCallback<Void> callback) {\n"+
	                  		             "        "+ExpressionInfo.callAsyncGetter(getter, "new AsyncCallbackDirectProxy<Void>(callback, \""+path+"\") {\n"+
	                            		             "            public void onSuccess(Void result) {\n"+
	                            		             "                "+type.getParameterizedQualifiedSourceName()+" base = ("+type.getParameterizedQualifiedSourceName()+") result;\n"+
	                            		             "                "+subexpr.callSetter("value")+"\n"+
	                            		             "                returnSuccess(null);\n"+
	                            		             "            }\n"+
	                   		             "        }")+";\n"+
	                            		             "    }");
	                        	} else if(subexpr.hasAsynchronousSetter()) {
	                            	asyncProxies.add("    public void "+setterName+"(final "+subexpr.getType().getParameterizedQualifiedSourceName()+" value, AsyncCallback<Void> callback) {\n"+
	                 		             "        "+ExpressionInfo.callAsyncGetter(getter, "new AsyncCallbackDirectProxy<Void>(callback, \""+path+"\") {\n"+
	               		             "            public void onSuccess(Void result) {\n"+
	               		             "                "+type.getParameterizedQualifiedSourceName()+" base = ("+type.getParameterizedQualifiedSourceName()+") result;\n"+
	               		             "                "+subexpr.callAsyncSetter("value", "callback")+"\n"+
	               		             "            }\n"+
	              		             "        }")+";\n"+
	               		             "    }");
	                        	}
	                    	} else setterName = null;
	                    	return new ExpressionInfo(path, getterName, setterName, subexpr.getType(), true, true, false);
	                    }
	                } else if(setter != null && lastOrOnlyPartOfTheExpression) {
	                	return new ExpressionInfo(path, getter, setter, type, asyncGetter, asyncSetter, false);
	                }
	                
	                /*
	                JClassType superclass = inType.getSuperclass();
	                if (superclass != null && !ReflectedClassInfo.OBJECT.equals(superclass)) {
	                	ExpressionInfo inherited = findAccessors(superclass, expr, path, matchAsync);
	                	if(inherited != null) {
	                		if(getter == null) { 
	                			if(inherited.getter != null) { getter = inherited.getter; asyncGetter = false; }
	                			else if(inherited.hasAsynchronousGetter()) { getter = inherited.asyncGetter; asyncGetter = true; }
	                		}
	                		if(setter == null) { 
	                			if(inherited.hasSynchronousSetter()) { setter = inherited.setter; asyncSetter = false; }
	                			else if(inherited.hasAsynchronousSetter()) { setter = inherited.asyncSetter; asyncSetter = true; }
	                		}
	                		if(type == null) type = inherited.getType();
	                	}
	                }
	                */
	                if(type != null) {
	                    if((getter != null) && (setter == null) && !asyncGetter 
	                        && (isConstants(inType))) {
	                        //logger.log(TreeLogger.INFO, "Considering value to be constant since it is part of a Constants or DictionaryConstants: "+getter);
	                        return new ExpressionInfo(path, getter, type, base.isConstant());
	                    } else {
	                        return new ExpressionInfo(path, getter, setter, type, asyncGetter, asyncSetter, false);
	                    }
	                }
	                //System.out.println("Failed to find property "+name+" on "+inType);
	                return null;
            	} finally {
            		LocalTreeLogger.popLogger();
            	}
            }

            
            /**
             * A wrapper for string.split() that doesn't split inside matching ()'s or []'s.
             * 
             * maxlen refers to the maximum length of the resulting array.
             */
            private String[] smartSplit(String s, char seperator, int maxlen) {
            	ArrayList<String> result = new ArrayList<String>();
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
            	return result.toArray(new String[result.size()]);
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
            					return "!(" + ExpressionInfo.converter(expr, accessors.getType(), PrimitiveTypeInfo.BOOLEAN) + ')';
            				}
            				@Override
							public String onSetExpr(String expr) throws UnableToCompleteException {
            					return ExpressionInfo.converter("!(" + expr + ')', PrimitiveTypeInfo.BOOLEAN, accessors.getType());
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
            			return new ExpressionInfo(path, path, JPrimitiveType.DOUBLE, true);
            		} else if(path.endsWith("L")) {
            			return new ExpressionInfo(path, path, JPrimitiveType.LONG, true);
            		} else {
            			return new ExpressionInfo(path, path, JPrimitiveType.INT, true);
            		}
            	} else if(path.equals("true") || path.equals("false")) {
            		return new ExpressionInfo(path, path, JPrimitiveType.BOOLEAN, true);
            	} else if(path.equals("null")) {
            		return new ExpressionInfo(path, path, RuntimeClassWrapper.OBJECT, true);
            		
            	} else if(path.startsWith("\"") && path.endsWith("\"")) {
            		return new ExpressionInfo(path, path, getType("java.lang.String"), true);
            		
            	}
            	String thisExpr = innerType ? myClass.getSimpleSourceName() + ".this" : "this";
                GeneratorTypeInfo classToSearch = myClass;
                if (path.equals("this")) {
                	while(classToSearch instanceof GeneratedInnerClassInfo) {
                		classToSearch = ((GeneratedInnerClassInfo)classToSearch).getFieldType(PARENT_VIEW_FIELD_NAME, true);
                		if(classToSearch == null)
                			break;
                		thisExpr = thisExpr+"."+PARENT_VIEW_FIELD_NAME;
                	}
                	
                	// When they use the expression "this", be sure to use the superclass of the generated class;
                	// the generated class won't behave well with isAssignableFrom() and isAssignableTo() because
                	// it's a fake object we created and isn't "known" by the type oracle.
                    return new ExpressionInfo(path, thisExpr, null, classToSearch, false, false, false);
                }
                
                // like books.service.AccountType.ACCOUNTS_RECEIVABLE or abc.def.Foo.bar
                Matcher staticReference = Pattern.compile("([a-z0-9_]+(?:\\.[a-z0-9_]+)+(?:\\.[A-Z][A-Za-z0-9_]+)+)\\.([A-Za-z0-9_]+.*)").matcher(path);
                if(staticReference.matches()) {
                	String className = staticReference.group(1);
                	String property = staticReference.group(2);
                	//System.out.println("Static reference: "+className+" property "+property);
					try {
						JClassType staticType = types.getType(className);
	                	JField field = staticType.getField(property);
	            	    JEnumType enum1 = staticType.isEnum();
	                	if(field != null && field.isStatic()) {
							return new ExpressionInfo(path, path, field.getType(), field.isFinal() || enum1!=null);
	                	}
	                	if(enum1 != null && property.equals("values()")) {
	                		return new ExpressionInfo(path, path, types.getArrayType(enum1), true);
	                	}
	                	return findAccessors(new ExpressionInfo(path, className, staticType, true), property, matchAsync, true);
					} catch (NotFoundException e) {
					}  
                }
                
                for (;;) {
                    ExpressionInfo accessors = findAccessors(new ExpressionInfo(path, thisExpr, classToSearch, true), path, matchAsync, false);
                    if (accessors != null) {
                        return accessors;
                    } else if(classToSearch instanceof GeneratedInnerClassInfo){
                        classToSearch = classToSearch.getFieldType(PARENT_VIEW_FIELD_NAME, true);
                        if (classToSearch == null) {
                            return null;
                        }
                        if(parentViewClass != null) {
                            thisExpr = thisExpr+"."+PARENT_VIEW_FIELD_NAME;
                        } else {
                            thisExpr = classToSearch.getSimpleSourceName() + ".this";
                        }
                    } else {
                    	return null;
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
				GeneratorTypeInfo targetOperandType;
				GeneratorTypeInfo resultType;
				GeneratorTypeInfo java_lang_String = RuntimeClassWrapper.STRING;
				if ("&&".equals(oper) || "||".equals(oper)){
					targetOperandType = PrimitiveTypeInfo.BOOLEAN;
					resultType = PrimitiveTypeInfo.BOOLEAN;
				} else if("+".equals(oper) && (right.getType().getParameterizedQualifiedSourceName().equals("java.lang.String") || left.getType().getParameterizedQualifiedSourceName().equals("java.lang.String"))) {
					targetOperandType = java_lang_String;
					resultType = java_lang_String;
				} else {
					targetOperandType = left.getType();
					if(arithmeticOper)
						resultType = left.getType();
					else // all other operations are boolean
						resultType = PrimitiveTypeInfo.BOOLEAN;
				}
				
				// Is it an asynchronous calculation?
				if(left.hasAsynchronousGetter() || right.hasAsynchronousGetter()) {
					return handleAsyncBinaryOperator(left, oper, right, targetOperandType, resultType);
				}
				
				String convertRight = right.conversionExpr(targetOperandType);
				if(convertRight == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+right.getType()+" "+right+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				String convertLeft = left.conversionExpr(targetOperandType);
				if(convertLeft == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+left+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				return new ExpressionInfo(path, "(" + convertLeft + oper + convertRight + ")", resultType, left.isConstant() && right.isConstant());
			}

			private ExpressionInfo handleAsyncBinaryOperator(ExpressionInfo left, String oper, ExpressionInfo right,
				GeneratorTypeInfo targetOperandType, GeneratorTypeInfo resultType) throws UnableToCompleteException {
				String getterName = "getCalculation"+calculations.size();
				String leftTypeName = left.getType().getParameterizedQualifiedSourceName();
				String classLeftTypeName = getBoxedClassName(left.getType());
				String rightTypeName = right.getType().getParameterizedQualifiedSourceName();
				String classRightTypeName = getBoxedClassName(right.getType());
				final String resultClassTypeName = getBoxedClassName(resultType);
				String convertRight = ExpressionInfo.converter("right", right.getType(), targetOperandType);
				if(convertRight == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+right.getType()+" "+right+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				String convertLeft = ExpressionInfo.converter("left", left.getType(), targetOperandType);
				if(convertLeft == null) {
					logger.log(TreeLogger.ERROR, "Can't convert "+left.getType()+" "+left+" to "+targetOperandType, null);
					throw new UnableToCompleteException();
				}
				if(left.hasAsynchronousGetter()) {
					if(right.hasAsynchronousGetter()) {
						calculations.add("    public void "+getterName+"(AsyncCallback<"+resultClassTypeName+"> callback) {\n"+
				             "        "+left.callAsyncGetter("new AsyncCallbackProxy<"+classLeftTypeName+","+resultClassTypeName+">(callback) {\n"+
				             "            public void onSuccess(final "+classLeftTypeName+" left) {\n"+
				             "                "+right.callAsyncGetter("new AsyncCallbackProxy<"+classRightTypeName+","+resultClassTypeName+">(callback) {\n"+
					         "                public void onSuccess(final "+classRightTypeName+" right) {\n"+
					         "                    returnSuccess("+convertLeft+oper+convertRight+");\n"+
					         "                }\n"+
					         "            }")+";\n"+
				             "            }\n"+
				             "        }")+";\n"+
				             "    }\n");
					} else {
						calculations.add("    public void "+getterName+"(AsyncCallback<"+resultClassTypeName+"> callback) {\n"+
				             "        "+left.callAsyncGetter("new AsyncCallbackProxy<"+classLeftTypeName+","+resultClassTypeName+">(callback) {\n"+
				             "            public void onSuccess(final "+classLeftTypeName+" left) {\n"+
					         "                final "+rightTypeName+" right = "+right.getterExpr()+";\n"+
					         "                returnSuccess("+convertLeft+oper+convertRight+");\n"+
				             "            }\n"+
				             "        }")+";\n"+
				             "    }\n");
					}
				} else {
					// right.asyncGetter must != null
					calculations.add("    public void "+getterName+"(AsyncCallback<"+resultClassTypeName+"> callback) {\n"+
				         "            final "+leftTypeName+" left = "+left.getterExpr()+";\n"+
				         "            "+right.callAsyncGetter("new AsyncCallbackProxy<"+classRightTypeName+","+resultClassTypeName+">(callback) {\n"+
				         "            public void onSuccess(final "+classRightTypeName+" right) {\n"+
				         "                returnSuccess("+convertLeft+oper+convertRight+");\n"+
				         "            }\n"+
				         "        }")+";\n"+
				         "    }\n");
					
				}
				return new ExpressionInfo(left+oper+right, getterName, null, resultType, true, false, false);
			}

			private String getBoxedClassName(final JType type) {
				return type.isPrimitive()!=null?type.isPrimitive().getQualifiedBoxedSourceName():type.getQualifiedSourceName();
			}
			private String getBoxedClassName(final GeneratorTypeInfo type) {
				if(type instanceof JTypeWrapper)
					return getBoxedClassName(((JTypeWrapper)type).getJType());
				if(type instanceof PrimitiveTypeInfo)
					return ((PrimitiveTypeInfo)type).getBoxedTypeName();
				return type.getParameterizedQualifiedSourceName();
			}

        }

        public String identifier(String id) {
            if(id == null) return null;
            id = id.trim().replaceAll("[^A-Za-z0-9_]+", "_");
            if(Character.isUpperCase(id.charAt(0)))
                id = Character.toLowerCase(id.charAt(0))+id.substring(1);
            return id;
        }

        public String replaceGroupMember(String expr, String newCallback) {
            return expr.replaceFirst("group.(?:<[^>]*>)?member\\((?:\"[^\"]*\")?[^)]*\\)", newCallback);
        }

        public Object backslashEscape(String substring) {
			return substring.replaceAll("([\'\"\\\\])", "\\\\$1").replace("\n", "\\n").replace("\r", "\\r");
		}

		protected boolean isConstants(GeneratorTypeInfo inType) throws UnableToCompleteException {
			return inType.implementsInterface(commonTypes.constants)
			    || inType.implementsInterface(commonTypes.dictionaryConstants);
		}

    }
	@Override
	protected GeneratorInstance createGeneratorInstance() {
        return new GeneratorInstance();
    }
}
