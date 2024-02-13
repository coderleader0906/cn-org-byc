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

package cn.org.byc.base.support.beans;

import cn.org.byc.base.config.Config;
import cn.org.byc.base.util.BeanUtils;
import cn.org.byc.base.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.expression.*;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpringExpressionBean类提供了一个灵活的机制来解析和执行Spring表达式语言（SpEL）表达式。
 * 该类实现了BeanFactoryAware和EnvironmentAware接口，允许它访问Spring BeanFactory和Environment，
 * 以便在表达式计算中使用Spring管理的bean和环境属性。
 *
 * @author ken
 */
@Slf4j
public class SpringExpressionBean  implements BeanFactoryAware, EnvironmentAware {

    private StandardEvaluationContext evaluationContext;
    private SpelExpressionParser expressionParser;
    private TemplateParserContext parserContext;
    private final Map<String, Expression> expressionCache;
    private BeanFactory beanFactory;

    /**
     * 默认构造函数，初始化一个不带根对象的SpringExpressionBean实例。
     */
    public SpringExpressionBean() {
        this(null);
    }

    /**
     * 构造函数，使用指定的根对象来初始化SpringExpressionBean实例。
     *
     * @param rootObject 可以为null。如果提供，该对象将被用作表达式评估的根上下文。
     */
    public SpringExpressionBean(@Nullable Object rootObject) {
        this(rootObject, "#{", "}", true);
    }

    /**
     * 构造函数，允许自定义模板前缀和后缀，并指定是否缓存解析后的表达式。
     *
     * @param tplPrefix  模板字符串的前缀，如"#{"
     * @param tplSuffix  模板字符串的后缀，如"}"
     * @param cacheable  指示是否缓存解析后的表达式。
     */
    public SpringExpressionBean(@Nullable String tplPrefix, @Nullable String tplSuffix, boolean cacheable) {
        this(TypedValue.NULL, tplPrefix, tplSuffix, cacheable);
    }

    /**
     * 构造函数，初始化SpringExpressionBean实例，允许自定义根对象、模板前缀和后缀，以及表达式的缓存策略。
     * 该构造函数配置了SpEL表达式解析器和评估上下文，允许注册自定义函数、类型定位器、属性访问器和bean解析器。
     *
     * @param rootObject 可选的根对象，用于表达式评估。如果为null，则使用{@link TypedValue#NULL}作为默认值。
     * @param tplPrefix  表达式模板的前缀，用于识别表达式字符串的开始。如果为null，不会设置模板上下文。
     * @param tplSuffix  表达式模板的后缀，用于识别表达式字符串的结束。如果为null，不会设置模板上下文。
     * @param cacheable  指定是否启用表达式缓存。当为true时，解析的表达式将被缓存以提高性能。
     */
    public SpringExpressionBean(@Nullable Object rootObject, @Nullable String tplPrefix, @Nullable String tplSuffix,
                                boolean cacheable) {
        // 初始化表达式缓存，根据cacheable参数决定是否创建缓存实例。
        if (cacheable) {
            this.expressionCache = new ConcurrentHashMap<>(4);
        } else {
            this.expressionCache = null;
        }

        // 设置根对象，如果传入的rootObject为null，则使用TypedValue.NULL作为默认值。
        if (rootObject == null) {
            rootObject = TypedValue.NULL;
        }

        // 配置SpEL解析器，包括编译模式和类加载器。
        SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
                ClassUtils.getDefaultClassLoader());
        this.expressionParser = new SpelExpressionParser(configuration);

        // 创建和配置评估上下文，包括根对象和类型定位器。
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext(rootObject);
        StandardTypeLocator typeLocator = new StandardTypeLocator();

        // 注册StringUtils类所在包，允许在表达式中直接使用StringUtils类的方法。
        typeLocator.registerImport(StringUtils.class.getPackage().getName());
        evaluationContext.setVariable("config", Config.env());

