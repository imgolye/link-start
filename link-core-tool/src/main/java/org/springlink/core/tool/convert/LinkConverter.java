package org.springlink.core.tool.convert;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cglib.core.Converter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springlink.core.tool.support.Try;
import org.springlink.core.tool.utils.ClassUtil;
import org.springlink.core.tool.utils.ConvertUtil;
import org.springlink.core.tool.utils.ReflectUtil;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 组合 spring cglib Converter 和 spring ConversionService
 *
 *
 */
@Slf4j
@AllArgsConstructor
public class LinkConverter implements Converter {
	private static final ConcurrentMap<String, TypeDescriptor> TYPE_CACHE = new ConcurrentHashMap<>();
	private final Class<?> targetClazz;

	/**
	 * cglib convert
	 * @param value 源对象属性
	 * @param target 目标对象属性类
	 * @param fieldName 目标的field名，原为 set 方法名，LinkBeanCopier 里做了更改
	 * @return {Object}
	 */
	@Override
	@Nullable
	public Object convert(Object value, Class target, final Object fieldName) {
		if (value == null) {
			return null;
		}
		// 类型一样，不需要转换
		if (ClassUtil.isAssignableValue(target, value)) {
			return value;
		}
		try {
			TypeDescriptor targetDescriptor = LinkConverter.getTypeDescriptor(targetClazz, (String) fieldName);
			return ConvertUtil.convert(value, targetDescriptor);
		} catch (Throwable e) {
			log.warn("LinkConverter error", e);
			return null;
		}
	}

	private static TypeDescriptor getTypeDescriptor(final Class<?> clazz, final String fieldName) {
		String srcCacheKey = clazz.getName() + fieldName;
		return TYPE_CACHE.computeIfAbsent(srcCacheKey, Try.of(k -> {
			// 这里 property 理论上不会为 null
			Field field = ReflectUtil.getField(clazz, fieldName);
			if (field == null) {
				throw new NoSuchFieldException(fieldName);
			}
			return new TypeDescriptor(field);
		}));
	}
}
