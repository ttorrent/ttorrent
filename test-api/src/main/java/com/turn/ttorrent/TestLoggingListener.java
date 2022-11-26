package com.turn.ttorrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

public class TestLoggingListener implements IInvokedMethodListener {
    private static final Logger log = LoggerFactory.getLogger(TestLoggingListener.class);

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        log.info("Entering {}.{}", method.getTestMethod().getRealClass().getSimpleName(), method.getTestMethod().getMethodName());
    }
}