        // 注册自定义函数isEmpty。
        try {
            evaluationContext.registerFunction("isEmpty", SpringExpressionBean.class.getDeclaredMethod("isEmpty", Object.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to register 'isEmpty' function.", ex);
        }
        evaluationContext.setTypeLocator(typeLocator);
        // 设置bean解析器，允许在表达式中引用Spring管理的bean。
        evaluationContext.setBeanResolver((context, beanName) -> {
            if (beanFactory != null) {
                return beanFactory.getBean(beanName);
            }
            return ContextUtils.getBean(beanName);
        });
        // evaluationContext.addPropertyAccessor(new
        // ReflectivePropertyAccessor());
        // 清除默认的属性访问器，并设置自定义属性访问器以支持特定的读写策略。
        evaluationContext.getPropertyAccessors().clear();
        List<PropertyAccessor> accessors = new ArrayList<>();
        accessors.add(new PropertyAccessor() {
            @Override
            public void write(EvaluationContext context, Object target, String name, Object newValue)
                    throws AccessException {
                if (target != null) {
                    ((Map<Object, Object>) target).put(name, newValue);
                }
            }

            @Override
            public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
                if (target != null) {
                    Map<String, Object> map = BeanUtils.toMap(target);
                    return new TypedValue(map.get(name));
                }
                return TypedValue.NULL;
            }

            @Override
            public Class<?>[] getSpecificTargetClasses() {
                return new Class<?>[] { Object.class };
            }

            @Override
            public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
                return false;
            }

            @Override
            public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
                return true;
            }
        });
        evaluationContext.setPropertyAccessors(accessors);

        // 设置评估上下文
        this.setEvaluationContext(evaluationContext);

        // 如果提供了模板前缀和后缀，则创建并设置模板解析上下文。
        if (tplPrefix != null && tplSuffix != null) {
            this.parserContext = new TemplateParserContext(tplPrefix, tplSuffix);
        }
    }

    /**
     * 获取当前SpringExpressionBean实例使用的评估上下文。
     * 评估上下文用于在表达式解析和计算过程中提供必要的配置和状态。
     *
     * @return 当前实例的{@link StandardEvaluationContext}对象。
     */
    public StandardEvaluationContext getEvaluationContext() {
        return evaluationContext;
    }

    /**
     * 设置用于表达式评估的{@link StandardEvaluationContext}。
     * 如果提供的评估上下文非空，并且存在一个表达式缓存，则此方法会清除现有的表达式缓存，
     * 因为新的评估上下文可能影响表达式的解析和计算结果。
     *
     * @param evaluationContext 新的评估上下文。如果为null，则不会更新当前的评估上下文。
     */
    public void setEvaluationContext(StandardEvaluationContext evaluationContext) {
        if (evaluationContext != null && expressionCache != null) {
            expressionCache.clear();
        }
        this.evaluationContext = evaluationContext;
    }

    /**
     * 获取用于解析SpEL表达式的解析器。
     * 此解析器配置了表达式解析的相关规则和配置。
     *
     * @return 当前实例使用的{@link SpelExpressionParser}对象。
     */
    public SpelExpressionParser getExpressionParser() {
        return expressionParser;
    }

    /**
     * 设置用于解析SpEL表达式的解析器。
     * 通过提供自定义的解析器，可以自定义表达式的解析行为和配置。
     *
     * @param expressionParser 新的表达式解析器。如果为null，则不更新当前使用的解析器。
     */
    public void setExpressionParser(SpelExpressionParser expressionParser) {
        if (expressionParser != null) {
            this.expressionParser = expressionParser;
        }
    }

    /**
     * 检查给定的表达式字符串是否符合定义的模板格式。
     * 模板格式由前缀和后缀定义，此方法用于判断表达式字符串是否包含这些特定的前后缀。
     *
     * @param expressionString 要检查的表达式字符串。
     * @return 如果表达式字符串符合模板格式，则返回true；否则返回false。
     */
    public boolean isTemplate(String expressionString) {
        if (this.parserContext != null && expressionString != null
                && expressionString.contains(this.parserContext.getExpressionPrefix())
                && expressionString.contains(this.parserContext.getExpressionSuffix())) {
            return true;
        }
        return false;
    }

    /**
     * 初始化给定的表达式字符串集合。此方法遍历集合中的每个表达式字符串，
     * 并尝试解析那些符合模板格式的表达式。这是预解析表达式的一种方式，可以提前发现潜在的错误，
     * 并可能提高后续表达式求值的性能。
     *
     * @param expressions 要初始化的表达式字符串集合。
     */
    public void initialExpressions(Collection<String> expressions) {
        if (!CollectionUtils.isEmpty(expressions)) {
            for (String expressionString : expressions) {
                if (this.isTemplate(expressionString)) {
                    parseExpression(expressionString);
                }
            }
        }
    }

    /**
     * 解析并执行给定的表达式字符串，返回表达式的值。
     * 根据表达式是否被缓存，此方法可能从缓存中获取已解析的表达式，或者解析并缓存新的表达式。
     *
     * @param expressionString 表达式字符串，不可为null或空。
     * @return 解析后的{@link Expression}实例，可用于进一步的值求解。
     */
    public Expression parseExpression(String expressionString) {
        if (StringUtils.isEmpty(expressionString)) {
            return null;
        }
        if (this.expressionCache == null) {
            return this.isTemplate(expressionString)
                    ? this.getExpressionParser().parseExpression(expressionString, parserContext)
                    : this.getExpressionParser().parseExpression(expressionString);
        }
        Expression expression = this.expressionCache.get(expressionString);
        if (expression == null) {
            expression = this.isTemplate(expressionString)
                    ? this.getExpressionParser().parseExpression(expressionString, parserContext)
                    : this.getExpressionParser().parseExpression(expressionString);
            this.expressionCache.put(expressionString, expression);
        }
        return expression;
    }

    /**
     * 根据指定的表达式字符串和期望的返回类型，评估表达式并返回结果。
     * 此方法首先解析表达式字符串，然后根据提供的评估上下文求值。
     *
     * @param expressionString 表达式字符串，不可为null。
     * @param valueClass 期望的返回值类型的Class对象。
     * @param <T> 返回值的类型。
     * @return 表达式的计算结果，其类型为T。如果表达式无法解析或计算，可能返回null。
     */
    public <T> T getValue(String expressionString, Class<T> valueClass) {
        Expression expression = this.parseExpression(expressionString);
        if (expression == null) {
            return null;
        }
        return expression.getValue(evaluationContext, valueClass);
    }

    /**
     * 根据给定的表达式字符串、根对象和预期返回类型，评估SpEL表达式并返回结果。
     * 此方法首先尝试解析给定的表达式字符串，如果解析成功，则使用提供的根对象和评估上下文来计算表达式的值。
     *
     * @param expressionString 待解析和评估的SpEL表达式字符串，不应为空。
     * @param rootObject 表达式评估时使用的根对象。如果表达式的解析和评估依赖于特定的上下文或变量，该对象将被用作根上下文。
     * @param valueClass 表达式预期返回值的类型。表达式计算的结果将尝试转换为此类型。
     * @param <T> 表达式返回值的泛型类型。
     * @return 如果表达式成功解析和评估，返回表达式的计算结果；如果表达式解析失败，返回null。
     */
    public <T> T getValue(String expressionString, Object rootObject, Class<T> valueClass) {
        Expression expression = this.parseExpression(expressionString);
        if (expression == null) {
            return null;
        }
        return expression.getValue(evaluationContext, rootObject, valueClass);
    }

    /**
     * 尝试解析并评估给定的SpEL表达式字符串，返回表达式的计算结果。如果表达式解析或评估过程中发生异常，
     * 将记录异常信息并返回指定的默认值。
     *
     * @param expressionString 待解析和评估的SpEL表达式字符串，不应为空。
     * @param rootObject 表达式评估时使用的根对象。如果表达式的解析和评估依赖于特定的上下文或变量，该对象将被用作根上下文。
     * @param defValue 如果在解析或评估表达式过程中发生异常，将返回此默认值。
     * @param valueClass 表达式预期返回值的类型。表达式计算的结果将尝试转换为此类型。
     * @param <T> 表达式返回值的泛型类型。
     * @return 如果表达式成功解析和评估，返回表达式的计算结果；如果发生异常，则返回defValue指定的默认值。
     */
    public <T> T getValue(String expressionString, Object rootObject, T defValue, Class<T> valueClass) {
        try {
            Expression expression = this.parseExpression(expressionString);
            if (expression == null) {
                return null;
            }
            return expression.getValue(evaluationContext, rootObject, valueClass);
        } catch (Exception ex) {
            log.error("error parse expression :" + expressionString, ex);
        }
        return defValue;
    }

    /**
     * 从缓存中获取并评估指定的SpEL表达式，返回计算结果。如果表达式已缓存，则直接使用缓存的结果进行评估；
     * 否则，返回指定的默认值。此方法提供了一种高效访问频繁使用的表达式的方式，通过减少解析开销来提升性能。
     * 如果在表达式评估过程中发生异常，将记录错误信息并返回默认值。
     *
     * @param expressionString 表达式字符串，不可为null。这是要评估的SpEL表达式。
     * @param rootObject 表达式评估时的根对象。可以为null，此时使用默认的评估上下文。
     * @param defValue 如果表达式不存在于缓存中，或者评估过程中发生异常，则返回的默认值。
     * @param valueClass 期望的返回值类型的Class对象，表达式的结果将被转换为此类型。
     * @param <T> 方法的返回类型，由valueClass参数指定。
     * @return 表达式的评估结果。如果表达式在缓存中存在且成功评估，则返回该结果；如果表达式不存在或评估失败，则返回defValue指定的默认值。
     * @throws IllegalStateException 如果在表达式评估过程中发生非受检异常，将捕获异常并记录，但不向调用者抛出。
     */
    public <T> T getValueFromCacheExpression(String expressionString, Object rootObject, T defValue,
                                             Class<T> valueClass) {
        if (expressionCache == null) {
            return null;
        }
        Expression expression = this.expressionCache.get(expressionString);
        if (expression != null) {
            try {
                return expression.getValue(evaluationContext, rootObject, valueClass);
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }
        return defValue;
    }

    /**
     * 设置BeanFactory，允许在表达式中访问Spring管理的bean。
     * 此方法实现自BeanFactoryAware接口，当Spring容器启动时，会自动调用此方法并注入BeanFactory实例。
     *
     * @param beanFactory Spring的BeanFactory，不可为null。用于在表达式评估时解析bean引用。
     * @throws BeansException 如果发生错误。
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory != null) {
            evaluationContext.setVariable("beanFactory", beanFactory);
        }
        this.beanFactory = beanFactory;
    }

    /**
     * 设置Spring {@link Environment}，使之能在表达式中使用环境属性。
     * 此方法实现自EnvironmentAware接口，当Spring容器启动时，会自动调用此方法并注入当前的Environment。
     *
     * @param environment Spring的环境上下文，不可为null。用于在表达式评估时提供环境变量。
     */
    @Override
    public void setEnvironment(Environment environment) {
        if (environment != null) {
            evaluationContext.setVariable("env", environment);
        }
    }

    /**
     * 用于检查给定的对象是否为空。支持字符串、集合和数组类型。
     * 此静态方法可在SpEL表达式中直接引用，用于增强表达式的功能。
     *
     * @param value 待检查的对象，可以是字符串、集合或数组。
     * @return 如果对象为空，则返回true；否则返回false。
     */
    public static boolean isEmpty(Object value) {
        if (StringUtils.isEmpty(value)) {
            return true;
        } else if (value instanceof Collection<?>) {
            return ((Collection<?>) value).isEmpty();
        } else if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }

    public static void main(String[] args) {
        Map<String, Object> root = new HashMap<String, Object>(3);
        root.put("now", new Date());
        root.put("value", "123testing");
        root.put("env", "prod");
        SpringExpressionBean bean = new SpringExpressionBean(root, "!{", "}", true);
        Object d = bean.getValue("!{'!'}{now}", Object.class);
        System.out.println(d);
        Object val = bean.getValue("!{env.equals('prod')}", Object.class);
        System.out.println(val);
    }

}
