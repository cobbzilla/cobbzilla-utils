package org.cobbzilla.util.collection.multi;

import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

public abstract class MultiResultDriverBase implements MultiResultDriver {

    @Getter protected MultiResult result = new MultiResult();

    protected abstract String successMessage(Object task);
    protected abstract String failureMessage(Object task);
    protected abstract void run(Object task) throws Exception;

    @Override public void before() {}
    @Override public void after() {}

    @Override public void exec(Object task) {
        try {
            before();
            run(task);
            success(successMessage(task));
        } catch (Exception e) {
            failure(failureMessage(task), e);
        } finally {
            after();
        }
    }

    @Override public void success(String message) { result.success(message); }
    @Override public void failure(String message, Exception e) { result.fail(message, e.toString() + "\n- stack -\n" + ExceptionUtils.getStackTrace(e) + "\n- end stack -\n"); }

}
