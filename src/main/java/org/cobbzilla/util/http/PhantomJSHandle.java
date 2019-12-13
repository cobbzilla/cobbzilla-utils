package org.cobbzilla.util.http;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.ErrorHandler;
import org.openqa.selenium.remote.Response;

import java.io.Closeable;

@AllArgsConstructor @Slf4j
public class PhantomJSHandle extends ErrorHandler implements Closeable {

    @Getter final PhantomJSDriver driver;

    @Override public void close() {
        if (driver != null) {
            driver.close();
            driver.quit();
        }
    }

}