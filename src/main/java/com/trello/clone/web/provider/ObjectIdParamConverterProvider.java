package com.trello.clone.web.provider;

import com.trello.clone.service.exception.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import org.bson.types.ObjectId;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class ObjectIdParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (!ObjectId.class.equals(rawType)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        ParamConverter<T> converter = (ParamConverter<T>) new ObjectIdParamConverter();
        return converter;
    }

    private static final class ObjectIdParamConverter implements ParamConverter<ObjectId> {

        @Override
        public ObjectId fromString(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }

            if (!ObjectId.isValid(value)) {
                throw new BadRequestException("'" + value + "' is not a valid ID");
            }

            return new ObjectId(value);
        }

        @Override
        public String toString(ObjectId value) {
            return value == null ? null : value.toHexString();
        }
    }
}
