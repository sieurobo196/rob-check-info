package com.cwr.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@UtilityClass
public class StringJsonUtil {
    private final static Logger logger = LoggerFactory.getLogger(StringJsonUtil.class);

    /**
     * Convert CfRequest object to String with Json format
     *
     * @return
     */
    public String toStringJson(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.info("There is an issue when convert Object to json: {} - {} ", obj, e);
        }
        return null;
    }

    public <T> T toObjectJson(String stringJson, Class<T> tClass) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            T cfResponse = mapper.readValue(stringJson, tClass);

            return cfResponse;

        } catch (UnrecognizedPropertyException e) {
            //should handle this case in future(store into DB ...)
            //1.should call delete job if throw error exception
            //2.show info message and don't throw error message
            logger.info("The parsing json to object is unrecognized property", e);
            return null;
        } catch (IOException e) {
            logger.info("There is an issue on response when parsing json String to Object ", e);
            return null;
        }
    }
}
