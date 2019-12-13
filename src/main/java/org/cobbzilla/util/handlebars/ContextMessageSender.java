package org.cobbzilla.util.handlebars;

public interface ContextMessageSender {

    void send(String recipient, String subject, String message, String contentType);

}
