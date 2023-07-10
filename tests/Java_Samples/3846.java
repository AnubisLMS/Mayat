package org.t2framework.lucy.config.stax;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import org.t2framework.commons.Constants;
import org.t2framework.commons.el.Expression;
import org.t2framework.commons.meta.ConstructorDesc;
import org.t2framework.commons.meta.MethodDesc;
import org.t2framework.commons.meta.PropertyDesc;
import org.t2framework.commons.meta.impl.MethodDescImpl;
import org.t2framework.commons.meta.impl.PropertyDescImpl;
import org.t2framework.commons.util.CollectionsUtil;
import org.t2framework.commons.util.JavaBeansUtil;
import org.t2framework.commons.util.Logger;
import org.t2framework.commons.util.StringUtil;
import org.t2framework.commons.util.Reflections.ClassUtil;
import org.t2framework.commons.util.Reflections.MethodUtil;
import org.t2framework.lucy.Lucy;
import org.t2framework.lucy.config.meta.ConfigurableBeanDesc;
import org.t2framework.lucy.config.meta.InjectConfig;
import org.t2framework.lucy.config.meta.impl.ConstructorInjectConfig;
import org.t2framework.lucy.config.meta.impl.MethodInjectConfig;
import org.t2framework.lucy.config.meta.impl.PropertyInjectConfig;
import org.t2framework.lucy.exception.MethodNotFoundException;

/**
 * 
 * <#if locale="en">
 * <p>
 * Tag handler for <inject>.
 * </p>
 * <#else>
 * <p>
 * 
 * </p>
 * </#if>
 * 
 * @author shot
 * 
 */
public class InjectTagHandler extends AbstractExpressionHandler implements XmlEventHandler {

    protected static Logger logger = Logger.getLogger(InjectTagHandler.class);

    @SuppressWarnings("unchecked")
    @Override
    public void start(XmlEventContext context, Attributes attributes) {
        ConfigurableBeanDesc beanDesc = (ConfigurableBeanDesc) context.peek();
        final String paramAttributes = attributes.getAndRemoveValue("parameterTypes");
        final Class<?>[] paramTypes = findParamTypes(paramAttributes);
        final String methodName = attributes.getAndRemoveValue("method");
        if (attributes.hasAnyAttribute()) {
            logForUnknownValue(context, attributes);
        }
        if (StringUtil.isEmpty(methodName) == false) {
            setupMethod(context, beanDesc, methodName, paramTypes);
        } else {
            setupConstructor(beanDesc);
        }
    }

    @SuppressWarnings("unchecked")
    protected void setupMethod(XmlEventContext context, ConfigurableBeanDesc beanDesc, String methodName, Class<?>[] paramTypes) {
        Class<?> c = beanDesc.getComponentClass();
        Method method = find(context, c, methodName, paramTypes);
        if (JavaBeansUtil.isSetMethod(methodName)) {
            beanDesc.addPropertyDesc(createPropertyDesc(c, method, paramTypes));
        } else {
            beanDesc.addMethodDesc(createMethodDesc(method, paramTypes));
        }
    }

    @SuppressWarnings("unchecked")
    protected void setupConstructor(ConfigurableBeanDesc beanDesc) {
        final Class<?> c = beanDesc.getComponentClass();
        Constructor<?>[] constructors = c.getConstructors();
        if (constructors.length == 1 && constructors[0].getParameterTypes().length == 0) {
            throw new IllegalStateException("method attribute must not be blank");
        }
        ConstructorDesc cd = beanDesc.getConstructorDesc();
        if (cd.hasConfig(InjectConfig.class) == false) {
            InjectConfig injectConfig = new ConstructorInjectConfig();
            cd.addConfig(injectConfig);
            beanDesc.setConstructorDesc(cd);
        } else {
            ConstructorInjectConfig config = (ConstructorInjectConfig) cd.findConfig(InjectConfig.class);
            config.incrementArgumentSize();
        }
    }

    protected Class<?>[] findParamTypes(String paramTypesStr) {
        if (StringUtil.isEmpty(paramTypesStr)) {
            return Constants.EMPTY_CLASS_ARRAY;
        }
        String[] params = StringUtil.split(paramTypesStr, ",");
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = ClassUtil.forName(params[i]);
        }
        return paramTypes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void end(XmlEventContext context, String body) {
        ConfigurableBeanDesc beanDesc = (ConfigurableBeanDesc) context.peek();
        final String source = body.trim();
        Expression<Lucy> expression = getExpression(source);
        if (expression != null) {
            beanDesc.getLatest().add(expression);
        }
    }

    public MethodDesc createMethodDesc(final Method method, Class<?>[] paramTypes) {
        MethodDesc md = new MethodDescImpl(method);
        InjectConfig injectConfig = new MethodInjectConfig(paramTypes);
        md.addConfig(injectConfig);
        return md;
    }

    @SuppressWarnings("unchecked")
    public PropertyDesc<?> createPropertyDesc(final Class<?> componentClass, final Method writeMethod, Class<?>[] paramTypes) {
        String propertyName = JavaBeansUtil.getPropertyName(writeMethod.getName());
        final Method readMethod = getReadMethod(componentClass, writeMethod);
        Class<?> propertyType = null;
        if (readMethod != null) {
            propertyType = readMethod.getReturnType();
        } else {
            propertyType = JavaBeansUtil.findPropertyTypeFromWriteMethod(writeMethod);
        }
        PropertyDesc pd = new PropertyDescImpl(componentClass, propertyType, propertyName);
        pd.setWriteMethod(writeMethod);
        if (readMethod != null) {
            pd.setReadMethod(readMethod);
        }
        InjectConfig injectDesc = new PropertyInjectConfig(writeMethod.getParameterTypes());
        pd.addConfig(injectDesc);
        return pd;
    }

    protected <T> Method getReadMethod(Class<T> componentClass, Method writeMethod) {
        return JavaBeansUtil.getReadMethodFromWriteMethod(componentClass, writeMethod);
    }

    protected Method find(XmlEventContext context, Class<?> componentClass, String methodName, Class<?>[] paramTypes) {
        List<Method> list = CollectionsUtil.newArrayList();
        for (Method m : componentClass.getMethods()) {
            if (methodName.equals(m.getName())) {
                list.add(m);
            }
        }
        if (list == null || list.isEmpty()) {
            throw new MethodNotFoundException(componentClass, methodName, context.getLineNumber(), context.getColumnNumber());
        }
        if (list.size() == 1) {
            return list.get(0);
        } else {
            if (paramTypes == null || paramTypes.length == 0) {
                return list.get(0);
            } else {
                return MethodUtil.getDeclaredMethod(componentClass, methodName, paramTypes);
            }
        }
    }

    protected void logForUnknownValue(XmlEventContext context, Attributes attributes) {
        logger.log("DLucyCore0003", new Object[] { "inject", attributes.getAllValueAsString(), context.getLineNumber(), context.getColumnNumber() });
    }
}
