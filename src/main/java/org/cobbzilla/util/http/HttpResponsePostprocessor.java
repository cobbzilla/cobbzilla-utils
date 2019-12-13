package org.cobbzilla.util.http;

public interface HttpResponsePostprocessor {

    HttpResponseBean postProcess (HttpResponseBean response);

}
