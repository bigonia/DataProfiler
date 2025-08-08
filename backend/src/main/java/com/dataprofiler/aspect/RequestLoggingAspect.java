package com.dataprofiler.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingAspect.class);

    /**
     * Define a pointcut for all public methods in classes annotated with @RestController
     * within the com.dataprofiler.controller package.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController com.dataprofiler.controller.*)")
    public void controllerMethods() {}

    /**
     * Advice that runs before any method matched by the controllerMethods pointcut.
     * It logs the request URL, HTTP method, query parameters, and request body.
     *
     * @param joinPoint The JoinPoint object provides access to the method being advised.
     */
    @Before("controllerMethods()")
    public void logRequest(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        
        // Wrap the request to allow multiple reads of the input stream
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);

        logger.info("\n============================== Request Start ==============================");
        logger.info("URL        : {}", requestWrapper.getRequestURL());
        logger.info("HTTP Method: {}", requestWrapper.getMethod());
        logger.info("Class.Method: {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());

        // Log Query Parameters
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = requestWrapper.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            params.put(paramName, requestWrapper.getParameter(paramName));
        }
        if (!params.isEmpty()) {
            logger.info("Query Params : {}", params);
        }

        // Log Request Body or MultipartFile info
        if ("POST".equalsIgnoreCase(requestWrapper.getMethod()) ||
            "PUT".equalsIgnoreCase(requestWrapper.getMethod()) ||
            "PATCH".equalsIgnoreCase(requestWrapper.getMethod())) {
            if (requestWrapper.getContentType() != null && requestWrapper.getContentType().startsWith("multipart/form-data")) {
                // For multipart requests, log file details instead of body
                Object[] args = joinPoint.getArgs();
                for (Object arg : args) {
                    if (arg instanceof MultipartFile) {
                        MultipartFile file = (MultipartFile) arg;
                        logger.info("Uploaded File: Name={}, Size={} bytes, ContentType={}",
                                file.getOriginalFilename(), file.getSize(), file.getContentType());
                    }
                }
            } else {
                try {
                    String requestBody = new String(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding());
                    if (!requestBody.isEmpty()) {
                        logger.info("Request Body : {}", requestBody);
                    }
                } catch (Exception e) {
                    logger.error("Could not log request body", e);
                }
            }
        }

        logger.info("============================== Request End ================================\n");
    }
}