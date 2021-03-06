/*
 * Copyright (c) 2010-2015 Jakub Białek
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.google.code.ssm.aop.counter;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.ssm.aop.support.AnnotationData;
import com.google.code.ssm.aop.support.AnnotationDataBuilder;
import com.google.code.ssm.api.counter.DecrementCounterInCache;

/**
 * 
 * @author Jakub Białek
 * @since 2.0.0
 * 
 */
@Aspect
public class DecrementCounterInCacheAdvice extends CounterInCacheBase {

    private static final Logger LOG = LoggerFactory.getLogger(DecrementCounterInCacheAdvice.class);

    @Pointcut("@annotation(com.google.code.ssm.api.counter.DecrementCounterInCache)")
    public void decrementSingleCounter() {
        /* pointcut definition */
    }

    @AfterReturning("decrementSingleCounter()")
    public void decrementSingle(final JoinPoint jp) throws Throwable {
        if (isDisabled()) {
            getLogger().info("Cache disabled");
            return;
        }
        // This is injected caching. If anything goes wrong in the caching, LOG
        // the crap outta it, but do not let it surface up past the AOP injection itself.
        // It will be invoked only if underlying method completes successfully.
        String cacheKey = null;
        DecrementCounterInCache annotation;
        try {
            Method methodToCache = getCacheBase().getMethodToCache(jp);
            annotation = methodToCache.getAnnotation(DecrementCounterInCache.class);
            AnnotationData data = AnnotationDataBuilder.buildAnnotationData(annotation, DecrementCounterInCache.class, methodToCache);
            cacheKey = getCacheBase().getCacheKeyBuilder().getCacheKey(data, jp.getArgs(), methodToCache.toString());
            getCacheBase().getCache(data).decr(cacheKey, 1);
        } catch (Exception ex) {
            warn(ex, "Decrementing counter [%s] via %s aborted due to an error.", cacheKey, jp.toShortString());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
