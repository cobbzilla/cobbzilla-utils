package org.cobbzilla.util.dns;

import java.util.List;

public interface DnsManager {

    /**
     * List matching DNS records
     * @param match The DnsRecordMatch query
     * @return a List of DnsRecords that match
     */
    List<DnsRecord> list(DnsRecordMatch match) throws Exception;

    /**
     * Write a DNS record
     * @param record a DNS record to create or update
     * @return true if the record was written, false if it was not (it may have been unchanged)
     */
    boolean write(DnsRecord record) throws Exception;

    /**
     * Publish changes to DNS records. Must be called after calling write if you want to see the changes publicly.
     */
    void publish() throws Exception;

    /**
     * Delete matching DNS records
     * @param match The DnsRecordMatch query
     * @return A count of the number of records deleted, or -1 if this DnsManager does not support returning counts
     */
    int remove(DnsRecordMatch match) throws Exception;

}
